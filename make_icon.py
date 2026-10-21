#!/usr/bin/env python3
"""Generate the Dermes adaptive-icon foreground: a mercury-trailing 'D' comet
on transparent 430x430 (adaptive safe zone = center 66x66dp of 108dp)."""
import math
from PIL import Image, ImageDraw, ImageFilter

S = 430  # supersample then downscale
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

cx, cy = S // 2, S // 2

# --- comet tail: tapered arc trailing down-left behind the D ---
pts = []
for i in range(80):
    t = i / 79.0
    ang = math.radians(-55 + t * 150)          # sweeps from upper-right to lower-left
    r = S * (0.30 - 0.16 * t)                   # radius shrinks toward tail tip
    x = cx + r * math.cos(ang)
    y = cy + r * math.sin(ang)
    w = (1 - t) * 26                            # taper width
    pts.append((x, y, w))

# draw tail as stacked glowing circles, far->near so the head overlaps them
for x, y, w in reversed(pts):
    if w < 1: continue
    fade = int(200 * (w / 26))
    d.ellipse([x - w, y - w, x + w, y + w], fill=(88, 166, 255, fade))

# soft glow pass on the tail area
glow = img.filter(ImageFilter.GaussianBlur(18))
img = Image.alpha_composite(glow, img)
d = ImageDraw.Draw(img)

# --- the 'D': thick geometric ring segment + vertical stem (chrome/mercury) ---
stem_x = cx - 78
d.rounded_rectangle([stem_x - 22, cy - 128, stem_x + 22, cy + 128], 22, fill=(232, 240, 254, 255))

bowl_cx = stem_x + 6
# outer bowl edge -> inner bowl edge (circular D bowl), as filled annulus segment
outer, inner = 128, 66
for y in range(cy - outer, cy + outer + 1):
    dy = y - cy
    if abs(dy) >= outer: continue
    xo = math.sqrt(outer * outer - dy * dy)
    xi = math.sqrt(max(0, inner * inner - dy * dy)) if abs(dy) < inner else -1
    x0 = max(stem_x, bowl_cx + (xi if xi >= 0 else 0))
    x1 = bowl_cx + xo
    if x1 > x0:
        # mercury vertical gradient: brighter at top
        t = (y - (cy - outer)) / (2 * outer)
        c = int(200 + 55 * t)
        d.line([(x0, y), (x1, y)], fill=(c, c, 255, 255), width=1)

# highlight slash on the bowl
hl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
hd = ImageDraw.Draw(hl)
for i in range(40):
    t = i / 39.0
    ang = math.radians(-70 + t * 60)
    r = 97
    x = bowl_cx + r * math.cos(ang)
    y = cy + r * math.sin(ang)
    hd.ellipse([x - 7, y - 7, x + 7, y + 7], fill=(255, 255, 255, int(180 * (1 - abs(t - 0.4)))))
img = Image.alpha_composite(img, hl.filter(ImageFilter.GaussianBlur(3)))

# --- downscale with center 66/108 safe-zone padding ---
safe = int(S * 66 / 108)
small = img.resize((safe, safe), Image.LANCZOS)
out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
out.paste(small, ((S - safe) // 2, (S - safe) // 2), small)
for size, path in [(430, "ic_dermes_fg.png"), (108, "ic_dermes_fg_108.png")]:
    out.resize((size, size), Image.LANCZOS).save(path)
print("written")
