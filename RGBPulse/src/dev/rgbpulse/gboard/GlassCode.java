package dev.rgbpulse.gboard;
final class GlassCode { static final String SOURCE =
"// Clear etched glass. lighting is OUR procedural field, not captured editor content.\n" +
"// Rounded-box lens coordinates remain in panel space; material noise is key-local.\n" +
"uniform shader lighting;\n" +
"uniform float4 box;\n" +
"uniform float density;\n" +
"uniform float optical;\n" +
"uniform float pressed;\n" +
"uniform float textureAmount;\n" +
"uniform float lensAmount;\n" +
"float noise(float2 p) {\n" +
"    float3 p3=fract(float3(p.xyx)*.1031);\n" +
"    p3+=dot(p3,p3.yzx+33.33);\n" +
"    return fract((p3.x+p3.y)*p3.z);\n" +
"}\n" +
"float sd(float2 p) {\n" +
"    float2 halfSize=max((box.zw-box.xy)*.5,float2(1));\n" +
"    float radius=min(10.0*density,min(halfSize.x,halfSize.y)*.58);\n" +
"    float2 q=abs(p-(box.xy+box.zw)*.5)-halfSize+radius;\n" +
"    return length(max(q,0.0))+min(max(q.x,q.y),0.0)-radius;\n" +
"}\n" +
"float sq(float v){return v*v;}\n" +
"half4 main(float2 p) {\n" +
"    float d=sd(p), cov=1.0-smoothstep(-.7,.7,d);\n" +
"    if(cov<=0.0)return half4(0.0);\n" +
"    float2 center=(box.xy+box.zw)*.5;\n" +
"    float2 halfSize=max((box.zw-box.xy)*.5,float2(1));\n" +
"    float2 local=(p-center)/halfSize;\n" +
"    float2 grad=float2(sd(p+float2(.5,0))-sd(p-float2(.5,0)),sd(p+float2(0,.5))-sd(p-float2(0,.5)));\n" +
"    float2 n=grad/max(length(grad),.0001);\n" +
"    float shortHalf=min(halfSize.x,halfSize.y);\n" +
"    float bevel=min(6.5*density,shortHalf*.55);\n" +
"    float t=clamp(-d/max(bevel,1.0),0.0,1.0);\n" +
"    // Inverse-power profile: pronounced folded rim, quiet central viewing area.\n" +
"    float slope=(pow(1.0+4.0*t,-1.6)-pow(5.0,-1.6))/(1.0-pow(5.0,-1.6));\n" +
"    if(optical>.5) {\n" +
"        float refr=min(6.0*density,shortHalf*.38)*lensAmount;\n" +
"        float2 offset=-n*slope*refr*(1.0+.22*pressed);\n" +
"        // Gentle center magnification, scaled to the SHORT side so spacebars don't stretch.\n" +
"        float dome=max(0.0,1.0-dot(local,local)*.5);\n" +
"        offset-=local*shortHalf*dome*(.035+.02*pressed)*lensAmount;\n" +
"        float spread=.075*slope;\n" +
"        // Child field is defined at any coordinate: no framebuffer clip sampling needed.\n" +
"        half4 rr=lighting.eval(p+offset*(1.0-spread));\n" +
"        half4 gg=lighting.eval(p+offset);\n" +
"        half4 bb=lighting.eval(p+offset*(1.0+spread));\n" +
"        float alpha=max(rr.a,max(gg.a,bb.a));\n" +
"        float3 rgb=min(float3(rr.r,gg.g,bb.b),float3(alpha));\n" +
"        return half4(rgb,alpha)*cov;\n" +
"    }\n" +
"    float2 dpLocal=(p-box.xy)/max(density,.1);\n" +
"    // Stable etched microfacets: sub-dp grain and faint directional striations.\n" +
"    float fine=noise(floor(dpLocal*2.2));\n" +
"    float grain=noise(floor(dpLocal*.85));\n" +
"    float brushed=sin(dpLocal.y*8.3+sin(dpLocal.x*.38)*.7)*.5+.5;\n" +
"    float micro=(fine*.60+grain*.25+brushed*.15)-.5;\n" +
"    float facing=dot(n,float2(-.8660254,-.5));\n" +
"    float lobe=pow(abs(facing),4.5);\n" +
"    float hair=exp(-sq((d+.60*density)/(.60*density)));\n" +
"    float inner=exp(-sq((d+2.0*density)/(1.65*density)));\n" +
"    // Broad softbox reflection on a curved surface, not a uniform outline.\n" +
"    float wash=exp(-sq((local.y+.62+local.x*.38)/.55))*.095;\n" +
"    float second=exp(-sq((local.y-.92+local.x*.3)/.26))*.028;\n" +
"    float spec=(hair*.50+inner*.095)*lobe*(1.0-.18*pressed);\n" +
"    float body=.055+wash+second+textureAmount*(.045+micro*.13);\n" +
"    float a=clamp(body+spec+pressed*.018,0.0,.85)*cov;\n" +
"    float rough=clamp(.66+micro*textureAmount*.50,0.0,1.0);\n" +
"    float3 base=mix(float3(.35,.44,.56),float3(.82,.89,.96),rough);\n" +
"    float3 col=mix(base,float3(.94,.97,1.0),clamp(spec/max(a,.001),0.0,1.0));\n" +
"    return half4(col*a,a);\n" +
"}\n";
}
