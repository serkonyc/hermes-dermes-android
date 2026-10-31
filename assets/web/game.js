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
// ---------- ground + dirt path ----------
const ground=new THREE.Mesh(new THREE.PlaneGeometry(300,320),M(0x3E7A2A,.95)); ground.rotation.x=-Math.PI/2; ground.position.set(0,-.02,-110); scene.add(ground);
const ties=new THREE.InstancedMesh(new THREE.BoxGeometry(LANE*3+1,.08,2.4),M(0x6B4E2E,.95),30); scene.add(ties);
// ---------- forest ----------
const TREE_PALETTE=[0x3C7A2B,0x4C9C37,0x2F6B22,0x5CBA47];
const trunkM=M(0x5A3A1F,.95);
const NTree=72, trunks=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),trunkM,NTree), canopies=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),M(0xffffff,.85),NTree), treePos=[];
const tCol=new THREE.Color();
for(let i=0;i<NTree;i++){const s=Math.random()<.5?-1:1; const h=4+Math.random()*6;
 treePos.push({x:s*(LANE*1.8+.8+Math.random()*16),z:Math.random()*320,h,w:1+Math.random()*.5,canopyH:h*.6,canopyW:2+Math.random()*1.5});
 tCol.setHex(TREE_PALETTE[i%TREE_PALETTE.length]).convertSRGBToLinear(); canopies.setColorAt(i,tCol);}
scene.add(trunks); scene.add(canopies);
// ---------- bushes ----------
const bushM=M(0x3E8A26,.9);
const NBush=48, bushes=new THREE.InstancedMesh(new THREE.BoxGeometry(.8,.6,.8),bushM,NBush), bushPos=[];
for(let i=0;i<NBush;i++){const s=Math.random()<.5?-1:1;
 bushPos.push({x:s*(LANE*1.6+.35+Math.random()*.5),z:Math.random()*220,h:.5+Math.random()*.45});}
scene.add(bushes);
// ---------- audio ----------
const AU={m:new Audio('audio/music.m4a'),pick:new Audio('audio/pickup.m4a'),
 bump:new Audio('audio/bump.m4a'),jump:new Audio('audio/jump.m4a')};
AU.m.loop=true; AU.m.volume=.35; AU.pick.volume=.55; AU.bump.volume=.7; AU.jump.volume=.35;
let audioOn=false;
function unlockAudio(){
 if(audioOn)return; audioOn=true;
 const p=AU.m.play();
 if(p&&p.catch)p.catch(()=>{audioOn=false;setTimeout(()=>AU.m.play().then(()=>{audioOn=true;}).catch(()=>{}),400);}); // Android file://: first play can reject, one delayed retry
}
function sfx(k){if(!audioOn)return;try{AU[k].currentTime=0;AU[k].play().catch(()=>{});}catch(e){}}
// unlock must ride the same gesture as game start, not a separate first touch
const _act=act; act=function(a){unlockAudio();return _act(a);};
addEventListener('touchstart',unlockAudio,{passive:true});
addEventListener('mousedown',unlockAudio);
// file:// WebView sometimes reports NotAllowedError even inside the gesture; retry on any later gesture too
addEventListener('touchend',()=>{if(!audioOn)unlockAudio();},{passive:true});

