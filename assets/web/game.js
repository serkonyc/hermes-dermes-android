// Neon Runner 3D — three.js r147 UMD. Logic ported from v2; renderer replaced.
'use strict';
const CY=0x2DE2E6, VI=0x7A5CFF, GO=0xF0C34E, RD=0xF0506E, BG=0x05070C;
const LANE=1.15, GRAV=26, JUMP_V=8.6, SPAWN_Z=-70, DESPAWN_Z=6, TAU=Math.PI*2;
const $=id=>document.getElementById(id);
let renderer,scene,camera,composer;
try{
  renderer=new THREE.WebGLRenderer({canvas:$('c'),antialias:false,powerPreference:'high-performance'});
}catch(e){ document.body.innerHTML='<p style="color:#2DE2E6;padding:40px;font:14px monospace">WebGL недоступен: '+e.message+'</p>'; throw e; }
const DPR=Math.min(devicePixelRatio||1,location.search.includes('slow')?.75:1.75); // 'slow': headless/software-GL test mode
renderer.setPixelRatio(DPR); renderer.setSize(innerWidth,innerHeight);
renderer.setClearColor(BG);
renderer.outputEncoding=THREE.sRGBEncoding; // r147 default linear -> scene ~6x too dark
scene=new THREE.Scene(); scene.fog=new THREE.Fog(BG,26,66);scene.fog.color.convertSRGBToLinear();
camera=new THREE.PerspectiveCamera(70,innerWidth/innerHeight,0.1,300);
scene.add(new THREE.HemisphereLight(0x33405c,0x05070C,0.7));
const keyL=new THREE.DirectionalLight(CY,0.7); keyL.position.set(-4,9,4); scene.add(keyL);
const rimL=new THREE.DirectionalLight(VI,0.8); rimL.position.set(3,4,-8); scene.add(rimL);
const M=(c,e,i)=>{const t=new THREE.Color(c).convertSRGBToLinear(),t2=new THREE.Color(e||0).convertSRGBToLinear();return new THREE.MeshStandardMaterial({color:t,emissive:t2,emissiveIntensity:i||1,roughness:.55,metalness:.15});};
const box=(w,h,d,m)=>new THREE.Mesh(new THREE.BoxGeometry(w,h,d),m);
// ---------- world ----------
const road=new THREE.Mesh(new THREE.PlaneGeometry(12,260),M(0x0d1320)); road.rotation.x=-Math.PI/2; road.position.z=-95; scene.add(road);
const edgeM=M(0x02222a,CY,2.2);
for(const s of[-1,1]){const e=box(.06,.06,260,edgeM);e.position.set(s*LANE*1.62,.03,-95);scene.add(e);}
const dashM=M(0x1a2540,0x2DE2E6,.35);
const dashes=new THREE.InstancedMesh(new THREE.PlaneGeometry(.08,1.5),dashM,44);
scene.add(dashes);
const QF=new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1,0,0),-Math.PI/2); // dashes lay flat
const poleM=M(0x141b2c,0x7A5CFF,.25), lampM=M(0x123,VI,2.4);
const poles=new THREE.InstancedMesh(new THREE.BoxGeometry(.07,1.15,.07),poleM,18);
const lamps=new THREE.InstancedMesh(new THREE.SphereGeometry(.08,6,6),lampM,18);
scene.add(poles);scene.add(lamps);
const cityM=M(0x0a0f1a,0x101a30,.25);
const NCity=64, city=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),cityM,NCity), cityPos=[];
for(let i=0;i<NCity;i++){const s=(Math.random()*2-1);cityPos.push({x:s*(9+Math.random()*22),z:Math.random()*240,h:4+Math.random()*12,w:2+Math.random()*4});}
scene.add(city);
// sun: canvas texture plane, fog off
{const cv=document.createElement('canvas');cv.width=cv.height=128;const g=cv.getContext('2d');
 const gr=g.createLinearGradient(0,0,0,128);gr.addColorStop(0,'#F0506E');gr.addColorStop(1,'#F0C34E');
 g.fillStyle=gr;g.beginPath();g.arc(64,64,60,0,TAU);g.fill();
 g.globalCompositeOperation='destination-out';
 for(let i=0;i<6;i++)g.fillRect(0,64+i*10+i*i*.9,128,2+i*.8);
 const t=new THREE.CanvasTexture(cv);t.encoding=THREE.sRGBEncoding;
 const sun=new THREE.Mesh(new THREE.PlaneGeometry(46,23),new THREE.MeshBasicMaterial({map:t,transparent:true,fog:false,depthWrite:false}));
 sun.position.set(0,12,-210);sun.scale.set(1.35,1.35,1);scene.add(sun);}
{const n=140,pos=new Float32Array(n*3);
 for(let i=0;i<n;i++){pos[i*3]=(Math.random()*2-1)*60;pos[i*3+1]=6+Math.random()*35;pos[i*3+2]=-30-Math.random()*160;}
 const g=new THREE.BufferGeometry();g.setAttribute('position',new THREE.BufferAttribute(pos,3));
 scene.add(new THREE.Points(g,new THREE.PointsMaterial({color:0xcfe0ff,size:.5,sizeAttenuation:true,fog:false,transparent:true,opacity:.8})));}
