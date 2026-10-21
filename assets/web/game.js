// Neon Runner v2 — single-file canvas runner. Chase camera fake-3D, 3 lanes.
'use strict';
const C = document.getElementById('c'), X = C.getContext('2d');
let W = 0, H = 0, DPR = 1;
function resize() {
  const vw = innerWidth, vh = innerHeight;
  const aspect = 9 / 16;                       // portrait design
  let cw = vw, ch = vw / aspect;
  if (ch > vh) { ch = vh; cw = vh * aspect; }
  DPR = Math.min(devicePixelRatio || 1, 2);
  C.width = cw * DPR; C.height = ch * DPR;
  C.style.width = cw + 'px'; C.style.height = ch + 'px';
  W = cw; H = ch; X.setTransform(DPR, 0, 0, DPR, 0, 0);
}
addEventListener('resize', resize); resize();

// ---------- assets ----------
const A = {}; const LOAD = ['run0','run1','run2','run3','run4','run5','jump','roll','idle'];
let loaded = 0;
LOAD.forEach(n => { const i = new Image(); i.onload = () => loaded++; i.onerror = () => loaded++; i.src = 'assets/' + n + '.png'; A[n] = i; });

// ---------- world constants ----------
const LANE = 1.15, GRAV = 26, JUMP_V = 8.6, SPAWN_Z = 70, DESPAWN_Z = -6;
const CAM = { h: 2.9, back: 7.2, pitch: 0.055, f: 1.02 };
function project(x, y, z) {
  const d = z + CAM.back; if (d < 0.4) return null;
  const yy = y - CAM.h + CAM.pitch * d;
  const s = CAM.f / d * H * 1.35;
  return { x: W / 2 + x * s, y: H * 0.456 - yy * s, s };
}

// ---------- state ----------
const S = { mode: 'menu', t: 0, dist: 0, speed: 26, coins: 0, score: 0,
  best: +(localStorage.getItem('nr_best') || 0), bestC: +(localStorage.getItem('nr_bestc') || 0),
  lane: 1, x: LANE, y: 0, vy: 0, air: false, roll: 0, anim: 0,
  obs: [], coinsArr: [], parts: [], stars: [], dead: 0, shake: 0, flash: 0 };
for (let i = 0; i < 60; i++) S.stars.push({ x: Math.random(), y: Math.random() * 0.45, r: Math.random() * 1.4 + 0.4, tw: Math.random() * 6 });

function reset() { Object.assign(S, { mode: 'run', t: 0, dist: 0, speed: 26, coins: 0, score: 0,
  lane: 1, x: LANE, y: 0, vy: 0, air: false, roll: 0, anim: 0, obs: [], coinsArr: [], parts: [], dead: 0, shake: 0, flash: 0 });
  nextSpawn = 30; }
let nextSpawn = 30;

// ---------- spawning ----------
function spawn() {
  const diff = Math.min(1, S.dist / 3000);                    // 0..1 over ~3k m
  const roll = Math.random();
  if (roll < 0.34) { // low block: jump
    S.obs.push({ type: 'low', lane: (Math.random() * 3) | 0, z: SPAWN_Z });
  } else if (roll < 0.62) { // overhead bar: roll under
    S.obs.push({ type: 'bar', lane: (Math.random() * 3) | 0, z: SPAWN_Z });
  } else if (roll < 0.84) { // wall: must switch lane, 1-2 lanes blocked
    const blocked = new Set(); const n = diff > 0.4 && Math.random() < 0.5 ? 2 : 1;
    while (blocked.size < n) blocked.add((Math.random() * 3) | 0);
    blocked.forEach(l => S.obs.push({ type: 'wall', lane: l, z: SPAWN_Z }));
  } else { // coin line in a free lane
    const l = (Math.random() * 3) | 0, aerial = Math.random() < 0.3;
    for (let i = 0; i < 6; i++) S.coinsArr.push({ lane: l, z: SPAWN_Z + i * 2.2, y: aerial ? 1.5 + Math.sin(i * 0.9) * 0.4 : 0.9, taken: false });
  }
  nextSpawn = S.dist + 16 + Math.random() * 14 - diff * 8;     // gap tightens with difficulty
}

