// Neon Runner -> Subway-style runner, day palette, no bloom. three.js r147 UMD.
'use strict';
const SKY=0x4FB8F5, HZ=0xBFE6FA, GR=0x8A7A66, GO=0xFFD24A, OR=0xFF8C42;
const LANE=1.15, GRAV=26, JUMP_V=8.6, SPAWN_Z=-70, DESPAWN_Z=6, TAU=Math.PI*2;
const $=id=>document.getElementById(id);
let renderer,scene,camera;
try{
  renderer=new THREE.WebGLRenderer({canvas:$('c'),antialias:false,powerPreference:'high-performance'});
}catch(e){ document.body.innerHTML='<p style="color:#333;padding:40px;font:14px monospace">WebGL недоступен: '+e.message+'</p>'; throw e; }
const DPR=Math.min(devicePixelRatio||1,location.search.includes('slow')?.75:1.75); // 'slow': headless test mode
renderer.setPixelRatio(DPR); renderer.setSize(innerWidth,innerHeight);
renderer.outputEncoding=THREE.sRGBEncoding; renderer.setClearColor(SKY);
scene=new THREE.Scene(); scene.fog=new THREE.Fog(HZ,45,120); scene.fog.color.convertSRGBToLinear();
camera=new THREE.PerspectiveCamera(70,innerWidth/innerHeight,0.1,320);
scene.add(new THREE.HemisphereLight(0xBFE6FA,0x8a7a66,0.9));
const sunL=new THREE.DirectionalLight(0xFFF3D6,1.15); sunL.position.set(-14,22,10); scene.add(sunL);
function M(c,r,m){return new THREE.MeshStandardMaterial({color:new THREE.Color(c).convertSRGBToLinear(),roughness:r==null?.8:r,metalness:m||0});}
const box=(w,h,d,m)=>new THREE.Mesh(new THREE.BoxGeometry(w,h,d),m);
// ---------- sky + clouds + distant sun ----------
{const cv=document.createElement('canvas');cv.width=8;cv.height=64;const g=cv.getContext('2d');
 const gr=g.createLinearGradient(0,0,0,64);gr.addColorStop(0,'#3FA9F0');gr.addColorStop(.72,'#BFE6FA');gr.addColorStop(1,'#E7F6FF');
 g.fillStyle=gr;g.fillRect(0,0,8,64);
 const t=new THREE.CanvasTexture(cv);t.encoding=THREE.sRGBEncoding;
 const sk=new THREE.Mesh(new THREE.SphereGeometry(290,8,12),new THREE.MeshBasicMaterial({map:t,side:THREE.BackSide,fog:false,depthWrite:false}));
 scene.add(sk);}
const cloudM=M(0xffffff,.95);cloudM.fog=false;
const clouds=new THREE.InstancedMesh(new THREE.BoxGeometry(1,.5,1),cloudM,10), cloudPos=[];
for(let i=0;i<10;i++)cloudPos.push({x:(Math.random()*2-1)*120,y:22+Math.random()*20,z:-60-Math.random()*180,s:8+Math.random()*14});
scene.add(clouds);
{const sun=new THREE.Mesh(new THREE.SphereGeometry(9,12,12),new THREE.MeshBasicMaterial({color:0xFFF6C9,fog:false}));
 sun.position.set(-70,60,-220);scene.add(sun);}
// ---------- ground + rails ----------
const ground=new THREE.Mesh(new THREE.PlaneGeometry(300,320),M(GR)); ground.rotation.x=-Math.PI/2; ground.position.set(0,-.02,-110); scene.add(ground);
const ties=new THREE.InstancedMesh(new THREE.BoxGeometry(LANE*3+1,.1,.5),M(0x6B4A2F),30); scene.add(ties);
const railM=M(0xB8C0C8,.4,.25);
for(let l=0;l<3;l++){for(const s of[-.38,.38]){const r=box(.09,.12,320,railM);
 r.position.set((l-1)*LANE+s,.11,-110);scene.add(r);}}
