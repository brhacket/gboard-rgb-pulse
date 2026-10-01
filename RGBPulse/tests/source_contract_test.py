"""Catalog, clean removal, shader sync and no diagnostic border."""
from pathlib import Path
import re,subprocess,sys
root=Path(__file__).resolve().parents[1];src=root/'src/dev/rgbpulse/gboard'
cfg=(src/'Config.java').read_text()
effects=re.findall(r'"([^"]+)"',cfg.split('String[] EFFECTS = {',1)[1].split('};',1)[0])
assert len(effects)==12 and len(set(effects))==12
assert effects[0]=='Hologram tiles' and effects[-1]=='Shuffle modern waves'
assert 'tapFx6' in cfg and 'tapFx4' not in cfg
assert 'effect = 0' in cfg
assert 'particles' not in cfg and 'AMBIENT' not in cfg
ui=(src/'SettingsActivity.java').read_text();fx=(src/'Fx.java').read_text()
# Legacy catalog remains loadable, but the simplified UI intentionally exposes one effect.
assert 'Browse animations' not in ui and 'Quiet by design.' in ui
assert 'slider(col, "Particles"' not in ui and 'Idle background' not in ui
assert 'Bitmap' not in fx and 'Burst' not in fx
assert '1+rnd.nextInt(Config.GPU_COUNT-1)' in fx
assert 'Config.LIQUID_SHUFFLE' in fx
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
assert 'GPU_COUNT = 11, LIQUID_SHUFFLE = 11' in cfg
print('PASS: clean liquid-only catalog, migration, shuffle, no particles/idle/border, shader sync')
