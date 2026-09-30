package dev.rgbpulse.gboard;

/** Geometry-only cap classification, shared with non-Android tests. Never identifies key text. */
final class CapPolicy {
    static boolean axisAligned(float[] m){
        return m!=null&&m.length==9&&Float.isFinite(m[0])&&Float.isFinite(m[4])&&Float.isFinite(m[2])&&Float.isFinite(m[5])
            &&m[0]>0&&m[4]>0&&Math.abs(m[1])<.0001f&&Math.abs(m[3])<.0001f
            &&Math.abs(m[6])<.0001f&&Math.abs(m[7])<.0001f&&Math.abs(m[8]-1)<.0001f;
    }
    /** One closed convex sampled contour with substantial interior, not an arrow or outline. */
    static boolean plate(float[] points,float width,float height){
        if(points==null||points.length<16||points.length%2!=0||width<=0||height<=0)return false;
        int n=points.length/2;double twiceArea=0;int direction=0;
        for(int i=0;i<n;i++){
            int j=(i+1)%n,k=(i+2)%n;float x=points[i*2],y=points[i*2+1];
            if(!Float.isFinite(x)||!Float.isFinite(y))return false;
            twiceArea+=(double)x*points[j*2+1]-(double)y*points[j*2];
            float cross=(points[j*2]-x)*(points[k*2+1]-points[j*2+1])-(points[j*2+1]-y)*(points[k*2]-points[j*2]);
            if(Math.abs(cross)>.001f){int sign=cross>0?1:-1;if(direction!=0&&sign!=direction)return false;direction=sign;}
        }
        double area=Math.abs(twiceArea)*.5;
        return direction!=0&&area>=width*height*.72&&area<=width*height*1.01;
    }
    /** Asymmetric row-edge touch areas: requires a known plate width/height, never letters. */
    static boolean edgeCap(float l,float t,float r,float b,float kw,float kh,float knownW,float knownH){
        float w=r-l,h=b-t;
        return Float.isFinite(l)&&Float.isFinite(t)&&Float.isFinite(r)&&Float.isFinite(b)
            &&kw>0&&kh>0&&knownW>0&&knownH>0
            &&Math.abs(w-knownW)<=knownW*.10f&&Math.abs(h-knownH)<=knownH*.10f
            &&w>=kw*.5f&&h>=kh*.62f&&w*h>=kw*kh*.36f
            &&l>=0&&r<=kw&&t>=-kh*.02f&&b<=kh*1.02f
            &&Math.abs((t+b)/2-kh/2)<=kh*.12f;
    }
    static boolean paddedCap(float l,float t,float r,float b,float kw,float kh,float pl,float pt,float pr,float pb){
        float cw=kw-pl-pr,ch=kh-pt-pb;
        // Padding must describe a substantial content area, not an icon-sized inset.
        return pl>=0&&pt>=0&&pr>=0&&pb>=0&&cw>=kw*.55f&&ch>=kh*.70f
            &&(r-l)>=cw*.80f&&(b-t)>=ch*.80f
            &&cap(l-pl,t-pt,r-pl,b-pt,cw,ch);
    }
    static boolean offsetPlate(float l,float t,float r,float b,float kw,float kh){
        return Float.isFinite(l)&&Float.isFinite(t)&&Float.isFinite(r)&&Float.isFinite(b)&&kw>0&&kh>0
            &&l>=0&&t>=0&&r<=kw&&b<=kh&&(r-l)>=kw*.42f&&(r-l)<=kw*.85f
            &&(b-t)>=kh*.70f&&(r-l)*(b-t)>=kw*kh*.32f
            &&Math.abs((t+b)/2-kh/2)<=kh*.12f;
    }
    static boolean cap(float l,float t,float r,float b,float kw,float kh) {
        if(!Float.isFinite(l)||!Float.isFinite(t)||!Float.isFinite(r)||!Float.isFinite(b)||kw<=0||kh<=0)return false;
        float w=r-l,h=b-t;
        // Full key plates or centered action pills. Small glyphs/icons are intentionally excluded.
        return w>=kw*.62f&&h>=kh*.62f&&w*h>=kw*kh*.45f
            &&w<=kw*1.08f&&h<=kh*1.08f&&l>=-kw*.04f&&t>=-kh*.04f
            &&r<=kw*1.04f&&b<=kh*1.04f
            &&Math.abs((l+r)/2-kw/2)<=kw*.12f&&Math.abs((t+b)/2-kh/2)<=kh*.12f;
    }
}