// ---------- city ----------
const PALETTE=[0xF25F5C,0xFFE066,0x6BCB77,0x4D96FF,0xFF9F45,0xF5EFE6,0x9B5DE5,0x00BBF9];
const winCv=document.createElement('canvas');winCv.width=winCv.height=64;{const g=winCv.getContext('2d');g.fillStyle='#fff';g.fillRect(0,0,64,64);g.fillStyle='rgba(30,40,60,.35)';for(let r=0;r<8;r++)for(let c=0;c<5;c++)g.fillRect(3+c*12,3+r*8,8,5);}
const winTex=new THREE.CanvasTexture(winCv);winTex.encoding=THREE.sRGBEncoding;
const cityM2=M(0xffffff,.9);cityM2.map=winTex;
const NCity=64, city=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),cityM2,NCity), cityPos=[];
const cCol=new THREE.Color();
for(let i=0;i<NCity;i++){const s=Math.random()<.5?-1:1;
 cityPos.push({x:s*(11+Math.random()*24),z:Math.random()*300,h:5+Math.random()*15,w:3+Math.random()*5});
 cCol.setHex(PALETTE[i%PALETTE.length]).convertSRGBToLinear();city.setColorAt(i,cCol);}
scene.add(city); // instanceColor works WITHOUT vertexColors (missing attr -> black)
// sidewalk curbs
for(const s of[-1,1]){const c=box(.5,.3,320,M(0xCFC4AE));c.position.set(s*(LANE*1.5+.6),.15,-110);scene.add(c);}
// ---------- player: running plunger (recognizable tool, not human) ----------
const plungerHandle=M(0xB47734,.92), plungerKnob=M(0x8D5A28,.9), plungerCup=M(0x25252A,.72), plungerLip=M(0x111114,.85), plungerWood=M(0xD6A86A,.85);
const player=new THREE.Group(), rig=new THREE.Group(); player.add(rig); scene.add(player);
function limb(w,h,m,pivotY){const p=new THREE.Group();const b=box(w,h,w,m);b.position.y=-h/2;p.add(b);p.position.y=pivotY;return p;}
var armL,armR,legL,legR, plungerBody;
{plungerBody=new THREE.Group(); plungerBody.position.y=.52; rig.add(plungerBody);
 const cup=new THREE.Mesh(new THREE.SphereGeometry(.55,24,12,0,TAU,0,Math.PI*.58),plungerCup); cup.rotation.x=Math.PI; cup.position.y=.58; cup.scale.set(1,.68,1); plungerBody.add(cup);
 const cupBottom=new THREE.Mesh(new THREE.CylinderGeometry(.55,.55,.14,26),M(0xC0392B,.8)); cupBottom.position.y=.51; plungerBody.add(cupBottom);
 const eyeL=new THREE.Mesh(new THREE.SphereGeometry(.07,10,8),new THREE.MeshBasicMaterial({color:0xffffff})); eyeL.position.set(-.12,.36,.36); plungerBody.add(eyeL);
 const eyeR=eyeL.clone(); eyeR.position.x=.12; plungerBody.add(eyeR);
 const smile=new THREE.Mesh(new THREE.TorusGeometry(.1,.02,8,12,Math.PI),new THREE.MeshBasicMaterial({color:0xffffff})); smile.position.set(0,.28,.4); smile.rotation.z=Math.PI; plungerBody.add(smile);
 const cupInner=new THREE.Mesh(new THREE.CylinderGeometry(.27,.18,.12,22),plungerWood); cupInner.position.y=.5; plungerBody.add(cupInner);
 const stick=new THREE.Mesh(new THREE.CylinderGeometry(.08,.08,1.15,16),plungerHandle); stick.position.y=1.16; plungerBody.add(stick);
 const knob=new THREE.Mesh(new THREE.SphereGeometry(.15,16,12),plungerKnob); knob.position.y=1.76; plungerBody.add(knob);
 const band=box(.24,.06,.08,plungerLip); band.position.set(0,1.2,.03); plungerBody.add(band);
 armL=limb(.09,.28,plungerHandle,.72); armR=limb(.09,.28,plungerHandle,.72); armL.position.set(-.21,.42,.04); armR.position.set(.21,.42,.04);
 legL=limb(.13,.32,plungerHandle,.26); legR=limb(.13,.32,plungerHandle,.26); legL.position.set(-.13,.1,.04); legR.position.set(.13,.1,.04);
 rig.add(armL,armR,legL,legR);}