// ---------- collision ----------
const hit = (o) => Math.abs(o.z) < 0.95 && o.lane === S.lane && Math.abs(S.x - (o.lane - 1) * LANE) < LANE * 0.6;
function playerBox() { const h = S.roll > 0 ? 0.8 : 1.62; return { feet: S.y, top: S.y + h }; }
function collide() {
  const pb = playerBox();
  for (const o of S.obs) {
    if (!hit(o)) continue;
    if (o.type === 'low' && pb.feet < 0.8) return die();       // feet must clear block top
    if (o.type === 'bar' && pb.top > 1.05) return die();       // head must stay under bar (roll)
    if (o.type === 'wall') return die();
  }
  for (const cn of S.coinsArr) {
    if (!cn.taken && Math.abs(cn.z) < 0.9 && cn.lane === S.lane && Math.abs(S.y + 0.6 - cn.y) < 0.9) {
      cn.taken = true; S.coins++; burst(project((cn.lane - 1) * LANE, cn.y, cn.z), '#F0C34E', 8);
    }
  }
}
function die() {
  S.mode = 'over'; S.dead = 0.001; S.shake = 1; S.flash = 1;
  S.best = Math.max(S.best, Math.floor(S.score)); localStorage.setItem('nr_best', S.best);
  S.bestC = Math.max(S.bestC, S.coins); localStorage.setItem('nr_bestc', S.bestC);
  burst(project(S.x, S.y + 0.8, 0), '#38E0D8', 26);
}
function burst(p, col, n) { if (!p) return;
  for (let i = 0; i < n; i++) S.parts.push({ x: p.x, y: p.y, vx: (Math.random() - 0.5) * 240, vy: (Math.random() - 0.8) * 240, life: 0.7, col }); }

// ---------- input ----------
function move(d) { const l = Math.max(0, Math.min(2, S.lane + d)); if (l !== S.lane) { S.lane = l; } }
function jump() { if (!S.air && S.roll <= 0) { S.vy = JUMP_V; S.air = true; } }
function doRoll() { S.roll = 0.5; if (S.air) S.vy = -14; }           // air-dive slams you down
function act(a) { if (S.mode === 'menu') { reset(); return; } if (S.mode === 'over') { if (S.dead > 0.8) reset(); return; }
  if (a === 'L') move(-1); else if (a === 'R') move(1); else if (a === 'U') jump(); else if (a === 'D') doRoll(); }
let tsx = 0, tsy = 0;
C.addEventListener('touchstart', e => { tsx = e.touches[0].clientX; tsy = e.touches[0].clientY; }, { passive: true });
C.addEventListener('touchend', e => {
  const dx = e.changedTouches[0].clientX - tsx, dy = e.changedTouches[0].clientY - tsy;
  if (Math.abs(dx) < 24 && Math.abs(dy) < 24) return act(S.mode === 'run' ? 'U' : 'T');
  if (Math.abs(dx) > Math.abs(dy)) act(dx > 0 ? 'R' : 'L'); else act(dy > 0 ? 'D' : 'U');
}, { passive: true });
addEventListener('keydown', e => act({ ArrowLeft: 'L', ArrowRight: 'R', ArrowUp: 'U', ArrowDown: 'D', ' ': 'U' }[e.key]));

// ---------- update ----------
function update(dt) {
  S.t += dt;
  for (const p of S.parts) { p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 500 * dt; p.life -= dt; }
  S.parts = S.parts.filter(p => p.life > 0);
  S.shake = Math.max(0, S.shake - dt * 2.5); S.flash = Math.max(0, S.flash - dt * 3);
  if (S.mode !== 'run') { if (S.dead) S.dead += dt; return; }
  S.speed = Math.min(52, 26 + S.dist / 220);
  const dz = S.speed * dt; S.dist += dz; S.score += dz * 1 + S.coins * 0;
  S.x += ((S.lane - 1) * LANE - S.x) * Math.min(1, dt * 14);
  S.vy -= GRAV * dt; S.y += S.vy * dt;
  if (S.y <= 0) { if (S.air) burst(project(S.x, 0.1, 0), '#7EA6F5', 5); S.y = 0; S.vy = 0; S.air = false; }
  if (S.roll > 0) S.roll -= dt;
  S.anim += dt * (S.air || S.roll > 0 ? 4 : S.speed * 0.32);
  for (const o of S.obs) o.z -= dz; for (const c of S.coinsArr) c.z -= dz;
  S.obs = S.obs.filter(o => o.z > DESPAWN_Z); S.coinsArr = S.coinsArr.filter(c => c.z > DESPAWN_Z && !c.taken);
  if (S.dist >= nextSpawn) spawn();
  collide();
}

