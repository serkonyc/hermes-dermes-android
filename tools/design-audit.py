#!/usr/bin/env python3
"""Full-app redesign plan: one pass over every screen. Modern AI-app patterns
(MD3 + Claude/Perplexity): flat message surfaces, role labels, generous spacing,
docked composer, visual anchors. Run me to audit current padding/spacing violations."""
import os, re, glob

ACT = sorted(glob.glob('/Users/serkonyc/.hermes/work/dermes-android/src/ai/serge/dermes/*Activity.java'))

issues = []
for f in ACT:
    src = open(f).read()
    name = os.path.basename(f)
    
    # magic numbers instead of tokens
    magic_pad = re.findall(r'setPadding\s*\(\s*(dp\(\d+\)|\d+)', src)
    if magic_pad: issues.append(f'{name}: {len(magic_pad)} magic paddings')
    
    # hardcoded hex
    hexes = re.findall(r'0x[0-9A-Fa-f]{8}', src)
    hexes = [h for h in hexes if h not in ['0xFFFFFFFF','0xFF000000']]  # allow pure white/black
    if len(hexes) > 0: issues.append(f'{name}: {len(hexes)} hardcoded colors')
    
    # text size literals
    sizes = re.findall(r'setTextSize\s*\(\s*([\d.]+)f', src)
    if len(sizes) > 3: issues.append(f'{name}: {len(sizes)} raw text sizes (should use type scale)')
    
    # bubble vs flat check (chat-specific)
    if 'Chat' in name and 'bubble' in src.lower():
        issues.append(f'{name}: still uses bubble metaphor (move to flat)')

print(f'Scanned {len(ACT)} Activities')
if issues:
    print('Found issues:')
    for i in issues: print(' -', i)
else:
    print('✓ Clean (all screens use tokens, no magic paddings)')
