#!/usr/bin/env python3
"""Pack keyed AI icons into res/: transparent objects at every density,
plus full-bleed launcher art (no plate edges -> no double rim under the
system circular mask). Run from repo root after raws are in build/."""
import sys
sys.path.insert(0, 'tools')
from keyer import key
from PIL import Image, ImageDraw
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)

# tile icon: keyed raw -> filename(v26 naming) mapping; keep exact existing names
TILES = {
    'ic_feed_raw.png':     't_feed-icon.png',
    'ic_capsule_raw.png':  't_capsule-icon.png',
    'ic_runner_raw.png':   't_runner_icon.png',
    'ic_bars_raw.png':     't_bars-icon.png',
    'ic_pulse_raw.png':    't_pulse-icon.png',
    'ic_update_raw.png':   't_update-icon.png',
    'ic_brain_big_raw.png': 't_brain-icon.png',
}
DENS = {'mdpi': 54, 'hdpi': 81, 'xhdpi': 108, 'xxhdpi': 162, 'xxxhdpi': 216}

def ensure(d, raw, fill):
    p = f'build/{raw.replace("_raw", "_keyed")}'
    if not os.path.exists(p):
        key(f'build/{raw}', p, fill)
    return Image.open(p)

for raw, dst in TILES.items():
    base = ensure(raw, raw, 0.78)
    for dens, px in DENS.items():
        out = Image.new('RGBA', (px, px), (0, 0, 0, 0))
        b = base.resize((px, px), Image.Resampling.LANCZOS)
        out.paste(b, (0, 0), b)
        d = f'res/mipmap-{dens}-v26'
        os.makedirs(d, exist_ok=True)
        out.save(f'{d}/{dst}')
    print('tile', dst)

# --- launcher: smooth full-bleed gradient + brain object, no edges ---
brain = ensure('ic_brain_big_raw.png', 'ic_brain_big_raw.png', 0.60)  # tighter for home-screen mask

def launcher_art(size):
    im = Image.new('RGB', (size, size))
    d = ImageDraw.Draw(im)
    # vertical deep-navy gradient + soft radial glow behind object
    top, bot = (16, 22, 44), (8, 10, 18)
    for y in range(size):
        t = y / size
        d.line([(0, y), (size, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bot)))
    glow = Image.new('L', (size, size), 0)
    gd = ImageDraw.Draw(glow)
    gd.ellipse([size * 0.15, size * 0.15, size * 0.85, size * 0.85], fill=70)
    glow = glow.filter(__import__('PIL.ImageFilter', fromlist=['GaussianBlur']).GaussianBlur(size // 10))
    accent = Image.new('RGB', (size, size), (70, 110, 210))
    im = Image.composite(accent, im, glow)
    px = int(size * 0.52)
    b = brain.resize((px, px), Image.Resampling.LANCZOS)
    im.paste(b, ((size - px) // 2, (size - px) // 2), b)
    return im

LDENS = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}
for dens, size in LDENS.items():
    art = launcher_art(size)
    d = f'res/mipmap-{dens}'
    art.save(f'{d}/ic_launcher_full.png')
    # legacy: rounded-square plate version (old launchers)
    leg = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], int(size * 0.22), fill=255)
    leg.paste(art.convert('RGBA'), (0, 0), mask)
    leg.save(f'{d}/ic_launcher_legacy.png')
    # round: circle mask of the full art
    rnd = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    cmask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(cmask).ellipse([0, 0, size - 1, size - 1], fill=255)
    rnd.paste(art.convert('RGBA'), (0, 0), cmask)
    rnd.save(f'{d}/ic_launcher_round.png')
    print('launcher', dens)
print('PACK_DONE')
