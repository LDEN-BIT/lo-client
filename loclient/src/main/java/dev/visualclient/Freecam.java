package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam: the camera detaches from your character and flies around on its own, while your
 * character stands still. The movement keys fly the camera, the mouse looks around.
 * Purely a client-side camera change, nothing is sent to the server.
 */
public final class Freecam {
    public static boolean active = false;

    private static Entity cam;       // an invisible marker entity the camera looks through
    private static Level camLevel;
    private static double x, y, z;

    private Freecam() {}

    public static void toggle(Minecraft mc) {
        if (active) stop();
        else start(mc);
    }

    private static void start(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        Entity e = EntityType.MARKER.create(mc.level, EntitySpawnReason.LOAD);
        if (e == null) return;
        Vec3 eye = mc.player.getEyePosition();
        x = eye.x;
        y = eye.y;
        z = eye.z;
        e.setPos(x, y, z);
        e.xo = x;
        e.yo = y;
        e.zo = z;
        cam = e;
        camLevel = mc.level;
        Freelook.yaw = mc.player.getYRot();
        Freelook.pitch = mc.player.getXRot();
        Freelook.active = true; // the mouse now turns the camera, not your character
        active = true;
    }

    public static void stop() {
        active = false;
        cam = null;
        camLevel = null;
        Freelook.active = false;
    }

    /** Runs at the start of every tick, before the game reads your movement keys. */
    public static void tickStart(Minecraft mc) {
        if (!active) return;
        if (mc.player == null || mc.level == null || camLevel != mc.level) {
            stop();
            return;
        }
        if (mc.screen != null) return;

        Options o = mc.options;
        double fwd = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double side = (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
        double up = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
        double speed = Config.get().freecamSpeed * (o.keySprint.isDown() ? 2.0 : 1.0);

        double yaw = Math.toRadians(Freelook.yaw);
        double pitch = Math.toRadians(Freelook.pitch);
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);

        cam.xo = cam.getX();
        cam.yo = cam.getY();
        cam.zo = cam.getZ();
        x += (fx * fwd + rx * side) * speed;
        y += (fy * fwd + up) * speed;
        z += (fz * fwd + rz * side) * speed;
        cam.setPos(x, y, z);

        // Keep your character still: the game never sees these keys this tick.
        o.keyUp.setDown(false);
        o.keyDown.setDown(false);
        o.keyLeft.setDown(false);
        o.keyRight.setDown(false);
        o.keyJump.setDown(false);
        o.keyShift.setDown(false);
        o.keySprint.setDown(false);
    }

    /** Called by the camera: while freecam is on it looks through the marker instead. */
    public static Entity focus(Entity original) {
        if (!active || cam == null) return original;
        cam.setYRot(Freelook.yaw);
        cam.setXRot(Freelook.pitch);
        cam.yRotO = Freelook.yaw;
        cam.xRotO = Freelook.pitch;
        return cam;
    }
}