// ---------- player rig ----------
const bodyM=M(0x0e1420,CY,.18), trimM=M(0x02222a,CY,2.0);
const player=new THREE.Group(), rig=new THREE.Group(); player.add(rig); scene.add(player);
function limb(w,h,trim){const p=new THREE.Group();const m=box(w,h,w,bodyM);m.position.y=-h/2;p.add(m);
 if(trim){const s=box(w*.4,h*.6,w*.4,trimM);s.position.set(0,-h*.55,-w*.55);p.add(s);}return p;}
{const torso=box(.4,.52,.24,bodyM);torso.position.y=1.06;rig.add(torso);
 const chest=box(.3,.05,.02,trimM);chest.position.set(0,1.12,-.13);rig.add(chest);
 const head=box(.24,.24,.22,bodyM);head.position.y=1.5;rig.add(head);
 const visor=box(.2,.05,.02,trimM);visor.position.set(0,1.52,-.12);rig.add(visor);
 var armL=limb(.1,.4,false),armR=limb(.1,.4,false),legL=limb(.13,.5,true),legR=limb(.13,.5,true);
 armL.position.set(-.27,1.3,0);armR.position.set(.27,1.3,0);
 legL.position.set(-.12,.78,0);legR.position.set(.12,.78,0);
 rig.add(armL,armR,legL,legR);}
const shadow=new THREE.Mesh(new THREE.CircleGeometry(.5,20),new THREE.MeshBasicMaterial({color:0,transparent:true,opacity:.5}));
shadow.rotation.x=-Math.PI/2;shadow.position.y=.02;scene.add(shadow);
// ---------- templates ----------
function tLow(){const g=new THREE.Group();const b=box(1.02,.85,.8,M(0x1a1430,VI,.5));b.position.y=.425;
 const s=box(1.04,.07,.82,M(0x2a1a4a,0xA78BDB,2.2));s.position.y=.85;g.add(b,s);return g;}
function tBar(){const g=new THREE.Group();const b=box(1.1,.42,.5,M(0x232a45,0x7F8CF0,.35));b.position.y=1.28;
 const s=box(1.1,.06,.52,M(0x332a08,GO,1.5));s.position.y=1.07;
 const l1=box(.07,1.06,.07,M(0x141b2c)),l2=l1.clone();l1.position.set(-.52,.53,0);l2.position.set(.52,.53,0);
 g.add(b,s,l1,l2);return g;}