const shCv=document.createElement('canvas');shCv.width=shCv.height=64;{const g=shCv.getContext('2d');
 const rg=g.createRadialGradient(32,32,4,32,32,30);rg.addColorStop(0,'rgba(30,22,12,.55)');rg.addColorStop(1,'rgba(30,22,12,0)');
 g.fillStyle=rg;g.fillRect(0,0,64,64);}
 const shadow=new THREE.Mesh(new THREE.PlaneGeometry(1.2,1.2),
  new THREE.MeshBasicMaterial({map:new THREE.CanvasTexture(shCv),transparent:true,depthWrite:false}));
 shadow.rotation.x=-Math.PI/2;shadow.position.y=.04;shadow.renderOrder=1;scene.add(shadow);
// ---------- templates: subway props ----------
function tLow(){ // orange barrier with white top
 const g=new THREE.Group();const b=box(1.02,.8,.6,M(0xFF8C42,.8));b.position.y=.4;
 const s=box(1.06,.12,.66,M(0xF5F5F5,.7));s.position.y=.84;
 const f1=box(.1,.7,.66,M(0x6B6B6B,.7)),f2=f1.clone();f1.position.set(-.46,.35,0);f2.position.set(.46,.35,0);
 g.add(b,s,f1,f2);return g;}
function tBar(){ // low gantry: gray beam + yellow hazard plate
 const g=new THREE.Group();const b=box(1.12,.5,.5,M(0x9AA5B1,.6,.15));b.position.y=1.32;
 const s=box(1.06,.2,.54,M(0xF4B400,.7));s.position.y=1.06;
 const l1=box(.09,1.06,.09,M(0x77808A,.6,.3)),l2=l1.clone();l1.position.set(-.53,.53,0);l2.position.set(.53,.53,0);
 g.add(b,s,l1,l2);return g;}
function tWall(){ // train car: colored body, windows, roof, dark nose
 const g=new THREE.Group();const c=PALETTE[(Math.random()*PALETTE.length)|0];
 const b=box(1.06,2.35,4.2,M(c,.75));b.position.y=1.42;
 const w1=box(1.1,.45,.9,M(0x1d2b3a,.3,.4)),w2=w1.clone();w1.position.set(0,1.9,-1);w2.position.set(0,1.9,1);
 const st=box(1.1,.16,4.25,M(0xF5EFE6,.7));st.position.y=1.35;
 const rf=box(.94,.12,4.2,M(0x555F68,.8));rf.position.y=2.62;
 const nose=box(1.0,.5,.25,M(0x2b2b2b,.6));nose.position.set(0,.5,-2.12);
 g.add(b,w1,w2,st,rf,nose);return g;}
function tCoin(){const g=new THREE.Group();
 const base=new THREE.Mesh(new THREE.SphereGeometry(.3,16,10),M(0x5A3A1F,.95)); base.scale.set(1,.42,1); base.position.y=0;
 const mid=new THREE.Mesh(new THREE.SphereGeometry(.22,14,10),M(0x6F4A25,.95)); mid.scale.set(1,.5,1); mid.position.y=.12;
 const top=new THREE.Mesh(new THREE.SphereGeometry(.13,12,8),M(0x7A5230,.95)); top.scale.set(1,.6,1); top.position.y=.24;
 const tip=new THREE.Mesh(new THREE.SphereGeometry(.06,10,6),M(0x7A5230,.95)); tip.position.set(.03,.32,.02); tip.rotation.z=-.4;
 const shine=new THREE.Mesh(new THREE.SphereGeometry(.045,8,6),new THREE.MeshBasicMaterial({color:0xFFF0C0})); shine.position.set(.1,.26,.12);
 g.add(base,mid,top,tip,shine); g.position.y=.02; return g;}
const TPL={low:tLow,bar:tBar,wall:tWall};
// ---------- state ----------
const S={mode:'menu',t:0,dist:0,speed:26,coins:0,score:0,
 best:+(localStorage.getItem('nr_best')||0),bestC:+(localStorage.getItem('nr_bestc')||0),
 lane:1,x:0,y:0,vy:0,air:false,roll:0,anim:0,dead:0,shake:0,flash:0,ts:1};
