package dev.rgbpulse.gboard;

/** Pure geometry guards, also exercised by tests without an Android runtime. */
final class BodyGeometry {
    static boolean acceptable(boolean isInputRoot, boolean namedKeyboard, boolean transformed,
            int keys, int rows, int width, int height, int clipWidth, int clipHeight,
            float density, int screenHeight) {
        if (isInputRoot || !namedKeyboard || transformed || keys < 8 || rows < 3) return false;
        if (width < 100*density || height < 75*density || clipWidth<=0 || clipHeight<=0) return false;
        if (clipWidth>width || clipHeight>height) return false;
        if ((long)clipWidth*clipHeight < (long)width*height*.55) return false;
        return clipHeight <= screenHeight*.85f;
    }
    static int score(int keys, int width, int height) {
        return keys*100000 - (int)((long)width*height/100);
    }
}