// ---------- player: blocky forest hero ----------
const skinM=M(0xF4D0B0,.8), tunicM=M(0x3FAE5A,.8), beltM=M(0x6B3E2A,.85), bootM=M(0x6B4A2F,.85), hatM=M(0x41C05E,.8), hairM=M(0x2E2E2E,.9);
const player=new THREE.Group(), rig=new THREE.Group(); player.add(rig); scene.add(player);
function limb(w,h,m,pivotY){const p=new THREE.Group();const b=box(w,h,w,m);b.position.y=-h/2;p.add(b);p.position.y=pivotY;return p;}
var armL,armR,legL,legR, head, sword;
{head=new THREE.Group(); head.position.set(0,.84,0); rig.add(head);
 const face=box(.6,.6,.6,skinM); head.add(face);
 const hair=box(.62,.42,.08,hairM); hair.position.set(0,.16,.34); head.add(hair);
 const eyeL=box(.1,.12,.08,M(0x1A2E14,.6)); eyeL.position.set(-.14,.04,.28); const eyeR=eyeL.clone(); eyeR.position.x=.14; head.add(eyeL,eyeR);
 const hat=new THREE.Mesh(new THREE.ConeGeometry(.44,.36,4),hatM); hat.position.set(0,.5,0); hat.rotation.y=Math.PI/4; head.add(hat);
 const body=box(.64,.56,.32,tunicM); body.position.set(0,.3,0); rig.add(body);
 const belt=box(.68,.08,.36,beltM); belt.position.set(0,.08,0); rig.add(belt);
 armL=limb(.18,.52,tunicM,.58); armR=limb(.18,.52,tunicM,.58); armL.position.set(-.4,.28,0); armR.position.set(.4,.28,0); rig.add(armL,armR);
 legL=limb(.24,.6,tunicM,.0); legR=limb(.24,.6,tunicM,.0); legL.position.set(-.15,-.26,0); legR.position.set(.15,-.26,0); rig.add(legL,legR);
 const bootL=box(.26,.16,.26,bootM); const bootR=bootL.clone(); bootL.position.set(-.15,-.58,0); bootR.position.set(.15,-.58,0); rig.add(bootL,bootR);
 // carried sword on the back
 sword=new THREE.Group();
 const blade=box(.08,.78,.04,M(0xC4CBD2,.35,.75)); blade.position.y=.34;
 const guard=box(.22,.05,.05,M(0x6B3E2A,.8)); guard.position.y=.02;
 const grip=box(.06,.16,.06,M(0x6B3E2A,.8)); grip.position.y=-.08;
 sword.add(blade,guard,grip); sword.position.set(0,.28,-.32); sword.rotation.x=.48; rig.add(sword);
}
const shCv=document.createElement('canvas');shCv.width=shCv.height=64;{const g=shCv.getContext('2d');
 const rg=g.createRadialGradient(32,32,4,32,32,30);rg.addColorStop(0,'rgba(30,22,12,.55)');rg.addColorStop(1,'rgba(30,22,12,0)');
 g.fillStyle=rg;g.fillRect(0,0,64,64);}
 const shadow=new THREE.Mesh(new THREE.PlaneGeometry(1.2,1.2),
  new THREE.MeshBasicMaterial({map:new THREE.CanvasTexture(shCv),transparent:true,depthWrite:false}));
shadow.rotation.x=-Math.PI/2;shadow.position.y=.04;shadow.renderOrder=1;scene.add(shadow);
// ---------- templates: forest obstacles ----------
function tLow(){ // low bush
 const g=new THREE.Group();const b=box(1.02,.7,.6,M(0x3E8A26,.9));b.position.y=.35;
 const s=box(1.06,.16,.66,M(0x5CBA47,.9));s.position.y=.74;
 const f1=box(.1,.6,.66,M(0x2F6B22,.9)),f2=f1.clone();f1.position.set(-.46,.3,0);f2.position.set(.46,.3,0);
 g.add(b,s,f1,f2);return g;}
function tBar(){ // fallen branch gantry: wood beam + leaves plate
 const g=new THREE.Group();const b=box(1.12,.5,.5,M(0x8A5A30,.95));b.position.y=1.32;
 const s=box(1.06,.2,.54,M(0x4C9C37,.9));s.position.y=1.06;
 const l1=box(.09,1.06,.09,M(0x5A3A1F,.9)),l2=l1.clone();l1.position.set(-.53,.53,0);l2.position.set(.53,.53,0);
 g.add(b,s,l1,l2);return g;}
