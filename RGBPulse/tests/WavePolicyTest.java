package dev.rgbpulse.gboard;

public final class WavePolicyTest {
    private static void equal(float expected, float actual) {
        if (Math.abs(expected - actual) > .0001f) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        // Row zero is a real row, not a sentinel meaning every row.
        equal(1, WavePolicy.glow(0, 0, 0, 0, 100, 1));
        equal(0, WavePolicy.glow(0, 50, 0, 0, 100, 1));
        equal(.5f, WavePolicy.glow(50, 0, 0, 0, 100, 1));
        equal(.5f, WavePolicy.glow(-50, 0, 0, 0, 100, 1));
        equal(0, WavePolicy.glow(110, 0, 0, 0, 100, 1));
        equal(0, WavePolicy.glow(0, 0, 0, 0, 0, 1));
        equal(0, WavePolicy.glow(0, 0, 0, 0, -5, 1));
        equal(1, WavePolicy.glow(100, 110, 100, 100, 20, 2));
        equal(0, WavePolicy.glow(100, 180, 100, 100, 20, 2));
        System.out.println("PASS: zero coordinates, symmetric expansion, row isolation and density");
    }
}
