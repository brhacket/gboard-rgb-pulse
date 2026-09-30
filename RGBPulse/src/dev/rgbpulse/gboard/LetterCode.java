package dev.rgbpulse.gboard;
final class LetterCode { static final String SOURCE =
"// Satin etched legend; static glyph-local variation. Paint clips this shader to glyphs.\n" +
"uniform float2 origin;\n" +
"uniform float textSize;\n" +
"uniform float amount;\n" +
"uniform float3 tint;\n" +
"float hash(float2 p) {float3 q=fract(float3(p.xyx)*.1031);q+=dot(q,q.yzx+33.33);return fract((q.x+q.y)*q.z);}\n" +
"half4 main(float2 p){\n" +
"    float2 uv=(p-origin)/max(textSize,1.0);\n" +
"    float grain=hash(floor(uv*54.0))-.5;\n" +
"    float y=clamp(-uv.y,0.0,1.0);\n" +
"    float satin=.90+.08*y+amount*(grain*.17+sin(uv.y*180.0)*.015);\n" +
"    return half4(clamp(tint*satin,0.0,1.0),1.0);\n" +
"}\n";
}
