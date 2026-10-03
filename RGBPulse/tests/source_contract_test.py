"""Catalog, clean removal, shader sync and no diagnostic border."""
from pathlib import Path
import re,subprocess,sys
root=Path(__file__).resolve().parents[1];src=root/'src/dev/rgbpulse/gboard'
cfg=(src/'Config.java').read_text()
effects=re.findall(r'"([^"]+)"',cfg.split('String[] EFFECTS = {',1)[1].split('};',1)[0])
assert len(effects)==10 and len(set(effects))==10
assert effects[0]=='Edge runner' and effects[-1]=='Wide aurora'
assert 'pulse47' in cfg and 'tapFx6' not in cfg
assert 'effect = 0' in cfg
assert 'particles' not in cfg and 'AMBIENT' not in cfg
ui=(src/'SettingsActivity.java').read_text();fx=(src/'Fx.java').read_text()
# Legacy catalog remains loadable, but the simplified UI intentionally exposes one effect.
assert 'Browse animations' not in ui and 'Make every tap yours.' in ui
assert 'slider(col, "Particles"' not in ui and 'Idle background' not in ui
assert 'Bitmap' not in fx and 'Burst' not in fx
assert 'cfg.pulseDuration' in fx and 'Config.LIQUID_SHUFFLE' not in fx
shader=(root/'shaders/field.agsl').read_text()
for token in ['Comet fan','Prism shards','Floating hearts','Galaxy swirl','Twinkle stars','Sprite','starX']:
    assert token not in shader and token not in fx
for name in ['PulseModule.java','SettingsActivity.java']:
    text=(src/name).read_text()
    assert '0xFF00FFCC' not in text and 'Paint diagnostic' not in text and 'Paint outline' not in text
assert 'Detailed layout logs (no drawing)' in ui
old=(src/'ShaderCode.java').read_text()
subprocess.run([sys.executable,str(root/'embed_shader.py')],check=True)
assert old==(src/'ShaderCode.java').read_text()
assert 'GPU_COUNT = 10' in cfg
print('PASS: curated pulse catalog, migration, no idle animation and shader sync')

field=(src/'FieldFx.java').read_text()
assert 'next=(next+1)%3' in field and 'if(style>=8)clear()' in field
assert 'RectF key' in fx and 'keyBounds' in field and 'keyBounds[4]' in shader
assert 'float cap=style<7.5?.48:.28' in shader
assert 'Rightward chase' not in cfg and 'Heartbeat' not in cfg and 'Inward sweep' not in cfg
print('PASS: exact ten-effect catalog, actual key footprint, independent short duration and bounded overlap')
