package dev.rgbpulse.gboard;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PanelPolicyTest {
    static final class Node {
        String type; Node parent;
        Node(String type,Node parent){this.type=type;this.parent=parent;}
    }
    static PanelPolicy.Access<Node> access=new PanelPolicy.Access<Node>() {
        public Node parent(Node n){return n.parent;}
        public boolean softKeyboardPanel(Node n){return n.type.equals("SoftKeyboardView");}
    };
    static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
    static void keys(ArrayList<Node> keys,Node row,int n){for(int i=0;i<n;i++)keys.add(new Node("SoftKeyView",row));}
    public static void main(String[] args) {
        // Fixture reconstructed from the user's 2026-09-29 Vector log.
        Node root=new Node("InputView",null), frame=new Node("FrameLayout",root);
        Node holder=new Node("KeyboardHolder",frame);
        Node bar=new Node("SoftKeyboardView",new Node("KeyboardViewHolder",holder));
        Node typing=new Node("SoftKeyboardView",new Node("KeyboardViewHolder",holder));
        Node barRow=new Node("ScaledKeyboardViewInner",bar), row=new Node("LinearLayout",typing);
        ArrayList<Node> all=new ArrayList<Node>(); keys(all,barRow,5); keys(all,row,34);
        LinkedHashMap<Node,ArrayList<Node>> map=PanelPolicy.group(all,root,access);
        ok(map.size()==2,"separate panels");
        ok(!map.containsKey(holder),"holder NEVER a candidate even with 39 combined keys");
        ok(map.get(bar).size()==5,"toolbar owns only its five keys");
        ok(map.get(typing).size()==34,"typing owns only its 34 keys");
        ok(!BodyGeometry.acceptable(false,true,false,5,1,1080,92,1072,92,2.75f,2296),"92px toolbar rejected");
        ok(BodyGeometry.acceptable(false,true,false,34,4,1080,512,1072,495,2.75f,2296),"512px typing panel accepted");
        // Unknown layouts don't get an ancestor fallback.
        all.clear();keys(all,new Node("UnknownPanel",holder),35);
        ok(PanelPolicy.group(all,root,access).isEmpty(),"no SoftKeyboardView -> empty");
        // A nested panel's keys cannot be counted in its ancestor panel.
        Node nested=new Node("SoftKeyboardView",typing);all.clear();keys(all,nested,12);
        map=PanelPolicy.group(all,root,access);
        ok(map.size()==1 && map.containsKey(nested),"nearest panel only");
        // Root stays excluded even if misidentified by an adapter.
        Node panelRoot=new Node("SoftKeyboardView",null);all.clear();keys(all,panelRoot,35);
        ok(PanelPolicy.group(all,panelRoot,access).isEmpty(),"never root");
        System.out.println("PASS: panel ownership regression fixture (9 assertions)");
    }
}
