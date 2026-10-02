package dev.rgbpulse.gboard;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.res.ColorStateList;

/** Adapter logic with compile-only Android stubs, not an emulator integration test. */
public final class NativeLegendsTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        Config cfg=new Config();cfg.ripple=true;cfg.letterInactive=0xff123456;cfg.letterActive=0xffabcdef;
        ViewGroup key=new ViewGroup();TextView text=new TextView();key.children.add(text);
        ColorStateList original=text.getTextColors();NativeLegends labels=new NativeLegends();
        labels.bind(key);labels.apply(cfg,0);check(text.getCurrentTextColor()==cfg.letterInactive);
        labels.apply(cfg,1);check(text.getCurrentTextColor()==cfg.letterActive);
        cfg.ripple=false;labels.apply(cfg,0);check(text.getTextColors()==original);
        cfg.ripple=true;labels.bind(key);labels.apply(cfg,1);
        ColorStateList updated=ColorStateList.valueOf(0x80cccccc);text.setTextColor(updated);
        labels.apply(cfg,1);check((text.getCurrentTextColor()>>>24)==128);
        key.children.clear();labels.bind(key);check(text.getTextColors()==updated);
        key.children.add(text);labels.bind(key);labels.apply(cfg,1);labels.restore();check(text.getTextColors()==updated);
        System.out.println("PASS: native legend color application, disable/detach restoration and theme-change preservation (stubs)");
    }
}