let nextSpawn=30; S.obs=[];S.coinsArr=[];
function reset(){S.mode='run';S.t=0;S.dist=0;S.speed=26;S.coins=0;S.score=0;S.lane=1;S.x=0;S.y=0;S.vy=0;
 S.air=false;S.roll=0;S.anim=0;S.dead=0;S.shake=0;S.flash=0;S.ts=1;
 for(const o of S.obs)scene.remove(o.m);for(const c of S.coinsArr)scene.remove(c.m);
 S.obs=[];S.coinsArr=[];nextSpawn=30;S.missed=0;}
function addObs(type,lane){const m=TPL[type]();m.position.set((lane-1)*LANE,0,SPAWN_Z);scene.add(m);S.obs.push({type,lane,z:SPAWN_Z,m});}
function spawn(){
 const diff=Math.min(1,S.dist/3000), r=Math.random();
 if(r<.3)addObs('low',(Math.random()*3)|0);
 else if(r<.54)addObs('bar',(Math.random()*3)|0);
 else if(r<.84){const bl=new Set();const n=diff>.4&&Math.random()<.5?2:1;   // trains more often
   while(bl.size<n)bl.add((Math.random()*3)|0);bl.forEach(l=>addObs('wall',l));}
 else{const l=(Math.random()*3)|0,aerial=Math.random()<.3;
  for(let i=0;i<6;i++){const m=tCoin();const y=aerial?1.5+Math.sin(i*.9)*.4:.95;
   m.position.set((l-1)*LANE,y,SPAWN_Z-i*2.2);scene.add(m);S.coinsArr.push({lane:l,z:SPAWN_Z-i*2.2,y,m,taken:false});}}
 nextSpawn=S.dist+16+Math.random()*14-diff*8;}
// ---------- particles ----------
const NP=260,ppos=new Float32Array(NP*3),pcol=new Float32Array(NP*3),pvel=new Float32Array(NP*3),plife=new Float32Array(NP);
let pPts;
{const g=new THREE.BufferGeometry();g.setAttribute('position',new THREE.BufferAttribute(ppos,3));
 g.setAttribute('color',new THREE.BufferAttribute(pcol,3));
 pPts=new THREE.Points(g,new THREE.PointsMaterial({size:.14,vertexColors:true,transparent:true,fog:false,depthWrite:false}));
 scene.add(pPts);}
let pCur=0;
function burst(x,y,z,col,n){const c=new THREE.Color(col).convertSRGBToLinear();
 for(let i=0;i<n;i++){const k=pCur++%NP;ppos[k*3]=x;ppos[k*3+1]=y;ppos[k*3+2]=z;
  pvel[k*3]=(Math.random()-.5)*6;pvel[k*3+1]=Math.random()*5+1;pvel[k*3+2]=(Math.random()-.5)*6;
  pcol[k*3]=c.r;pcol[k*3+1]=c.g;pcol[k*3+2]=c.b;plife[k]=.7;}
 pPts.geometry.attributes.position.needsUpdate=pPts.geometry.attributes.color.needsUpdate=true;}
// ---------- collision ----------
const laneHit=o=>o.lane===S.lane&&Math.abs(S.x-(o.lane-1)*LANE)<LANE*.6;
function collide(){const h=S.roll>0?.8:1.62,feet=S.y,top=S.y+h;
 for(const o of S.obs){if(!laneHit(o))continue;
  const near=o.type==='wall'?(o.z+2.1>-0.4&&o.z-2.1<0.4):Math.abs(o.z)<0.95;   // train is 4.2 long
  if(!near)continue;
  if(o.type==='low'&&feet<.8)return die();
  if(o.type==='bar'&&top>1.05)return die();
  if(o.type==='wall')return die();}
 for(const c of S.coinsArr){if(!c.taken&&!c.missed&&Math.abs(c.z)<.9&&c.lane===S.lane&&Math.abs(S.y+.6-c.y)<.9){
  c.taken=true;scene.remove(c.m);S.coins++;burst((c.lane-1)*LANE,c.y,c.z,0x8B5E34,10);}}}
