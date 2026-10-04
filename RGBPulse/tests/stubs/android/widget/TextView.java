package android.widget;
import android.content.res.ColorStateList;
public class TextView extends android.view.View {
    private ColorStateList colors=ColorStateList.valueOf(0xffeeeeee);
    public ColorStateList getTextColors(){return colors;}
    public int getCurrentTextColor(){return colors.getDefaultColor();}
    public void setTextColor(ColorStateList value){colors=value;}
}
