#!/usr/bin/env python3
"""Static preview of the dashboard visual column, mirrors Canvas code in DashboardActivity.
Run: python3 tools/preview.py /tmp/dash_preview.png"""
import math, random, sys
from PIL import Image, ImageDraw, ImageFilter

W, H, S = 412, 900, 2          # phone width dp, column height dp, supersample
BG = (13, 17, 23)

def sc(*args):  # scale dp -> px
    return tuple(a * S for a in args)

img = Image.new("RGB", (W * S, H * S), BG)
d = ImageDraw.Draw(img, "RGBA")
rnd = random.Random(42)

# ---- PULSE (0..150): nebula core + ripple rings -----------------------------
y0, y1 = 0, 150
cx, cy = W / 2, (y0 + y1) / 2 + 6
base = (y1 - y0) * 0.24
# outer haze layers
for rr, al in [(base*4.2, 10), (base*3.0, 16), (base*2.0, 34), (base*1.45, 70)]:
    d.ellipse(sc(cx-rr, y0+ (cy-y0)-rr, cx+rr, (cy-y0)+y0+rr), fill=(88,166,255,al))
# dark core
d.ellipse(sc(cx-base*1.1, cy-base*1.1, cx+base*1.1, cy+base*1.1), fill=BG)
# ripple rings (thicker, additive glow)
for age, width, peak in [(0.15, 5.0, 190), (0.55, 3.5, 130), (1.0, 2.2, 70), (1.35, 1.6, 35)]:
    r = base + (min(W*0.46, (y1-y0)*0.95) - base) * (age/1.6)
    al = int(peak * (1 - age/1.6))
    bbox = sc(cx-r, cy-r, cx+r, cy+r)
    d.ellipse(bbox, outline=(120,190,255,al), width=int(width*S))
    glow = img.filter(ImageFilter.GaussianBlur(2*S)) if False else None

# ---- STARFIELD + GALAXY (150..350) ------------------------------------------
gy0, gy1 = 150, 350
gcx, gcy = W/2, (gy0+gy1)/2
# distant stars
for _ in range(140):
    x, y = rnd.uniform(0, W), rnd.uniform(gy0, gy1)
    r = rnd.choice([0.4, 0.5, 0.7, 0.9, 1.3])
    al = rnd.randint(30, 140)
    tint = rnd.choice([(200,220,255), (255,240,220), (170,200,255)])
    d.ellipse(sc(x-r, y-r, x+r, y+r), fill=tint+(al,))
# orbital ellipses
for orbit in [40, 95]:
    d.ellipse(sc(gcx-orbit*1.6, gcy-orbit*0.6, gcx+orbit*1.6, gcy+orbit*0.6),
              outline=(136,136,136,50), width=1*S)
# central glow
for rr, al in [(70, 30), (45, 50), (25, 90)]:
    d.ellipse(sc(gcx-rr, gcy-rr, gcx+rr, gcy+rr), fill=(88,166,255,al))
# star: Qwen3.8-Flash 76GB
ang = 0.6
sx = gcx + math.cos(ang) * 40 * 1.6
sy = gcy + math.sin(ang) * 40 * 0.6
rad = 10 + 26*0.95
for rr, al in [(rad*2.4, 26), (rad*1.7, 45), (rad*1.25, 80)]:
    d.ellipse(sc(sx-rr, sy-rr, sx+rr, sy+rr), fill=(88,166,255,al))
d.ellipse(sc(sx-rad, sy-rad, sx+rad, sy+rad), fill=(137,193,255,255))
d.ellipse(sc(sx-rad*0.45, sy-rad*0.5, sx+rad*0.3, sy+rad*0.1), fill=(255,255,255,220))

# ---- SPARKLINE (350..470): gradient area + glow line + last dot -------------
sy0, sy1 = 350, 470
samples = [21.4,24.8,23.1,31.6,28.9,26.4,33.2,29.7,27.5,35.1,31.2,38.4,34.9,29.8,36.2,41.0,37.5,32.1,28.4,34.7,
           39.2,44.1,40.6,35.2,30.9,36.8,42.3,47.5,43.2,37.1]
maxv = max(samples)
def pts():
    out = []
    n = len(samples)
    for i, v in enumerate(samples):
        x = 24 + (W-48) * i/(n-1)
        y = sy1 - (sy1-sy0) * 0.9 * v/maxv - 4
        out.append((x, y))
    return out
P = pts()
# area fill: vertical gradient via strips
for i in range(len(P)-1):
    x0, y0_ = P[i]; x1, y1_ = P[i+1]
    steps = 12
    for k in range(steps):
        ta, tb = k/steps, (k+1)/steps
        ya = y0_ + (sy1-y0_)*ta; yb = y0_ + (sy1-y0_)*tb
        al = int(70 * (1 - ta)**1.4)
        d.polygon(sc(x0, ya, x1, ya, x1, yb, x0, yb), fill=(88,166,255,al))
# glow line then line
for w, al in [(8, 26), (4.5, 60), (2.2, 255)]:
    d.line(sc(*[c for p in P for c in p]), fill=(88,166,255,al), width=int(w*S), joint="curve")
lx, ly = P[-1]
d.ellipse(sc(lx-5, ly-5, lx+5, ly+5), fill=(255,255,255,255))
d.ellipse(sc(lx-9, ly-9, lx+9, ly+9), outline=(137,193,255,120), width=2*S)

# ---- ACTIVITY METER (470..534): full-width pill, ticks ----------------------
my0, my1 = 470, 534
running, waiting = 2, 1
total = running + waiting
px0, px1 = 24, W-24
pw, ph = px1-px0, 26
py = (my0+my1)/2 - ph/2
rw = pw * running/total
d.rounded_rectangle(sc(px0, py, px1, py+ph), radius=13*S, fill=(63,63,70,255))
d.rounded_rectangle(sc(px0, py, px0+rw, py+ph), radius=13*S, fill=(63,185,80,255))
d.rounded_rectangle(sc(px0, py, px0+rw*0.5, py+ph), radius=13*S, fill=(94,222,110,255))
for i in range(1, 10):
    tx = px0 + pw*i/10
    d.line(sc(tx, py+ph-3, tx, py+ph), fill=(13,17,23,200), width=1*S)

# ---- METRIC CARDS (534..900): 2-col glass grid ------------------------------
def card(x, y, w, h, label, value, unit=""):
    d.rounded_rectangle(sc(x, y, x+w, y+h), radius=14*S, fill=(22,27,34,255), outline=(48,54,61,255), width=1*S)
    # accent dot + label
    d.ellipse(sc(x+16, y+18, x+22, y+24), fill=(88,166,255,255))

cards = [
    ("REQUESTS", "1 477"), ("TOKENS OUT", "504 K"),
    ("GPU", "88%"), ("MEM", "75.6 GB"),
    ("CACHE HIT", "58.8%"), ("UPTIME", "4h 52m"),
]
gx0, gy = 24, 552
cw, ch, gap = (W-48-16)/2, 74, 16
for i, (lab, val) in enumerate(cards):
    r, c = divmod(i, 2)
    card(gx0 + c*(cw+gap), gy + r*(ch+gap), cw, ch, lab, val)

img = img.resize((W, H), Image.LANCZOS)
img.save(sys.argv[1] if len(sys.argv) > 1 else "/tmp/dash_preview.png")
print("saved", sys.argv[1] if len(sys.argv) > 1 else "/tmp/dash_preview.png")
