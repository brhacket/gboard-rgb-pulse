package dev.rgbpulse.gboard;
final class PanelGlassCode { static final String SOURCE =
"// Weighted sharp / Gaussian branches combined with PLUS (premultiplied addition).\n" +
"// Neither branch contains legends, editor content or a captured View hierarchy.\n" +
"uniform shader backdrop;\n" +
"uniform float2 resolution;\n" +
"uniform float density;\n" +
"uniform float lensAmount;\n" +
"uniform float textureAmount;\n" +
"uniform float frostAmount;\n" +
"uniform float roundness;\n" +
"uniform float metalAmount;\n" +
"uniform float blurredPass;\n" +
"uniform float keyCount;\n" +
"uniform float4 keys[64];\n" +
"// Rounded lens outline. At maximum roundness letter keys approach circular bubbles;\n" +
"// wide keys retain their width as capsules. Touch rectangles remain unchanged.\n" +
"float sd(float2 p,float4 b){\n" +
" float2 h=max((b.zw-b.xy)*.5,float2(1.0));\n" +
" float radius=min(h.x,h.y)*mix(.40,1.0,clamp(roundness,0.0,1.0));\n" +
" float2 q=abs(p-(b.xy+b.zw)*.5)-h+radius;\n" +
" return length(max(q,0.0))+min(max(q.x,q.y),0.0)-radius;\n" +
"}\n" +
"float hash(float2 p){float3 q=fract(float3(p.xyx)*.1031);q+=dot(q,q.yzx+33.33);return fract((q.x+q.y)*q.z);}\n" +
"half4 main(float2 p){\n" +
" if(p.x<0.0||p.y<0.0||p.x>=resolution.x||p.y>=resolution.y)return half4(0);\n" +
" float4 b=float4(0.0);float d=1e5;\n" +
" for(int i=0;i<64;i++){\n" +
"  if(float(i)>=keyCount)break;\n" +
"  float4 k=keys[i];\n" +
"  if(k.z<=k.x||k.w<=k.y||p.x<k.x-1.0||p.y<k.y-1.0||p.x>k.z+1.0||p.y>k.w+1.0)continue;\n" +
"  float distance=sd(p,k);\n" +
"  if(distance<d){d=distance;b=k;}\n" +
" }\n" +
" float2 lo=float2(.5),hi=max(resolution-.5,lo);\n" +
" half4 original=backdrop.eval(clamp(p,lo,hi));\n" +
" if(d>.7)return blurredPass>.5?half4(0):original;\n" +
" float cov=1.0-smoothstep(-.7,.7,d);\n" +
" float2 center=(b.xy+b.zw)*.5,h=(b.zw-b.xy)*.5;\n" +
" float2 q=(p-center)/max(h,float2(1));\n" +
" float2 normal=float2(sd(p+float2(.5,0),b)-sd(p-float2(.5,0),b),sd(p+float2(0,.5),b)-sd(p-float2(0,.5),b));\n" +
" normal/=max(length(normal),.0001);\n" +
" float halfShort=min(h.x,h.y),bevel=min(9.0*density,halfShort*.78);\n" +
" float t=clamp(-d/max(bevel,1.0),0.0,1.0);\n" +
" float slope=(pow(1.0+4.0*t,-1.35)-pow(5.0,-1.35))/(1.0-pow(5.0,-1.35));\n" +
" float2 bend=-normal*slope*min(9.5*density,halfShort*.70)*lensAmount;\n" +
" bend-=q*halfShort*max(0.0,1.0-dot(q,q)*.5)*.13*lensAmount;\n" +
" float dispersion=.035*slope;\n" +
" half4 red=backdrop.eval(clamp(p+bend*(1.0-dispersion),lo,hi));\n" +
" half4 green=backdrop.eval(clamp(p+bend,lo,hi));\n" +
" half4 blue=backdrop.eval(clamp(p+bend*(1.0+dispersion),lo,hi));\n" +
" float alpha=max(red.a,max(green.a,blue.a));\n" +
" float3 rgb=min(float3(red.r,green.g,blue.b),float3(alpha));\n" +
" float frost=clamp(frostAmount,0.0,1.0);\n" +
" float amount=smoothstep(0.0,.85,frost);\n" +
" // Convex glass: thin Fresnel edge, inner returning highlight and broad sky reflection.\n" +
" // No solid colored cap: diffuse backdrop remains visible through the curved surface.\n" +
" float facing=dot(normal,float2(-.60,-.80));\n" +
" float fresnel=exp(-max(-d,0.0)/max(.85*density,.5));\n" +
" float outer=fresnel*(.075+.52*pow(max(facing,0.0),3.0)+.18*pow(max(-facing,0.0),5.0));\n" +
" float innerDistance=(d+2.2*density)/max(.6*density,.5);\n" +
" float innerLine=exp(-innerDistance*innerDistance);\n" +
" float returned=innerLine*.12*pow(max(-facing,0.0),3.0);\n" +
" float ell=q.x*q.x*.8+(q.y+.72)*(q.y+.72)*2.8;\n" +
" float sky=exp(-ell*2.2)*.16;\n" +
" float lower=exp(-(q.x*q.x*2.0+(q.y-.82)*(q.y-.82)*20.0))*.035;\n" +
" float grain=(hash(floor((p-b.xy)/density*3.1))-.5)*.012*textureAmount;\n" +
" float top=clamp(.006+.025*frost+outer+returned+sky+lower+grain,0.0,.80);\n" +
" // Thin brushed-silver rim, inside the lens boundary; directional bright/dark bands.\n" +
" float rim=1.0-smoothstep(.65*density,1.35*density,max(-d,0.0));\n" +
" float metallic=clamp(metalAmount,0.0,1.0)*rim;\n" +
" float silver=.28+.65*pow(abs(facing),3.0);\n" +
" float brush=(hash(floor(p/density*3.0))-.5)*.028;\n" +
" float3 material=float3(.92,.96,1.0)*top;\n" +
" material=material*(1.0-metallic)+float3(silver*.94,silver*.97,silver+brush)*metallic;\n" +
" top=top+metallic*(1.0-top);\n" +
" // PLUS sum equals one weighted image, even for transparent backgrounds.\n" +
" // Material is added in the sharp branch only; the diffuse branch contributes no rim.\n" +
" if(blurredPass>.5)return half4(rgb,alpha)*(cov*amount*(1.0-top));\n" +
" rgb=material+rgb*((1.0-top)*(1.0-amount));\n" +
" alpha=top+alpha*((1.0-top)*(1.0-amount));\n" +
" return original*(1.0-cov)+half4(rgb,alpha)*cov;\n" +
"}\n";
}
