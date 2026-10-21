package ai.serge.dermes;

/** Tiny ARGB helpers (java.awt is not on Android). */
final class Colors {
    static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
    /** Component lerp of two opaque ARGB colors; t=0 -> a, t=1 -> b. */
    static int mix(int a, int b, float t) {
        int tt = (int) (255 * Math.max(0f, Math.min(1f, t)));
        int it = 255 - tt;
        int rr = (((a >> 16) & 0xFF) * it + ((b >> 16) & 0xFF) * tt) / 255;
        int gg = (((a >> 8) & 0xFF) * it + ((b >> 8) & 0xFF) * tt) / 255;
        int bb = ((a & 0xFF) * it + (b & 0xFF) * tt) / 255;
        return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
    }
    static int lighten(int color, float t) { return mix(color, 0xFFFFFFFF, t); }
}
