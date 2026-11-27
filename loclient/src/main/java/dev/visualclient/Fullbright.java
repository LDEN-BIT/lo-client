package dev.visualclient;

/**
 * Fullbright: while on, the game reads the Brightness (gamma) setting as a very high value, so
 * every light level looks bright. Nothing about your saved Brightness setting changes.
 */
public final class Fullbright {
    public static final double GAMMA = 15.0;

    private Fullbright() {}

    public static boolean on() {
        return Config.get().fullbright;
    }
}
