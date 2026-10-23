#!/usr/bin/env python3
"""New chat screen design: full-bleed Claude-style flat messages, no bubbles,
generous spacing, docked composer. Mirrors the Java layout plan for visual review."""
from PIL import Image, ImageDraw, ImageFont

W, H = 1080, 2400
BG = (18, 20, 26)
TEXT_HIGH = (242, 245, 250)
TEXT_MED = (169, 179, 194)
TEXT_LOW = (105, 116, 138)
ACCENT = (126, 166, 245)
SURFACE = (28, 32, 41)
SURFACE_HIGH = (36, 42, 54)

MARGIN = 60
PAD = 48
GAP = 48  # 16dp between message blocks

def font(sz, bold=False):
    # system-ui font approximation (SF Pro / Roboto)
    p = '/System/Library/Fonts/SFNSDisplay.ttf' if not bold else '/System/Library/Fonts/SFNSDisplay-Bold.ttf'
    try: return ImageFont.truetype(p, sz)
    except: return ImageFont.load_default()

def text_width(draw, s, f):
    return draw.textbbox((0,0), s, font=f)[2]

im = Image.new('RGB', (W, H), BG)
d = ImageDraw.Draw(im)

# Header: title + tagline, no bottom border (flat surface)
y = 80
d.text((MARGIN, y), "Neuro Chat", font=font(72), fill=TEXT_HIGH)
d.text((MARGIN, y+90), "on-device · nothing leaves the phone", font=font(36), fill=TEXT_LOW)

# Chat area: full-bleed, 16dp side padding, messages flat with subtle left accent
y = 280
msg_x = MARGIN
# User message (right-aligned block, tinted background)
umsg = "Как жизнь?"
uw = text_width(d, umsg, font(52)) + PAD*2
uh = 120
ux = W - MARGIN - uw
# rounded rect with very subtle accent tint (not bubble)
d.rounded_rectangle([ux, y, ux+uw, y+uh], 32, fill=(30, 45, 80))
d.text((ux+PAD, y+42), umsg, font=font(52), fill=TEXT_HIGH)

y += uh + GAP

# Assistant message: left-aligned, full width minus margin, SURFACE bg
amsg = "Всё в порядке, хотя запутанно."
aw = W - MARGIN*2
ah = 180
d.rounded_rectangle([MARGIN, y, MARGIN+aw, y+ah], 32, fill=SURFACE)
d.text((MARGIN+PAD, y+54), amsg, font=font(52), fill=TEXT_HIGH)

# "thinking" indicator (if mid-stream): three dots or animated text
y2 = y + ah + 40
d.text((MARGIN, y2), "thinking…", font=font(44), fill=ACCENT)

# Composer docked bottom: full-bleed, SURFACE_HIGH, input pill + send pill
bar_h = 200
bar_y = H - bar_h - 60  # nav clearance
# input field: pill, SURFACE_HIGH
input_x = MARGIN
input_w = W - MARGIN*2 - 240  # leave 240px for send button
input_h = 140
input_y = bar_y + 30
d.rounded_rectangle([input_x, input_y, input_x+input_w, input_y+input_h], 70, fill=SURFACE_HIGH)
d.text((input_x+PAD, input_y+50), "Message…", font=font(52), fill=TEXT_LOW)

# Send button: pill, ACCENT filled
send_x = W - MARGIN - 200
d.rounded_rectangle([send_x, input_y, send_x+200, input_y+input_h], 70, fill=ACCENT)
d.text((send_x+65, input_y+48), "Send", font=font(48, True), fill=(11, 18, 32))

# nav hint at very bottom (3 dots)
nav_y = H - 50
for i in range(3):
    d.ellipse([W//2 - 60 + i*40, nav_y, W//2 - 20 + i*40, nav_y+40], fill=TEXT_LOW)

im.save('build/chat_flat_design.png')
print('Flat Claude-style chat design saved: build/chat_flat_design.png')
