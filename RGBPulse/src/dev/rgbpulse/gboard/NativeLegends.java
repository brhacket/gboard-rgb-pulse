package dev.rgbpulse.gboard;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.*;

/** Fallback for native TextView legends whose cached glyphs bypass Canvas text hooks. */
final class NativeLegends {
    private static final class Entry {
        ColorStateList original, applied;
        Entry(ColorStateList colors){original=colors;}
    }
    private final IdentityHashMap<TextView,Entry> entries=new IdentityHashMap<>();
    void bind(View key){
        Set<TextView> live=Collections.newSetFromMap(new IdentityHashMap<TextView,Boolean>());
        collect(key,live,0);
        for(Iterator<Map.Entry<TextView,Entry>> it=entries.entrySet().iterator();it.hasNext();){
            Map.Entry<TextView,Entry> item=it.next();if(!live.contains(item.getKey())){restore(item.getKey(),item.getValue());it.remove();}
        }
        for(TextView label:live)if(!entries.containsKey(label))entries.put(label,new Entry(label.getTextColors()));
    }
    private void collect(View view,Set<TextView> live,int depth){
        if(depth>5)return;
        if(view instanceof TextView)live.add((TextView)view);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),live,depth+1);}
    }
    void apply(Config cfg,float wave){
        if(!cfg.ripple){restore();return;}
        for(Map.Entry<TextView,Entry> item:entries.entrySet()){
            TextView label=item.getKey();Entry entry=item.getValue();
            if(entry.applied!=null&&label.getTextColors()!=entry.applied)entry.original=label.getTextColors();
            int nativeColor=entry.original.getColorForState(label.getDrawableState(),entry.original.getDefaultColor());
            int color=LegendTint.color(nativeColor,wave,cfg.letterInactive,cfg.letterActive);
            if(entry.applied==null||label.getCurrentTextColor()!=color){entry.applied=ColorStateList.valueOf(color);label.setTextColor(entry.applied);}
        }
    }
    private void restore(TextView label,Entry entry){if(entry.applied!=null&&label.getTextColors()==entry.applied)label.setTextColor(entry.original);entry.applied=null;}
    void restore(){for(Map.Entry<TextView,Entry> item:entries.entrySet())restore(item.getKey(),item.getValue());entries.clear();}
}
