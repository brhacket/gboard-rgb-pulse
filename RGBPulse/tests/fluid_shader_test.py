"""Desktop Skia checks of the magnetic fluid shader: not Android/Gboard device verification."""
import struct
from pathlib import Path
import numpy as np
import skia

ROOT = Path(__file__).resolve().parents[1]
src = (ROOT / 'shaders' / 'fluid.agsl').read_text()
effect = skia.RuntimeEffect.MakeForShader(src)
assert effect is not None, 'fluid.agsl must compile as a Skia runtime effect'
W, H = 360, 180
BLOBS = 12
TOUCHES = 4


def pack(time=0.4, strength=0.7, hue=0.75, hue2=0.55, sat=0.5, tilt=(0.0, 0.0),
         ripple=1.0, glow=1.0, detail=1.0, blobs=None, touches=None, vels=None):
    # Declaration order: float2 resolution, ten scalars, blobs[12], touches[4],
    # touchVel[4] — every float4 array starts 16-byte aligned.
    values = [W, H, time, strength, hue, hue2, sat, tilt[0], tilt[1], ripple, glow, detail]
    blob_rows = blobs if blobs is not None else [[150, 90, 42, 0.2], [185, 95, 38, 0.0], [215, 85, 34, 0.5]]
    for row in blob_rows:
        values += list(row)
    for _ in range(BLOBS - len(blob_rows)):
        values += [0, -9999, 0, 0]  # parked far away with zero radius
    touch_rows = touches if touches is not None else []
    for row in touch_rows:
        values += list(row)
    for _ in range(TOUCHES - len(touch_rows)):
        values += [0, 0, 0, 0]
    vel_rows = vels if vels is not None else [list(t[:2]) + [t[3], 0.0] for t in (touch_rows or [])]
    for row in vel_rows:
        values += list(row)
    for _ in range(TOUCHES - len(vel_rows)):
        values += [0, 0, 0, 0]
    assert len(values) == 12 + BLOBS * 4 + TOUCHES * 8
    return skia.Data.MakeWithCopy(struct.pack('=' + str(len(values)) + 'f', *values))


def render(data, padded=False):
    pad = 6 if padded else 0
    surface = skia.Surface(W + 2 * pad, H + 2 * pad)
    canvas = surface.getCanvas()
    canvas.clear(skia.ColorTRANSPARENT)
    canvas.translate(pad, pad)
    canvas.clipRect(skia.Rect.MakeWH(W, H))
    canvas.drawPaint(skia.Paint(Shader=effect.makeShader(data)))
    return surface.makeImageSnapshot().toarray(colorType=skia.ColorType.kRGBA_8888_ColorType)


base = render(pack())
alpha = base[:, :, 3]
# Liquid body renders where the blobs are and nowhere near the corners.
assert alpha[60:120, 140:240].max() > 60, 'fluid body visible around the blob cluster'
assert alpha[:12, :12].max() == 0 and alpha[-12:, -12:].max() == 0
assert alpha.max() <= int(0.88 * 0.7 * 255) + 1, 'alpha bounded by strength'
# Where the liquid is visible it also carries color, not just alpha.
lit = alpha > 40
assert base[lit][:, :3].max() > 40, 'visible liquid has visible color'
# Zero strength renders nothing.
assert not render(pack(strength=0.0)).any()
# A finger magnetically extends the surface toward an otherwise empty corner.
spot = (40, 36)
empty = render(pack(touches=[]))
assert empty[spot[1], spot[0], 3] == 0
magnet = render(pack(touches=[[spot[0], spot[1], 1.0, 0.2]]))
assert magnet[spot[1], spot[0], 3] > 30, 'touch pulls the liquid toward it'
assert magnet[spot[1] - 6:spot[1] + 6, spot[0] - 6:spot[0] + 6, 3].max() > 40
# A fast finger leaves a bright drag streak behind it.
streak = render(pack(touches=[[180, 90, 1.0, 0.9]], vels=[[900, 0, 0.9, 0]]))
no_streak = render(pack(touches=[[180, 90, 1.0, 0.9]], vels=[[0, 0, 0, 0]]))
assert streak[:, :140, :3].astype(float).sum() > no_streak[:, :140, :3].astype(float).sum(), 'drag streak trails the motion'
# A fast drop stretches along its velocity instead of staying round.
round_a = render(pack(blobs=[[180, 90, 46, 0.0]], touches=[], vels=[[0, 0, 0, 0]]))
fast_a = render(pack(blobs=[[180, 90, 46, 0.0]], touches=[], vels=[[1500, 0, 1.0, 0]]))


def extent(a):
    ys, xs = np.where(a[:, :, 3] > 40)
    return xs.max() - xs.min(), ys.max() - ys.min()


rw, rh = extent(round_a)
fw, fh = extent(fast_a)
assert abs(rw - rh) < 8, 'a resting drop is round'
assert fw > rw + 12 and fh < rh, 'a moving drop stretches along its motion'
# Tilting the device changes the lighting of the same surface.
left = render(pack(tilt=(-1.0, 0.0)))
right = render(pack(tilt=(1.0, 0.0)))
assert np.abs(left.astype(float) - right.astype(float)).sum() > 400, 'tilt relights the fluid'
# Charged blobs glow differently than dead ones.
charged = render(pack(blobs=[[150, 90, 42, 1.0], [185, 95, 38, 1.0], [215, 85, 34, 1.0]]))
assert np.abs(charged.astype(float) - base.astype(float)).mean() > 1.0
# Time drives the internal shimmer.
early = render(pack(time=0.1))
late = render(pack(time=2.3))
assert np.abs(early.astype(float)[60:120, 140:240] - late.astype(float)[60:120, 140:240]).sum() > 120
# Clipping: nothing leaks outside the requested panel rectangle.
clipped = render(pack(), padded=True)
assert not clipped[:6].any() and not clipped[-6:].any() and not clipped[:, :6].any() and not clipped[:, -6:].any()
print('PASS: magnetic fluid shader compiles, stays clipped and bounded, and reacts to touches, tilt, charge and time')
