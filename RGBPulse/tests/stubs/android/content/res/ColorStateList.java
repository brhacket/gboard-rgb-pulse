package android.content.res;
public class ColorStateList {
    private final int color;
    private ColorStateList(int value){color=value;}
    public static ColorStateList valueOf(int value){return new ColorStateList(value);}
    public int getDefaultColor(){return color;}
    public int getColorForState(int[] state,int fallback){return color;}
}