function tWall(){const g=new THREE.Group();const b=box(1.1,2.6,.45,M(0x2a1020,RD,1.1));b.position.y=1.3;
 const f1=box(1.14,.08,.5,M(0x330a14,0xff97a8,2));f1.position.y=2.58;
 const f2=box(1.14,.08,.5,f1.material);f2.position.y=.06;g.add(b,f1,f2);return g;}
function tCoin(){return new THREE.Mesh(new THREE.OctahedronGeometry(.26),M(0x332a08,GO,1.7));}
const TPL={low:tLow,bar:tBar,wall:tWall};
// ---------- state ----------
const S={mode:'menu',t:0,dist:0,speed:26,coins:0,score:0,
 best:+(localStorage.getItem('nr_best')||0),bestC:+(localStorage.getItem('nr_bestc')||0),
 lane:1,x:0,y:0,vy:0,air:false,roll:0,anim:0,dead:0,shake:0,flash:0,ts:1};
S.x=0; let nextSpawn=30;
S.obs=[];S.coinsArr=[];
function reset(){S.mode='run';S.t=0;S.dist=0;S.speed=26;S.coins=0;S.score=0;S.lane=1;S.x=0;S.y=0;S.vy=0;
 S.air=false;S.roll=0;S.anim=0;S.dead=0;S.shake=0;S.flash=0;S.ts=1;
 for(const o of S.obs)scene.remove(o.m);for(const c of S.coinsArr)scene.remove(c.m);
 S.obs=[];S.coinsArr=[];nextSpawn=30;}
// ---------- spawn ----------
function addObs(type,lane){const m=TPL[type]();m.position.set((lane-1)*LANE,0,SPAWN_Z);scene.add(m);S.obs.push({type,lane,z:SPAWN_Z,m});}
function spawn(){
 const diff=Math.min(1,S.dist/3000), r=Math.random();
 if(r<.34)addObs('low',(Math.random()*3)|0);
 else if(r<.62)addObs('bar',(Math.random()*3)|0);
 else if(r<.84){const bl=new Set();const n=diff>.4&&Math.random()<.5?2:1;
   while(bl.size<n)bl.add((Math.random()*3)|0);bl.forEach(l=>addObs('wall',l));}
 else{const l=(Math.random()*3)|0,aerial=Math.random()<.3;
  for(let i=0;i<6;i++){const m=tCoin();const y=aerial?1.5+Math.sin(i*.9)*.4:.9;
   m.position.set((l-1)*LANE,y,SPAWN_Z+i*2.2);scene.add(m);S.coinsArr.push({lane:l,z:SPAWN_Z+i*2.2,y,m,taken:false});}}
 nextSpawn=S.dist+16+Math.random()*14-diff*8;}
// ---------- particles ----------
const NP=260,ppos=new Float32Array(NP*3),pcol=new Float32Array(NP*3),pvel=new Float32Array(NP*3),plife=new Float32Array(NP);
{const g=new THREE.BufferGeometry();g.setAttribute('position',new THREE.BufferAttribute(ppos,3));
 g.setAttribute('color',new THREE.BufferAttribute(pcol,3));
 var pPts=new THREE.Points(g,new THREE.PointsMaterial({size:.12,vertexColors:true,transparent:true,fog:false,blending:THREE.AdditiveBlending,depthWrite:false}));
 scene.add(pPts);}
let pCur=0;
function burst(x,y,z,col,n){const c=new THREE.Color(col);
 for(let i=0;i<n;i++){const k=pCur++%NP;ppos[k*3]=x;ppos[k*3+1]=y;ppos[k*3+2]=z;
  pvel[k*3]=(Math.random()-.5)*6;pvel[k*3+1]=Math.random()*5+1;pvel[k*3+2]=(Math.random()-.5)*6;
  pcol[k*3]=c.r;pcol[k*3+1]=c.g;pcol[k*3+2]=c.b;plife[k]=.7;}
 pPts.geometry.attributes.position.needsUpdate=pPts.geometry.attributes.color.needsUpdate=true;}
