package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Buddy: a small client-side mob that follows you around. It only exists on your screen:
 * nothing is sent to the server, nobody else sees it, and you can pick any living mob and name it.
 */
public final class Buddy {
    private static final int BUDDY_ID = -2_000_001;

    private static Entity entity;
    private static ClientLevel levelOf;
    private static String spawnedType = "";

    private static List<EntityType<?>> types;

    private Buddy() {}

    public static boolean isBuddy(Entity e) {
        return e != null && e == entity;
    }

    /** Every summonable living mob, sorted by name. */
    public static List<EntityType<?>> types() {
        if (types == null) {
            List<EntityType<?>> list = new ArrayList<>();
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (t.canSummon() && isLiving(t)) list.add(t);
            }
            list.sort(Comparator.comparing(t -> t.getDescription().getString()));
            types = list;
        }
        return types;
    }

    /** Everything that is not "misc" is a mob; a few living ones live in misc. */
    private static boolean isLiving(EntityType<?> t) {
        if (t.getCategory() != MobCategory.MISC) return true;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath();
        return id.equals("villager") || id.equals("wandering_trader")
                || id.equals("iron_golem") || id.equals("snow_golem") || id.equals("armor_stand");
    }

    public static String idOf(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    public static String displayName(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key != null && BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
            return BuiltInRegistries.ENTITY_TYPE.getValue(key).getDescription().getString();
        }
        return id;
    }

    public static void tick(Minecraft mc) {
        Config c = Config.get();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        if (entity != null && levelOf != level) {
            entity = null; // we changed worlds or dimensions; the old one is already gone
        }
        if (level == null || player == null || !c.buddyEnabled) {
            despawn(level);
            return;
        }
        if (entity != null && (entity.isRemoved() || !spawnedType.equals(c.buddyType))) {
            despawn(level);
        }
        if (entity == null) {
            spawn(level, player, c);
            if (entity == null) return;
        }

        // Where the buddy wants to be: just behind you, a bit to your right.
        double yaw = Math.toRadians(player.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double rx = Math.cos(yaw), rz = Math.sin(yaw);
        double tx = player.getX() - fx * 1.3 + rx * 0.9;
        double ty = player.getY();
        double tz = player.getZ() - fz * 1.3 + rz * 0.9;

        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        double dist = Math.sqrt((tx - ex) * (tx - ex) + (ty - ey) * (ty - ey) + (tz - ez) * (tz - ez));
        double nx, ny, nz;
        if (dist > 10.0) {
            nx = tx; ny = ty; nz = tz; // too far away (teleport etc.): snap next to you
        } else {
            nx = ex + (tx - ex) * 0.2;
            ny = ey + (ty - ey) * 0.25;
            nz = ez + (tz - ez) * 0.2;
        }

        double dx = nx - ex, dz = nz - ez;
        float targetYaw = dx * dx + dz * dz > 1.0e-4
                ? (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90f
                : player.getYRot();
        float yawNow = Mth.rotLerp(0.3f, entity.getYRot(), targetYaw);

        entity.setPos(nx, ny, nz);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setYRot(yawNow);
        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yawNow);
            living.setYBodyRot(yawNow);
            AttributeInstance scale = living.getAttribute(Attributes.SCALE);
            if (scale != null && Math.abs(scale.getBaseValue() - c.buddyScale) > 1.0e-3) {
                scale.setBaseValue(c.buddyScale);
            }
        }
        entity.fallDistance = 0f;

        Component want = c.buddyName == null || c.buddyName.isBlank() ? null : Component.literal(c.buddyName);
        if (want == null) {
            if (entity.getCustomName() != null) {
                entity.setCustomName(null);
                entity.setCustomNameVisible(false);
            }
        } else if (entity.getCustomName() == null || !entity.getCustomName().getString().equals(c.buddyName)) {
            entity.setCustomName(want);
            entity.setCustomNameVisible(true);
        }
    }

    private static void spawn(ClientLevel level, LocalPlayer player, Config c) {
        try {
            Identifier key = Identifier.tryParse(c.buddyType);
            if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) return;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(key);
            Entity e = type.create(level, EntitySpawnReason.LOAD);
            if (!(e instanceof LivingEntity living)) return;

            AttributeInstance scale = living.getAttribute(Attributes.SCALE);
            if (scale != null) scale.setBaseValue(c.buddyScale);
            e.setNoGravity(true);
            e.noPhysics = true;
            e.setSilent(true);
            e.setInvulnerable(true);
            e.setId(BUDDY_ID);
            e.setPos(player.getX(), player.getY(), player.getZ());
            level.addEntity(e);

            entity = e;
            levelOf = level;
            spawnedType = c.buddyType;
        } catch (Throwable t) {
            System.err.println("[Lo Client] Could not spawn the buddy: " + t);
            c.buddyEnabled = false;
            entity = null;
        }
    }

    private static void despawn(ClientLevel level) {
        if (entity != null && level != null && levelOf == level) {
            try {
                level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
            } catch (Throwable ignored) {
            }
        }
        entity = null;
    }
}