function tWall(){ // big boulder block
 const g=new THREE.Group();
 const b=box(1.06,2.35,4.2,M(0x7A7875,.9));b.position.y=1.42;
 const w1=box(1.1,.45,.9,M(0x9E9E9E,.9)),w2=w1.clone();w1.position.set(0,1.9,-1);w2.position.set(0,1.9,1);
 const st=box(1.1,.16,4.25,M(0x6A6A6A,.85));st.position.y=1.35;
 const rf=box(.94,.12,4.2,M(0x606060,.85));rf.position.y=2.62;
 const nose=box(1.0,.5,.25,M(0x5A5A5A,.85));nose.position.set(0,.5,-2.12);
 g.add(b,w1,w2,st,rf,nose);return g;}
function tCoin(){ // sword pickup
 const g=new THREE.Group();
 const blade=box(.08,.7,.05,M(0xC4CBD2,.35,.75)); blade.position.y=.65;
 const guard=box(.22,.05,.05,M(0x6B3E2A,.85)); guard.position.y=.26;
 const grip=box(.06,.16,.06,M(0x6B3E2A,.85)); grip.position.y=.12;
 const pommel=new THREE.Mesh(new THREE.SphereGeometry(.05,8,6),M(0xFFD24A,.5,.2)); pommel.position.y=.04;
 g.add(blade,guard,grip,pommel); return g;}
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
 else if(r<.84){const bl=new Set();const n=diff>.4&&Math.random()<.5?2:1;   // boulders more often
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
  const near=o.type==='wall'?(o.z+2.1>-0.4&&o.z-2.1<0.4):Math.abs(o.z)<0.95;   // boulder is 4.2 long
  if(!near)continue;
  if(o.type==='low'&&feet<.7)return die();
  if(o.type==='bar'&&top>1.05)return die();
  if(o.type==='wall')return die();}
 for(const c of S.coinsArr){if(!c.taken&&Math.abs(c.z)<.9&&c.lane===S.lane&&Math.abs(S.y+.6-c.y)<.9){
  c.taken=true;scene.remove(c.m);S.coins++;sfx('pick');burst((c.lane-1)*LANE,c.y,c.z,0xC4CBD2,10);}}}
function die(){S.mode='over';S.dead=.001;S.shake=1;S.flash=.4;S.ts=.32;sfx('bump');
 S.best=Math.max(S.best,Math.floor(S.score));localStorage.setItem('nr_best',S.best);
 S.bestC=Math.max(S.bestC,S.coins);localStorage.setItem('nr_bestc',S.bestC);
 burst(S.x,S.y+.8,0,0xF5F5F5,30);showOver();}