// ---------- collision ----------
const hit=o=>Math.abs(o.z)<.95&&o.lane===S.lane&&Math.abs(S.x-(o.lane-1)*LANE)<LANE*.6;
function collide(){const h=S.roll>0?.8:1.62,feet=S.y,top=S.y+h;
 for(const o of S.obs){if(!hit(o))continue;
  if(o.type==='low'&&feet<.8)return die();
  if(o.type==='bar'&&top>1.05)return die();
  if(o.type==='wall')return die();}
 for(const c of S.coinsArr){if(!c.taken&&Math.abs(c.z)<.9&&c.lane===S.lane&&Math.abs(S.y+.6-c.y)<.9){
  c.taken=true;scene.remove(c.m);S.coins++;burst((c.lane-1)*LANE,c.y,c.z,GO,10);}}}
function die(){S.mode='over';S.dead=.001;S.shake=1;S.flash=.45;S.ts=.32;
 S.best=Math.max(S.best,Math.floor(S.score));localStorage.setItem('nr_best',S.best);
 S.bestC=Math.max(S.bestC,S.coins);localStorage.setItem('nr_bestc',S.bestC);
 burst(S.x,S.y+.8,0,CY,40);showOver();}
// ---------- input ----------
function move(d){S.lane=Math.max(0,Math.min(2,S.lane+d));}
function jump(){if(!S.air&&S.roll<=0){S.vy=JUMP_V;S.air=true;}}
function doRoll(){S.roll=.5;if(S.air)S.vy=-14;}
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
$('mbest').textContent='BEST '+fmt(S.best)+' \u00b7 \u25cf '+S.bestC;
function showMenu(){$('menu').style.display='flex';$('over').style.display='none';$('hud').style.display='none';$('meta').style.display='none';}
function showRun(){$('menu').style.display='none';$('over').style.display='none';$('hud').style.display='flex';$('meta').style.display='block';}
function showOver(){$('over').style.display='flex';$('hud').style.display='none';
 $('ostats').textContent='SCORE '+fmt(Math.floor(S.score))+' \u00b7 \u25cf '+S.coins+' \u00b7 '+Math.floor(S.dist)+'m';
 $('obest').textContent='BEST '+fmt(S.best)+' \u00b7 \u25cf '+S.bestC;
 setTimeout(()=>{$('ogo').style.visibility='visible';},800);}
