package dev.visualclient;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Draws the round, smooth menu textures (buttons, sliders, text fields, scrollbars) in the menu
 * color you pick, at 4x resolution so they are not pixelated. Needs java.awt; the caller falls
 * back to the bundled purple textures if that is not available.
 */
final class ThemeTextures {
    private static final int S = 4; // texture pixels per GUI pixel
    private static final int[] DARK = {16, 16, 28};
    private static final int[] DARK_BORDER = {22, 22, 36};
    private static final int[] BASE = {150, 110, 255}; // color the bundled separators/backgrounds use

    private ThemeTextures() {}

    static void write(Path root, int rgb) throws IOException {
        Path widget = root.resolve("assets").resolve("minecraft").resolve("textures")
                .resolve("gui").resolve("sprites").resolve("widget");
        Files.createDirectories(widget);
        int[] c = {(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255};
        int[] white = {255, 255, 255};

        sprite(widget, "button", 200, 20, 8, argb(mix(DARK, c, 0.07f), 235), argb(mix(DARK_BORDER, c, 0.5f), 255), 1f, "8");
        sprite(widget, "button_highlighted", 200, 20, 8, argb(mix(DARK, c, 0.30f), 245), argb(c, 255), 1f, "8");
        sprite(widget, "button_disabled", 200, 20, 8, argb(new int[]{14, 14, 22}, 200), argb(new int[]{40, 40, 56}, 255), 1f, "8");
        sprite(widget, "slider", 200, 20, 8, argb(new int[]{12, 12, 24}, 235), argb(mix(DARK_BORDER, c, 0.5f), 255), 1f, "8");
        sprite(widget, "slider_highlighted", 200, 20, 8, argb(mix(new int[]{12, 12, 24}, c, 0.12f), 245), argb(c, 255), 1f, "8");
        sprite(widget, "text_field", 200, 20, 7, argb(new int[]{10, 10, 20}, 235), argb(mix(DARK_BORDER, c, 0.5f), 255), 1f, "7");
        sprite(widget, "text_field_highlighted", 200, 20, 7, argb(new int[]{12, 12, 26}, 245), argb(c, 255), 1f, "7");
        String hb = "{\"left\":3,\"top\":8,\"right\":3,\"bottom\":8}";
        sprite(widget, "slider_handle", 8, 20, 4, argb(c, 255), argb(mix(c, white, 0.5f), 255), 0.75f, hb);
        sprite(widget, "slider_handle_highlighted", 8, 20, 4, argb(mix(c, white, 0.35f), 255), argb(white, 255), 0.75f, hb);
        String sb = "{\"left\":3,\"top\":3,\"right\":3,\"bottom\":3}";
        sprite(widget, "scroller", 6, 32, 3, argb(c, 255), argb(mix(c, white, 0.4f), 255), 0.75f, sb);
        sprite(widget, "scroller_background", 6, 32, 3, argb(new int[]{24, 24, 40}, 220), 0, 0f, sb);

        // Separators and list backgrounds: the bundled purple images, tinted to your color.
        Path gui = root.resolve("assets").resolve("minecraft").resolve("textures").resolve("gui");
        for (String f : new String[]{"footer_separator.png", "header_separator.png",
                "inworld_footer_separator.png", "inworld_header_separator.png",
                "inworld_menu_list_background.png", "menu_list_background.png"}) {
            recolor(gui, f, c);
        }
    }

    private static void sprite(Path dir, String name, int lw, int lh, float radius,
                               int fill, int border, float bw, String mcmetaBorder) throws IOException {
        int w = lw * S, h = lh * S;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setComposite(AlphaComposite.Src);
            float arc = radius * S * 2f;
            if (bw > 0f && border != 0) {
                g.setColor(new Color(border, true));
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
                float b = bw * S;
                float innerArc = Math.max(0f, arc - 2f * b);
                g.setColor(new Color(fill, true));
                g.fill(new RoundRectangle2D.Float(b, b, w - 2f * b, h - 2f * b, innerArc, innerArc));
            } else {
                g.setColor(new Color(fill, true));
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        Files.write(dir.resolve(name + ".png"), out.toByteArray());
        Files.writeString(dir.resolve(name + ".png.mcmeta"),
                "{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":" + lw + ",\"height\":" + lh
                        + ",\"border\":" + mcmetaBorder + "}}}");
    }

    private static void recolor(Path guiDir, String file, int[] c) throws IOException {
        try (InputStream in = ThemeTextures.class.getResourceAsStream("/assets/minecraft/textures/gui/" + file)) {
            if (in == null) return;
            BufferedImage src = ImageIO.read(in);
            if (src == null) return;
            BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int p = src.getRGB(x, y);
                    int a = p >>> 24;
                    int r = Math.min(255, Math.round(((p >> 16) & 255) * (c[0] / (float) BASE[0])));
                    int g = Math.min(255, Math.round(((p >> 8) & 255) * (c[1] / (float) BASE[1])));
                    int b = Math.min(255, Math.round((p & 255) * (c[2] / (float) BASE[2])));
                    out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            Files.createDirectories(guiDir);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bytes);
            Files.write(guiDir.resolve(file), bytes.toByteArray());
        }
    }

    private static int[] mix(int[] a, int[] b, float t) {
        return new int[]{
                Math.round(a[0] + (b[0] - a[0]) * t),
                Math.round(a[1] + (b[1] - a[1]) * t),
                Math.round(a[2] + (b[2] - a[2]) * t)};
    }

    private static int argb(int[] rgb, int alpha) {
        return (alpha << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }
}
