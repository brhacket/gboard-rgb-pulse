package dev.rgbpulse.gboard;

import android.content.Context;
import android.graphics.*;

/** Font loading and satin legend paint; no access to editor text. Shared by preview and hook. */
final class Typography {
    static String modulePath;
    static Typeface face=Typeface.DEFAULT;
    static String loaded="";
    static RuntimeShader ink;
    static boolean shaderFailed;
    static void configure(Config c,Context context) {
        String signature=c.font+":"+c.bold+":"+c.fontData.hashCode();
        if(signature.equals(loaded))return;
        loaded=signature;
        String[] families={"sans-serif","sans-serif-medium","sans-serif-rounded","monospace","serif"};
        face=Typeface.create(families[Math.min(c.font,4)],c.bold?Typeface.BOLD:Typeface.NORMAL);
        try {
            if(c.font>=6){
                // Direct APK access avoids Android package-visibility filtering in Gboard.
                String apk=context.getPackageName().equals("dev.rgbpulse.gboard")?context.getApplicationInfo().sourceDir:modulePath;
                String[] bundled={"manrope","outfit","spacegrotesk","syne","orbitron"};
                String entry="assets/fonts/"+bundled[Math.max(0,Math.min(4,c.font-6))]+".ttf";
                int weight=c.bold?700:(c.font==6?550:500);
                try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(apk)){
                    java.util.zip.ZipEntry e=zip.getEntry(entry);
                    if(e==null||e.getSize()<1||e.getSize()>1048576)throw new java.io.IOException("Invalid bundled font");
                    java.nio.ByteBuffer bytes=java.nio.ByteBuffer.allocateDirect((int)e.getSize());
                    try(java.io.InputStream in=zip.getInputStream(e)){
                        byte[] block=new byte[8192];int n;while((n=in.read(block))>0)bytes.put(block,0,n);
                    }
                    bytes.flip();
                    android.graphics.fonts.Font f=new android.graphics.fonts.Font.Builder(bytes)
                        .setFontVariationSettings("'wght' "+weight).setWeight(weight).build();
                    face=new Typeface.CustomFallbackBuilder(new android.graphics.fonts.FontFamily.Builder(f).build())
                        .setSystemFallback("sans-serif").setStyle(new android.graphics.fonts.FontStyle(weight,android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT)).build();
                }
            }else if(c.font==5 && !c.fontData.isEmpty()){
                byte[] bytes=android.util.Base64.decode(c.fontData,0);
                if(bytes.length>1048576)throw new IllegalArgumentException("Font too large");
                java.io.File f=java.io.File.createTempFile("rgb-font-",".ttf",context.getCacheDir());
                try {
                    try(java.io.FileOutputStream out=new java.io.FileOutputStream(f)){out.write(bytes);}
                    face=Typeface.create(Typeface.createFromFile(f),c.bold?Typeface.BOLD:Typeface.NORMAL);
                }finally{f.delete();}
            }
        }catch(Exception e){face=Typeface.create("sans-serif",c.bold?Typeface.BOLD:Typeface.NORMAL);android.util.Log.w("RGBPulse","Font unavailable; using system fallback",e);}
    }
    static void finishPaint(Paint paint,Config c,float x,float baseline){
        finishPaint(paint,c,x,baseline,0);
    }
    static void finishPaint(Paint paint,Config c,float x,float baseline,float wave){
        float brightness=c.letterBrightness/100f+(1-c.letterBrightness/100f)*Math.max(0,Math.min(1,wave));
        // Small halo and a crisp material-filled core, not a thick white blur.
        paint.setAntiAlias(true);paint.setSubpixelText(true);
        paint.clearShadowLayer();paint.setShader(null);
        int tint=Color.HSVToColor(new float[]{c.hue1,.055f,brightness});
        int originalAlpha=paint.getAlpha();paint.setColor(tint);paint.setAlpha(originalAlpha);
        if(c.glow>0){
            int glow=Color.HSVToColor(Math.round(160*c.glow/100f*brightness),new float[]{c.hue1,.36f,1});
            paint.setShadowLayer(Math.max(.5f,paint.getTextSize()*.065f*c.glow/100f),0,0,glow);
        }
        if(c.letterTexture>0 && !shaderFailed){
            try {
                if(ink==null)ink=new RuntimeShader(LetterCode.SOURCE);
                ink.setFloatUniform("origin",x,baseline);ink.setFloatUniform("textSize",paint.getTextSize());
                ink.setFloatUniform("amount",c.letterTexture/100f);
                ink.setFloatUniform("tint",Color.red(tint)/255f,Color.green(tint)/255f,Color.blue(tint)/255f);
                paint.setShader(ink);
            }catch(RuntimeException e){shaderFailed=true;paint.setShader(null);}
        }
    }
}
