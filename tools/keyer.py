#!/usr/bin/env python3
"""Adaptive chroma-key: samples the actual background color from image borders
(AI models render 'pure magenta' as pink ~D23C8B), keys it out with a soft ramp,
suppresses spill, trims to content, outputs 512 transparent PNG.
Usage: keyer.py in.png out_512.png [fill_ratio]"""
import sys
from PIL import Image, ImageFilter

def key(src_path, dst_path, fill=0.80):
    im = Image.open(src_path).convert('RGB')
    w, h = im.size
    px = im.load()
    # border pixels -> per-channel median = background color
    border = []
    for x in range(0, w, 7):
        for y in (0, 1, 2, 3, h - 4, h - 3, h - 2, h - 1):
            border.append(px[x, y])
    for y in range(0, h, 7):
        for x in (0, 1, 2, 3, w - 4, w - 3, w - 2, w - 1):
            border.append(px[x, y])
    rs = sorted(p[0] for p in border); gs = sorted(p[1] for p in border); bs = sorted(p[2] for p in border)
    m = len(border) // 2
    r0, g0, b0 = rs[m], gs[m], bs[m]
    l0 = float(r0 + g0 + b0)
    cr0, cg0 = r0 / l0, g0 / l0  # background chromaticity: shadows/glow pools share it
    out = Image.new('RGBA', (w, h))
    op = out.load()
    minx, miny, maxx, maxy = w, h, 0, 0
    lo, hi = 0.015, 0.06  # chromaticity distance ramp
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            lum = r + g + b
            if lum == 0:
                op[x, y] = (0, 0, 0, 0)
                continue
            cd = ((r / lum - cr0) ** 2 + (g / lum - cg0) ** 2
                  + ((r + g) / lum - (cr0 + cg0)) ** 2) ** 0.5
            a = 0.0 if cd <= lo else 255.0 if cd >= hi else 255.0 * (cd - lo) / (hi - lo)
            ai = int(a)
            if ai > 60:
                # pink spill suppression: lift G toward R,B lean where pixel is edge-mixed
                if r - g > 60 and b - g > 20:
                    over = min(r - g, (b - g)) * 0.4
                    r = int(r - over); b = int(b - over)
                op[x, y] = (r, g, b, ai)
                if x < minx: minx = x
                if x > maxx: maxx = x
                if y < miny: miny = y
                if y > maxy: maxy = y
            else:
                op[x, y] = (0, 0, 0, 0)
    if maxx == 0:
        raise SystemExit(f'nothing keyed out — bg=({r0},{g0},{b0})?')
    # --- pass 2: magenta despill (bg rim light baked into art) + defringe ---
    # stronger the closer the pixel is to the keyed edge (lower alpha)
    for y in range(miny, maxy + 1):
        for x in range(minx, maxx + 1):
            a_cur = op[x, y][3]
            if a_cur == 0:
                continue
            r, g, b, _ = op[x, y]
            lean = min(r - g, b - g)
            if lean > 12:
                k = 0.85 if a_cur < 250 else 0.55  # edge pixels: near-total despill
                cut = int(lean * k)
                op[x, y] = (max(r - cut, 0), g, max(b - cut, 0), a_cur)
    # defringe: erode alpha 1px (min filter) then soften 1px
    alpha = out.split()[3].filter(ImageFilter.MinFilter(3))
    alpha = alpha.filter(ImageFilter.GaussianBlur(0.8))
    out.putalpha(alpha)
    bw, bh = maxx - minx, maxy - miny
    size = int(max(bw, bh) / fill)
    ox, oy = minx - (size - bw) // 2, miny - (size - bh) // 2
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    crop = out.crop((max(ox, 0), max(oy, 0), min(ox + size, w), min(oy + size, h)))
    canvas.paste(crop, (max(0, -ox), max(0, -oy)))
    res = canvas.resize((512, 512), Image.Resampling.LANCZOS)
    res.save(dst_path)
    print(f'{dst_path} bg=({r0},{g0},{b0}) bbox={bw}x{bh}')

if __name__ == '__main__':
    key(sys.argv[1], sys.argv[2], float(sys.argv[3]) if len(sys.argv) > 3 else 0.80)