function die(){S.mode='over';S.dead=.001;S.shake=1;S.flash=.4;S.ts=.32;
 S.best=Math.max(S.best,Math.floor(S.score));localStorage.setItem('nr_best',S.best);
 S.bestC=Math.max(S.bestC,S.coins);localStorage.setItem('nr_bestc',S.bestC);
 burst(S.x,S.y+.8,0,0xF5F5F5,30);showOver();}
// ---------- input ----------
function move(d){S.lane=Math.max(0,Math.min(2,S.lane+d));}
function jump(){if(!S.air&&S.roll<=0){S.vy=JUMP_V;S.air=true;}}
function doRoll(){S.roll=.45;if(S.air)S.vy=Math.min(S.vy,-2);}
function act(a){if(S.mode==='menu'){reset();showRun();return;}
 if(S.mode==='over'){if(S.dead>.8)reset(),showRun();return;}
 if(a==='L')move(-1);else if(a==='R')move(1);else if(a==='U')jump();else if(a==='D')doRoll();}
let tsx=0,tsy=0;
addEventListener('touchstart',e=>{tsx=e.touches[0].clientX;tsy=e.touches[0].clientY;},{passive:true});
addEventListener('touchend',e=>{const dx=e.changedTouches[0].clientX-tsx,dy=e.changedTouches[0].clientY-tsy;
 if(Math.abs(dx)<24&&Math.abs(dy)<24)return act(S.mode==='run'?'U':'T');
 if(Math.abs(dx)>Math.abs(dy))act(dx>0?'R':'L');else act(dy>0?'D':'U');},{passive:true});
addEventListener('keydown',e=>act({ArrowLeft:'L',ArrowRight:'R',ArrowUp:'U',ArrowDown:'D',' ':'U'}[e.key]));
// ---------- HUD ----------
const fmt=n=>(''+n).padStart(6,'0');
$('mbest').textContent='РЕКОРД '+fmt(S.best)+' · 💩 '+S.bestC;
function showMenu(){$('menu').style.display='flex';$('over').style.display='none';$('hud').style.display='none';$('meta').style.display='none';}
function showRun(){$('menu').style.display='none';$('over').style.display='none';$('hud').style.display='flex';$('meta').style.display='block';}
function showOver(){$('over').style.display='flex';$('hud').style.display='none';
 $('ostats').textContent=fmt(Math.floor(S.score))+' · 💩 '+S.coins+' · '+Math.floor(S.dist)+'m';
 $('obest').textContent='РЕКОРД '+fmt(S.best)+' · 💩 '+S.bestC;
 setTimeout(()=>{$('ogo').style.visibility='visible';},800);}
showMenu();
// ---------- update ----------
const M4=new THREE.Matrix4(),Q0=new THREE.Quaternion(),V1=new THREE.Vector3(),V0=new THREE.Vector3(1,1,1);
function updInst(){
 for(let i=0;i<30;i++){V1.set(0,.05,((S.dist*10)%4)-i*4+4);M4.compose(V1,Q0,V0);ties.setMatrixAt(i,M4);}
 ties.instanceMatrix.needsUpdate=true;
 for(let i=0;i<10;i++){const c=cloudPos[i];V1.set(c.x,c.y,c.z);V0.set(c.s,c.s*.35,c.s*.6);
  M4.compose(V1,Q0,V0);clouds.setMatrixAt(i,M4);}
 clouds.instanceMatrix.needsUpdate=true;V0.set(1,1,1);
 for(let i=0;i<NCity;i++){const b=cityPos[i],z=60-(((b.z+S.dist*.55)%300)+300)%300;
  V1.set(b.x,b.h/2,z);V0.set(b.w,b.h,b.w);
  M4.compose(V1,Q0,V0);city.setMatrixAt(i,M4);}
 city.instanceMatrix.needsUpdate=true;V0.set(1,1,1);}