// ---------- input ----------
function move(d){S.lane=Math.max(0,Math.min(2,S.lane+d));}
function jump(){if(!S.air&&S.roll<=0){S.vy=JUMP_V;S.air=true;sfx('jump');}}
function doRoll(){S.roll=.45;if(S.air)S.vy=Math.min(S.vy,-2);}
function act(a){if(window.dlog&&a!=='T'&&a!=='O')dlog.swipe();
 if(S.mode==='menu'){reset();showRun();if(window.dlog)dlog.run();return;}
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
$('mbest').textContent='РЕКОРД '+fmt(S.best)+' · ⚔ '+S.bestC;
function showMenu(){$('menu').style.display='flex';$('over').style.display='none';$('hud').style.display='none';$('meta').style.display='none';}
function showRun(){$('menu').style.display='none';$('over').style.display='none';$('hud').style.display='flex';$('meta').style.display='block';}
function showOver(){$('over').style.display='flex';$('hud').style.display='none';
 $('ostats').textContent=fmt(Math.floor(S.score))+' · ⚔ '+S.coins+' · '+Math.floor(S.dist)+'m';
 $('obest').textContent='РЕКОРД '+fmt(S.best)+' · ⚔ '+S.bestC;
 setTimeout(()=>{$('ogo').style.visibility='visible';},800);}
showMenu();
// ---------- update ----------
const M4=new THREE.Matrix4(),Q0=new THREE.Quaternion(),V1=new THREE.Vector3(),V0=new THREE.Vector3(1,1,1);
function updInst(){
 for(let i=0;i<30;i++){V1.set(0,.05,((S.dist*10)%4)-i*4+4);M4.compose(V1,Q0,V0);ties.setMatrixAt(i,M4);}
 ties.instanceMatrix.needsUpdate=true;
 for(let i=0;i<10;i++){const c=cloudPos[i];V1.set(c.x,c.y,-260+(((c.z+S.dist*.35)%320)+320)%320);V0.set(c.s,c.s*.35,c.s*.6);
  M4.compose(V1,Q0,V0);clouds.setMatrixAt(i,M4);}
 clouds.instanceMatrix.needsUpdate=true;V0.set(1,1,1);
 for(let i=0;i<NTree;i++){const b=treePos[i],z=-260+(((b.z+S.dist*.85)%320)+320)%320;
  V1.set(b.x,b.h/2,z);V0.set(b.w,b.h,b.w);M4.compose(V1,Q0,V0);trunks.setMatrixAt(i,M4);
  V1.set(b.x,b.h+b.canopyH/2,z);V0.set(b.canopyW,b.canopyH,b.canopyW);M4.compose(V1,Q0,V0);canopies.setMatrixAt(i,M4);}
 trunks.instanceMatrix.needsUpdate=true;canopies.instanceMatrix.needsUpdate=true;V0.set(1,1,1);
 for(let i=0;i<NBush;i++){const b=bushPos[i],z=-200+(((b.z+S.dist)%220)+220)%220;
  V1.set(b.x,b.h/2,z);V0.set(.9,b.h,.9);M4.compose(V1,Q0,V0);bushes.setMatrixAt(i,M4);}
 bushes.instanceMatrix.needsUpdate=true;V0.set(1,1,1);}
function update(dt){
 S.t+=dt;
 for(let i=0;i<NP;i++){if(plife[i]>0){plife[i]-=dt;const k=i*3;
  ppos[k]+=pvel[k]*dt;pvel[k+1]+=pvel[k+1]*dt;pvel[k+2]+=pvel[k+2]*dt;pvel[k+1]-=9*dt;
  if(plife[i]<=0)ppos[k+1]=-50;}}
 pPts.geometry.attributes.position.needsUpdate=true;
 S.shake=Math.max(0,S.shake-dt*2.5);S.flash=Math.max(0,S.flash-dt*1.8);$('flash').style.opacity=S.flash;
 if(S.mode==='over'){S.dead+=dt;S.ts+=(1-S.ts)*Math.min(1,dt*2);}
 if(S.mode!=='run')return;
 S.speed=Math.min(52,26+S.dist/220);
 const dz=S.speed*dt;S.dist+=dz;S.score+=dz;
 S.x+=((S.lane-1)*LANE-S.x)*Math.min(1,dt*14);
 S.vy-=GRAV*dt;S.y+=S.vy*dt;
 if(S.y<=0){if(S.air)burst(S.x,.1,0,0x6B4E2E,6);S.y=0;S.vy=0;S.air=false;}
 if(S.roll>0)S.roll-=dt;
 S.anim+=dt*(S.air||S.roll>0?4:S.speed*.32);
 for(const o of S.obs)o.z+=dz,o.m.position.z=o.z;
 for(const c of S.coinsArr){c.z+=dz;c.m.position.z=c.z;c.m.rotation.y+=dt*4;}
 S.obs=S.obs.filter(o=>o.z<DESPAWN_Z||(scene.remove(o.m),false));
 S.coinsArr=S.coinsArr.filter(c=>c.z<DESPAWN_Z&&!c.taken||(scene.remove(c.m),false));
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
 armL.rotation.x=-sw*.8;armR.rotation.x=sw*.8;
 head.rotation.y=Math.sin(S.t*.8)*.06;}
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
 $('coins').textContent='⚔ '+S.coins;
 $('best').textContent=fmt(S.best);
 $('meta').textContent=Math.floor(S.dist)+'m \u00b7 '+Math.floor(S.speed)+'km/h';
 renderer.render(scene,camera);
 requestAnimationFrame(loop);}
requestAnimationFrame(loop);