// ---------- render ----------
function poly(pts, col) { X.fillStyle = col; X.beginPath(); pts.forEach((p, i) => i ? X.lineTo(p.x, p.y) : X.moveTo(p.x, p.y)); X.closePath(); X.fill(); }
function quad(x1, z1, x2, z2, col, y = 0) { const a = project(x1, y, z1), b = project(x2, y, z1), c = project(x2, y, z2), d = project(x1, y, z2); if (a && b && c && d) poly([a, b, c, d], col); }
function shade(hex, k) { const n = parseInt(hex.slice(1), 16);
  const r = Math.min(255, (n >> 16) * k | 0), g = Math.min(255, ((n >> 8) & 255) * k | 0), b = Math.min(255, (n & 255) * k | 0);
  return `rgb(${r},${g},${b})`; }
function box(lx, rx, y0, y1, z, col) {   // solid: front face + top + one side
  const hw = 0.55, x0 = lx - hw, x1 = rx + hw;
  const f = [project(x0, y0, z - 0.4), project(x1, y0, z - 0.4), project(x1, y1, z - 0.4), project(x0, y1, z - 0.4)];
  if (f.every(Boolean)) poly(f, col);
  const t = [project(x0, y1, z - 0.4), project(x1, y1, z - 0.4), project(x1, y1, z + 0.4), project(x0, y1, z + 0.4)];
  if (t.every(Boolean)) poly(t, shade(col, 1.35));
  const sx = S.x < lx ? x1 : x0;
  const s = [project(sx, y0, z - 0.4), project(sx, y0, z + 0.4), project(sx, y1, z + 0.4), project(sx, y1, z - 0.4)];
  if (s.every(Boolean)) poly(s, shade(col, 0.62));
}
function drawWorld() {
  const g = X.createLinearGradient(0, 0, 0, H); g.addColorStop(0, '#0B0E14'); g.addColorStop(0.55, '#101827'); g.addColorStop(1, '#0B0E14');
  X.fillStyle = g; X.fillRect(0, 0, W, H);
  for (const st of S.stars) { X.globalAlpha = 0.35 + 0.55 * Math.abs(Math.sin(S.t * 1.4 + st.tw));
    X.fillStyle = '#cfe0ff'; X.fillRect(st.x * W, st.y * H, st.r, st.r); } X.globalAlpha = 1;
  // synthwave sun + skyline on horizon
  const hy = H * 0.456, sun = X.createLinearGradient(0, hy - 90, 0, hy);
  sun.addColorStop(0, '#F0506E'); sun.addColorStop(1, '#F0C34E');
  X.save(); X.beginPath(); X.rect(0, hy - 90, W, 90); X.clip();
  X.fillStyle = sun; X.beginPath(); X.arc(W * 0.5 - S.x * 6, hy, 64, Math.PI, 0); X.fill();
  X.fillStyle = '#0B0E14'; for (let i = 0; i < 5; i++) X.fillRect(0, hy - 58 + i * 13, W, 3.5);
  X.restore();
  X.fillStyle = '#141B2C'; let bx = (-S.x * 6 - S.dist * 0.35) % 46;
  for (let i = -1; i < W / 46 + 1; i++) { const bh = 22 + ((i * 7919) % 5) * 11; X.fillRect(bx + i * 46, hy - bh, 34, bh); }
  quad(-10, 0, 10, SPAWN_Z, '#131A2A', -0.02);                             // ground haze
  quad(-LANE * 1.6, 0, LANE * 1.6, SPAWN_Z, '#1A2233');                     // road
  for (let z = -(S.dist % 4); z < 62; z += 4)                                // center dashes
    quad(-0.09, z + 1.4, 0.09, z + 2.6, 'rgba(126,166,245,0.5)');
  for (let i = 0; i < 2; i++) { const ex = (i ? 1 : -1) * LANE * 1.62;      // neon edges
    const p0 = project(ex, 0, 0), p1 = project(ex, 0, SPAWN_Z);
    if (p0 && p1) { X.strokeStyle = '#38E0D8'; X.lineWidth = 2.5; X.shadowColor = '#38E0D8'; X.shadowBlur = 12;
      X.beginPath(); X.moveTo(p0.x, p0.y); X.lineTo(p1.x, p1.y); X.stroke(); X.shadowBlur = 0; } }
  for (let z = -(S.dist % 8); z < 58; z += 8) {                          // pylons
    for (const s of [-1, 1]) { const p = project(s * LANE * 1.75, 0, z), t = project(s * LANE * 1.75, 1.1, z);
      if (p && t) { X.strokeStyle = 'rgba(126,166,245,0.7)'; X.lineWidth = Math.max(1, 0.09 * p.s);
        X.beginPath(); X.moveTo(p.x, p.y); X.lineTo(t.x, t.y); X.stroke();
        X.fillStyle = '#7EA6F5'; X.beginPath(); X.arc(t.x, t.y, Math.max(1.2, 0.1 * t.s), 0, 7); X.fill(); } } }
}
function drawEntities() {
  const items = [];
  for (const o of S.obs) items.push({ z: o.z, f: () => drawObs(o) });
  for (const c of S.coinsArr) items.push({ z: c.z, f: () => drawCoin(c) });
  items.push({ z: 0, f: drawPlayer });
  items.sort((a, b) => b.z - a.z).forEach(i => i.f());
}
function drawObs(o) {
  const lx = (o.lane - 1) * LANE;
  if (o.type === 'low') { box(lx, lx, 0, 0.85, o.z, '#A78BDB');
    const p = project(lx, 0.8, o.z - 0.4); if (p) { X.fillStyle = '#cdbcf5'; X.fillRect(p.x - 0.55 * p.s, p.y - 0.1 * p.s, 1.1 * p.s, 0.1 * p.s); } }
  else if (o.type === 'bar') { box(lx, lx, 1.05, 1.5, o.z, '#7F8CF0'); box(lx, lx, 1.6, 2.5, o.z + 0.1, '#404a80'); const t = project(lx, 1.5, o.z - 0.4); if (t) { X.fillStyle = '#F0C34E'; X.fillRect(t.x - 0.55 * t.s, t.y - 0.05 * t.s, 1.1 * t.s, 0.1 * t.s); }
    const p = project(lx, 1.05, o.z); if (p) { X.fillStyle = '#F0C34E'; X.font = `bold ${Math.max(8, p.s * 0.3)}px sans-serif`; X.textAlign = 'center'; X.fillText('▼', p.x, p.y - 4); } }
  else { box(lx, lx, 0, 2.6, o.z, '#F0506E'); const t = project(lx, 2.6, o.z - 0.4); if (t) { X.fillStyle = '#ff97a8'; X.fillRect(t.x - 0.55 * t.s, t.y, 1.1 * t.s, 0.08 * t.s); } }
}
function drawCoin(c) { const p = project((c.lane - 1) * LANE, c.y + Math.sin(S.t * 5 + c.z) * 0.08, c.z); if (!p) return;
  const r = 0.3 * p.s; X.save(); X.translate(p.x, p.y); X.scale(Math.abs(Math.sin(S.t * 4 + c.z)) * 0.45 + 0.55, 1);
  X.fillStyle = '#F0C34E'; X.shadowColor = '#F0C34E'; X.shadowBlur = 10; X.beginPath(); X.arc(0, 0, r, 0, 7); X.fill(); X.restore(); }
