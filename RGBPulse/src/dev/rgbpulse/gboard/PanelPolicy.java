package dev.rgbpulse.gboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Nearest-panel ownership prevents toolbar keys leaking into the typing panel. */
final class PanelPolicy {
    interface Access<T> {
        T parent(T node);
        boolean softKeyboardPanel(T node);
    }
    static <T> LinkedHashMap<T,ArrayList<T>> group(ArrayList<T> keys,T root,Access<T> access) {
        LinkedHashMap<T,ArrayList<T>> groups=new LinkedHashMap<T,ArrayList<T>>();
        for(T key:keys) {
            T parent=access.parent(key);
            for(int guard=0; parent!=null && parent!=root && guard<40; guard++) {
                if(access.softKeyboardPanel(parent)) {
                    ArrayList<T> list=groups.get(parent);
                    if(list==null) {list=new ArrayList<T>();groups.put(parent,list);}
                    list.add(key);
                    break; // Never aggregate keys at a holder/outer panel.
                }
                parent=access.parent(parent);
            }
        }
        return groups;
    }
}