showMenu();
// ---------- update ----------
const M4=new THREE.Matrix4(),Q0=new THREE.Quaternion(),V1=new THREE.Vector3(),V0=new THREE.Vector3(1,1,1),VS=new THREE.Vector3();
function updInst(){
 for(let i=0;i<44;i++){const l=i%2,z=-((S.dist%4)+((i/2)|0)*4);
  V1.set((l?LANE:-LANE)*.5,.01,z);M4.compose(V1,QF,V0);dashes.setMatrixAt(i,M4);}
 dashes.instanceMatrix.needsUpdate=true;
 for(let i=0;i<18;i++){const s=i%2?1:-1,z=-((S.dist%8)+((i/2)|0)*8);
  V1.set(s*LANE*1.9,.575,z);M4.compose(V1,Q0,V0);poles.setMatrixAt(i,M4);
  V1.y=1.2;M4.compose(V1,Q0,V0);lamps.setMatrixAt(i,M4);}
 poles.instanceMatrix.needsUpdate=lamps.instanceMatrix.needsUpdate=true;
 for(let i=0;i<NCity;i++){const b=cityPos[i],z=60-(((b.z+S.dist*.9)%240)+240)%240;
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
 if(S.y<=0){if(S.air)burst(S.x,.1,0,0x7EA6F5,6);S.y=0;S.vy=0;S.air=false;}
 if(S.roll>0)S.roll-=dt;
 S.anim+=dt*(S.air||S.roll>0?4:S.speed*.32);
 for(const o of S.obs)o.z+=dz,o.m.position.z=o.z;
 for(const c of S.coinsArr)c.z+=dz,c.m.position.z=c.z;
 S.obs=S.obs.filter(o=>o.z<DESPAWN_Z||(scene.remove(o.m),false));
 S.coinsArr=S.coinsArr.filter(c=>c.z<DESPAWN_Z&&!c.taken);
 if(S.dist>=nextSpawn)spawn();
 collide();}
// ---------- animate rig + camera ----------
function animRig(){
 player.position.set(S.x,S.y,0);
 shadow.position.x=S.x;const k=Math.max(.25,1-S.y*.28);shadow.scale.setScalar(k);shadow.material.opacity=.5*k;
 if(S.roll>0){const p=1-S.roll/.5;rig.rotation.x=-p*TAU;rig.position.y=.42;
  armL.rotation.x=armR.rotation.x=-1.2;legL.rotation.x=legR.rotation.x=1.0;return;}
 rig.rotation.x=0;rig.position.y=0;
 if(S.air){rig.rotation.x=-.12;armL.rotation.x=armR.rotation.x=-2.1;
  legL.rotation.x=-.9;legR.rotation.x=.5;return;}
 const a=S.anim*2,sw=Math.sin(a);
 rig.position.y=Math.abs(Math.sin(a))*.03;
 legL.rotation.x=sw*.9;legR.rotation.x=-sw*.9;
 armL.rotation.x=-sw*.8;armR.rotation.x=sw*.8;}
let camX=0,camF=70;
function animCam(dt){
 camX+=(S.x*.55-camX)*Math.min(1,dt*6);
 camera.position.set(camX+(Math.random()-.5)*.5*S.shake,2.45+S.y*.22+(Math.random()-.5)*.5*S.shake,7.1);
 camera.lookAt(S.x*.7,1.05,-9);
 camera.rotation.z+=(S.x*.55-camera.position.x)*.045-camera.rotation.z;
 const tf=70+(S.speed-26)*.5;camF+=(tf-camF)*Math.min(1,dt*3);
 if(Math.abs(camera.fov-camF)>.05){camera.fov=camF;camera.updateProjectionMatrix();}}
// ---------- bloom ----------
let BLOOM=!!(THREE.UnrealBloomPass&&THREE.EffectComposer)&&!location.search.includes('slow');
function mkComposer(){if(!BLOOM)return;
 composer=new THREE.EffectComposer(renderer);
 composer.addPass(new THREE.RenderPass(scene,camera));
 composer.bloomPass=new THREE.UnrealBloomPass(new THREE.Vector2(innerWidth/2,innerHeight/2),.85,.55,.72);
 composer.addPass(composer.bloomPass);}
mkComposer();
addEventListener('resize',()=>{renderer.setSize(innerWidth,innerHeight);
 camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();
 if(composer)composer.setSize(innerWidth,innerHeight);});
// ---------- loop ----------
let last=performance.now(),fpsAcc=0,fpsN=0,fpsLow=0;
function loop(now){const dt=Math.min(.05,(now-last)/1000);last=now;
 update(dt*S.ts);updInst();animRig();animCam(dt);
 $('score').textContent='SCORE '+fmt(Math.floor(S.score));
 $('coins').textContent='\u25cf '+S.coins;
 $('best').textContent='BEST '+fmt(S.best);
 $('meta').textContent=Math.floor(S.dist)+'m \u00b7 '+Math.floor(S.speed)+'km/h';
 if(composer)composer.render();else renderer.render(scene,camera);
 requestAnimationFrame(loop);
 fpsAcc+=dt;fpsN++;if(fpsAcc>2){if(fpsN/fpsAcc<38)fpsLow++;else fpsLow=0;fpsAcc=0;fpsN=0;
  if(fpsLow>=2&&BLOOM){BLOOM=false;composer=null;}}}// shortcut: 2 slow windows ~6s -> drop bloom; upgrade to tiered quality if devices need middle ground
requestAnimationFrame(loop);
