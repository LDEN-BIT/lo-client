package dev.visualclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Server list ping (the standard Minecraft status protocol), run off the main thread. */
final class ServerPinger {
    record Seg(String text, int rgb) {}

    static final class Info {
        volatile int state;          // 0 pinging, 1 online, 2 offline
        volatile int online = -1;
        volatile int max = -1;
        volatile long latency = -1;
        volatile List<List<Seg>> motd = List.of();
        volatile byte[] favicon;     // PNG bytes from the server, if it has an icon
    }

    private static final int[] LEGACY = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final String[] NAMES = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "LoClient-Ping");
        t.setDaemon(true);
        return t;
    });

    private ServerPinger() {}

    static void ping(String ip, Info info) {
        info.state = 0;
        POOL.submit(() -> {
            try {
                run(ip, info);
            } catch (Throwable t) {
                info.state = 2;
            }
        });
    }

    private static void run(String ip, Info info) throws IOException {
        ServerAddress address = ServerAddress.parseString(ip);
        Optional<ResolvedServerAddress> resolved = ServerNameResolver.DEFAULT.resolveAddress(address);
        if (resolved.isEmpty()) {
            info.state = 2;
            return;
        }
        InetSocketAddress target = resolved.get().asInetSocketAddress();
        try (Socket socket = new Socket()) {
            socket.connect(target, 4000);
            socket.setSoTimeout(4000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            ByteArrayOutputStream hs = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(hs);
            writeVarInt(h, 0);
            writeVarInt(h, 774);
            byte[] host = address.getHost().getBytes(StandardCharsets.UTF_8);
            writeVarInt(h, host.length);
            h.write(host);
            h.writeShort(address.getPort());
            writeVarInt(h, 1);
            writeVarInt(out, hs.size());
            out.write(hs.toByteArray());
            writeVarInt(out, 1);
            writeVarInt(out, 0);
            out.flush();

            readVarInt(in);              // packet length
            readVarInt(in);              // packet id
            int len = readVarInt(in);
            byte[] data = new byte[len];
            in.readFully(data);
            parse(new String(data, StandardCharsets.UTF_8), info);

            long start = System.nanoTime();
            writeVarInt(out, 9);
            writeVarInt(out, 1);
            out.writeLong(start);
            out.flush();
            readVarInt(in);
            readVarInt(in);
            in.readLong();
            info.latency = (System.nanoTime() - start) / 1_000_000L;
            info.state = 1;
        }
    }

    private static void parse(String json, Info info) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        if (o.has("players") && o.get("players").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("players");
            if (p.has("online")) info.online = p.get("online").getAsInt();
            if (p.has("max")) info.max = p.get("max").getAsInt();
        }
        if (o.has("favicon") && o.get("favicon").isJsonPrimitive()) {
            try {
                String f = o.get("favicon").getAsString();
                int comma = f.indexOf(',');
                if (comma >= 0) info.favicon = Base64.getMimeDecoder().decode(f.substring(comma + 1));
            } catch (IllegalArgumentException ignored) {
            }
        }
        List<Seg> segs = new ArrayList<>();
        if (o.has("description")) flatten(o.get("description"), 0xFFFFFF, segs);
        info.motd = splitLines(segs);
    }

    private static void flatten(JsonElement e, int inherited, List<Seg> out) {
        if (e.isJsonPrimitive()) {
            addText(e.getAsString(), inherited, out);
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) flatten(x, inherited, out);
        } else if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            int color = inherited;
            if (o.has("color") && o.get("color").isJsonPrimitive()) color = parseColor(o.get("color").getAsString(), inherited);
            if (o.has("text") && o.get("text").isJsonPrimitive()) addText(o.get("text").getAsString(), color, out);
            if (o.has("extra")) flatten(o.get("extra"), color, out);
        }
    }

    private static int parseColor(String s, int fallback) {
        if (s.startsWith("#") && s.length() == 7) {
            try {
                return Integer.parseInt(s.substring(1), 16);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(s)) return LEGACY[i];
        }
        return fallback;
    }

    /** Handles the old section-sign color codes inside plain text. */
    private static void addText(String text, int base, List<Seg> out) {
        int color = base;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\u00a7' && i + 1 < text.length()) {
                if (sb.length() > 0) {
                    out.add(new Seg(sb.toString(), color));
                    sb.setLength(0);
                }
                char code = Character.toLowerCase(text.charAt(++i));
                int idx = "0123456789abcdef".indexOf(code);
                if (idx >= 0) color = LEGACY[idx];
                else if (code == 'r') color = base;
                continue;
            }
            sb.append(ch);
        }
        if (sb.length() > 0) out.add(new Seg(sb.toString(), color));
    }

    private static List<List<Seg>> splitLines(List<Seg> segs) {
        List<List<Seg>> lines = new ArrayList<>();
        List<Seg> cur = new ArrayList<>();
        for (Seg s : segs) {
            String[] parts = s.text().split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    lines.add(cur);
                    cur = new ArrayList<>();
                }
                if (!parts[i].isEmpty()) cur.add(new Seg(parts[i], s.rgb()));
            }
        }
        lines.add(cur);
        return lines;
    }

    private static void writeVarInt(OutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    private static int readVarInt(InputStream in) throws IOException {
        int result = 0, shift = 0, b;
        do {
            b = in.read();
            if (b < 0) throw new IOException("end of stream");
            result |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) throw new IOException("VarInt too big");
        } while ((b & 0x80) != 0);
        return result;
    }
}