function drawPlayer() {
  const p = project(S.x, S.y, 0); if (!p) return;
  const img = S.roll > 0 ? A.roll : S.air ? A.jump : A['run' + (Math.floor(S.anim) % 6)];
  if (!(img && img.complete && img.naturalWidth)) { X.fillStyle = '#38E0D8'; X.beginPath(); X.arc(p.x, p.y - 0.8 * p.s, 0.4 * p.s, 0, 7); X.fill(); return; }
  const hh = (S.roll > 0 ? 1.05 : 1.62) * p.s, ww = hh * img.width / img.height;
  const gy = project(S.x, 0, 0);
  if (gy) { X.fillStyle = 'rgba(0,0,0,0.45)'; X.beginPath(); const k = Math.max(0.35, 1 - S.y * 0.3); X.ellipse(gy.x, gy.y, 0.5 * gy.s * k, 0.14 * gy.s * k, 0, 0, 7); X.globalAlpha = k; X.fill(); X.globalAlpha = 1; X.fill(); }
  X.save(); X.translate(p.x, p.y - (S.roll > 0 ? hh * 0.42 : hh * 0.02));
  X.shadowColor = 'rgba(56,224,216,0.8)'; X.shadowBlur = 18;
  X.drawImage(img, -ww / 2, -hh * 0.98, ww, hh); X.restore();
}
function drawParts() { for (const p of S.parts) { X.globalAlpha = Math.max(0, p.life / 0.7); X.fillStyle = p.col;
  X.fillRect(p.x - 2, p.y - 2, 4, 4); } X.globalAlpha = 1; }
