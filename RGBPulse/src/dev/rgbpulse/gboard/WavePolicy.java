package dev.rgbpulse.gboard;

/** Pure row-wave math, shared by the hook and interactive preview. */
final class WavePolicy {
    static float glow(float cx, int top, float origin, int rowTop, float radius, float dp) {
        if (Math.abs((float) top - rowTop) > 36 * dp || radius <= 1) return 0;
        return Math.max(0, 1 - Math.abs(cx - origin) / radius);
    }
}
