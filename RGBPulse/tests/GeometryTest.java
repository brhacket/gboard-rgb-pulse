package dev.rgbpulse.gboard;

public class GeometryTest {
    static int checks;
    static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(BodyGeometry.acceptable(false,true,false,32,4,1080,750,1080,720,3,2400),"normal keyboard");
        check(BodyGeometry.acceptable(false,true,false,32,4,650,580,620,550,3,2400),"floating keyboard");
        check(BodyGeometry.acceptable(false,true,false,12,4,1080,750,1080,700,3,2400),"numeric keypad");
        check(!BodyGeometry.acceptable(true,true,false,32,4,1080,750,1080,720,3,2400),"root must never qualify");
        check(!BodyGeometry.acceptable(false,false,false,32,4,1080,750,1080,720,3,2400),"unknown panel fails closed");
        check(!BodyGeometry.acceptable(false,true,false,6,1,1080,100,1080,95,3,2400),"toolbar is not keys body");
        check(!BodyGeometry.acceptable(false,true,false,0,0,1080,750,1080,720,3,2400),"no keys -> no fallback");
        check(!BodyGeometry.acceptable(false,true,false,32,4,1080,2400,1080,750,3,2400),"full IME window rejected");
        check(!BodyGeometry.acceptable(false,true,false,32,4,1080,2400,1080,2400,3,2400),"full screen body rejected");
        check(!BodyGeometry.acceptable(false,true,true,32,4,1080,750,1080,720,3,2400),"transformed keys fail closed");
        check(!BodyGeometry.acceptable(false,true,false,32,4,1080,750,1080,900,3,2400),"clip cannot exceed view");
        check(!BodyGeometry.acceptable(false,true,false,32,4,0,0,0,0,3,2400),"zero bounds");
        check(BodyGeometry.score(32,1080,740)>BodyGeometry.score(32,1080,1400),"tighter group preferred");
        check(BodyGeometry.score(32,1080,740)>BodyGeometry.score(10,1080,120),"full keyboard preferred to one row");
        // Property: every candidate spanning a whole screen is rejected, at every density tested.
        for(int width=320;width<=2560;width+=160)for(int height=480;height<=3200;height+=160)
            check(!BodyGeometry.acceptable(false,true,false,35,4,width,height,width,height,1,height),"no full screen");
        System.out.println("PASS: "+checks+" geometry guards (not Android runtime tests)");
    }
}