function fmt(n) { return ('' + n).padStart(6, '0'); }
function drawHUD() {
  X.fillStyle = 'rgba(10,13,20,0.6)'; X.fillRect(0, 0, W, 34);
  X.font = 'bold 13px monospace'; X.textAlign = 'left'; X.fillStyle = '#38E0D8'; X.fillText('SCORE ' + fmt(Math.floor(S.score)), 12, 22);
  X.textAlign = 'center'; X.fillStyle = '#F0C34E'; X.fillText('● ' + S.coins, W / 2, 22);
  X.textAlign = 'right'; X.fillStyle = '#7EA6F5'; X.fillText('BEST ' + fmt(S.best), W - 12, 22);
  X.fillStyle = 'rgba(126,166,245,0.6)'; X.textAlign = 'right'; X.font = '10px monospace';
  X.fillText(Math.floor(S.dist) + 'm  ' + Math.floor(S.speed) + 'km/h', W - 12, 48);
}
function center(title, col, sub) {
  X.fillStyle = 'rgba(11,14,20,0.72)'; X.fillRect(0, H * 0.28, W, H * 0.40);
  X.textAlign = 'center'; X.fillStyle = col; X.font = 'bold 30px sans-serif'; X.fillText(title, W / 2, H * 0.42);
  X.fillStyle = '#cfd8ea'; X.font = '14px sans-serif'; sub.forEach((s, i) => X.fillText(s, W / 2, H * 0.50 + i * 22));
}
function render() {
  X.save(); if (S.shake > 0) X.translate((Math.random() - 0.5) * 10 * S.shake, (Math.random() - 0.5) * 10 * S.shake);
  drawWorld(); if (S.mode !== 'menu') drawEntities(); drawParts(); X.restore();
  if (S.flash > 0) { X.fillStyle = `rgba(240,80,110,${S.flash * 0.35})`; X.fillRect(0, 0, W, H); }
  if (S.mode === 'menu') center('NEON RUNNER', '#38E0D8', ['свайп: ↑ прыжок  ↓ подкат  ← → смена полосы', 'BEST ' + fmt(S.best) + '   ● ' + S.bestC, '— тап, чтобы бежать —']);
  if (S.mode === 'over') center('ТЫ УМЕР', '#F0506E', ['SCORE ' + fmt(Math.floor(S.score)) + '   ● ' + S.coins + '   ' + Math.floor(S.dist) + 'm', 'BEST ' + fmt(S.best), S.dead > 0.8 ? '— тап, чтобы снова —' : '']);
  if (S.mode === 'run') drawHUD();
  if (loaded < LOAD.length) { X.fillStyle = '#38E0D8'; X.textAlign = 'center'; X.font = '12px monospace'; X.fillText('assets ' + loaded + '/' + LOAD.length, W / 2, H - 20); }
}

// ---------- loop ----------
let last = performance.now();
function loop(now) { const dt = Math.min(0.05, (now - last) / 1000); last = now; update(dt); render(); requestAnimationFrame(loop); }
requestAnimationFrame(loop);