function update(dt){
 S.t+=dt;
 for(let i=0;i<NP;i++){if(plife[i]>0){plife[i]-=dt;const k=i*3;
  ppos[k]+=pvel[k]*dt;ppos[k+1]+=pvel[k+1]*dt;ppos[k+2]+=pvel[k+2]*dt;pvel[k+1]-=9*dt;
  if(plife[i]<=0)ppos[k+1]=-50;}}
 pPts.geometry.attributes.position.needsUpdate=true;
 S.shake=Math.max(0,S.shake-dt*2.5);S.flash=Math.max(0,S.flash-dt*1.8);$('flash').style.opacity=S.flash;
 if(S.mode==='over'){S.dead+=dt;S.ts+=(1-S.ts)*Math.min(1,dt*2);}
 if(S.mode!=='run')return;
 S.speed=Math.min(52,26+S.dist/220);
 const dz=S.speed*dt;S.dist+=dz;S.score+=dz;
 S.x+=((S.lane-1)*LANE-S.x)*Math.min(1,dt*14);
 S.vy-=GRAV*dt;S.y+=S.vy*dt;
 if(S.y<=0){if(S.air)burst(S.x,.1,0,0xB09A7A,6);S.y=0;S.vy=0;S.air=false;}
 if(S.roll>0)S.roll-=dt;
 S.anim+=dt*(S.air||S.roll>0?4:S.speed*.32);
 for(const o of S.obs)o.z+=dz,o.m.position.z=o.z;
 for(const c of S.coinsArr){if(!c.missed){c.z+=dz;c.m.position.z=c.z;} c.m.rotation.y+=dt*4;}
 S.obs=S.obs.filter(o=>o.z<DESPAWN_Z||(scene.remove(o.m),false));
 // missed poops stop moving forward once they pass the player (z>0.9) and are removed far behind
 for(const c of S.coinsArr){if(!c.taken&&!c.missed&&c.z>0.9){c.missed=true;}}
 S.coinsArr=S.coinsArr.filter(c=>c.z<25&&!c.taken||(scene.remove(c.m),false));
 if(S.dist>=nextSpawn)spawn();
 collide();}
// ---------- rig + camera ----------
function animRig(){
 player.position.set(S.x,S.y,0);
 shadow.position.set(S.x,.04,0);const k=Math.max(.3,1-S.y*.22);shadow.scale.setScalar(k);
 if(S.roll>0){const p=1-S.roll/.5;rig.rotation.x=-p*TAU;rig.position.y=.42;
  armL.rotation.x=armR.rotation.x=-1.2;legL.rotation.x=legR.rotation.x=1.0;return;}
 rig.rotation.x=0;rig.position.y=0;
 if(S.air){rig.rotation.x=-.12;armL.rotation.x=armR.rotation.x=-2.1;legL.rotation.x=-.9;legR.rotation.x=.5;return;}
 const a=S.anim*2,sw=Math.sin(a);
 rig.position.y=Math.abs(Math.sin(a))*.03;
 legL.rotation.x=sw*.9;legR.rotation.x=-sw*.9;
 armL.rotation.x=-sw*.8;armR.rotation.x=sw*.8;}
let camX=0,camF=70;
function animCam(dt){
 camX+=(S.x*.55-camX)*Math.min(1,dt*6);
 camera.position.set(camX+(Math.random()-.5)*.5*S.shake,2.45+S.y*.22+(Math.random()-.5)*.5*S.shake,7.1);
 camera.lookAt(S.x*.7,1.15,-9);
 camera.rotation.z+=(S.x*.55-camera.position.x)*.04-camera.rotation.z;
 const tf=70+(S.speed-26)*.5;camF+=(tf-camF)*Math.min(1,dt*3);
 if(Math.abs(camera.fov-camF)>.05){camera.fov=camF;camera.updateProjectionMatrix();}}
addEventListener('resize',()=>{renderer.setSize(innerWidth,innerHeight);
 camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();});
// ---------- loop ----------
let last=performance.now();
function loop(now){const dt=Math.min(.05,(now-last)/1000);last=now;
 update(dt*S.ts);updInst();animRig();animCam(dt);
 $('score').textContent=fmt(Math.floor(S.score));
 $('coins').textContent='💩 '+S.coins;
 $('best').textContent=fmt(S.best);
 $('meta').textContent=Math.floor(S.dist)+'m \u00b7 '+Math.floor(S.speed)+'km/h';
 renderer.render(scene,camera);
 requestAnimationFrame(loop);}
requestAnimationFrame(loop);
