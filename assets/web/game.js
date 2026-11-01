// Neon Runner -> Subway-style runner, day palette, no bloom. three.js r147 UMD.
'use strict';
const SKY=0x4FB8F5, HZ=0xBFE6FA, GR=0x8A7A66, GO=0xFFD24A, OR=0xFF8C42;let SKYDOME=null;
const LANE=1.725, GRAV=26, JUMP_V=8.6, SPAWN_Z=-70, DESPAWN_Z=6, TAU=Math.PI*2; // LANE x1.5: тропа - основная игровая зона
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
const HEMI=new THREE.HemisphereLight(0xBFE6FA,0x8a7a66,0.9);scene.add(HEMI);
const sunL=new THREE.DirectionalLight(0xFFF3D6,1.15); sunL.position.set(-14,22,10); scene.add(sunL);
function M(c,r,m){return new THREE.MeshStandardMaterial({color:new THREE.Color(c).convertSRGBToLinear(),roughness:r==null?.8:r,metalness:m||0});}
const box=(w,h,d,m)=>new THREE.Mesh(new THREE.BoxGeometry(w,h,d),m);
// ---------- sky + clouds + distant sun ----------
{const cv=document.createElement('canvas');cv.width=8;cv.height=64;const g=cv.getContext('2d');
 const gr=g.createLinearGradient(0,0,0,64);gr.addColorStop(0,'#3FA9F0');gr.addColorStop(.72,'#BFE6FA');gr.addColorStop(1,'#E7F6FF');
 g.fillStyle=gr;g.fillRect(0,0,8,64);
 const t=new THREE.CanvasTexture(cv);t.encoding=THREE.sRGBEncoding;
 const sk=new THREE.Mesh(new THREE.SphereGeometry(290,8,12),new THREE.MeshBasicMaterial({map:t,side:THREE.BackSide,fog:false,depthWrite:false}));
 scene.add(sk);SKYDOME=sk;}
 const cloudM=M(0xffffff,.95);cloudM.fog=false;cloudM.flatShading=true;
const CLOUD_P=[[ -.33,-.06,.7],[0,.14,.95],[.33,-.06,.7]]; // три сферических хлопка на облако
const clouds=new THREE.InstancedMesh(new THREE.IcosahedronGeometry(1,1),cloudM,30), cloudPos=[];
for(let i=0;i<10;i++)cloudPos.push({x:(Math.random()*2-1)*120,y:22+Math.random()*20,z:-60-Math.random()*180,s:8+Math.random()*14});
scene.add(clouds);
// 3D-солнца нет: оно на текстуре неба; объёмная сфера на горизонте давала «жёлтый диск», съезжающий на героя при изгибах тропы и прыжках
// ---------- дальние деревни и замки: кластеры домов и крепостей по сторонам от дороги ----------
const vr=(s=>()=>((s=(s*16807)%2147483647)/2147483647))(4242); // свой сид: расстановка стабильна между запусками
const noFog=m=>{m.fog=false;m.flatShading=true;return m;}; // дальше far-тумана - рисуем силуэтами, как облака
const houseBody=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),noFog(M(0xD9C9A4,.95)),18);
const houseRoof=new THREE.InstancedMesh(new THREE.ConeGeometry(.8,1,4),noFog(M(0xB0512F,.9)),18);
const towerMesh=new THREE.InstancedMesh(new THREE.CylinderGeometry(.7,.95,1,8),noFog(M(0xB6B1A6,.95)),6);
const towerTop=new THREE.InstancedMesh(new THREE.ConeGeometry(.95,1,6),noFog(M(0x44607F,.9)),6);
const castleWall=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,.5),noFog(M(0xC6C0B2,.95)),4);
// детали: светящиеся окна, зубцы стены, флагштоки с флагами
const houseWin=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,.1),noFog(M(0xFFCE6B,.4)),36);
const merlon=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),noFog(M(0xCDC7BA,.95)),20);
const poleMesh=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),noFog(M(0x4A382A,.95)),6);
const flagMesh=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),noFog(M(0xC23B3B,.9)),6);
scene.add(houseBody,houseRoof,towerMesh,towerTop,castleWall);
scene.add(houseWin,merlon,poleMesh,flagMesh);
const housePos=[],towerPos=[],wallPos=[];
for(const side of[-1,1]){
 for(let k=0;k<3;k++){const cx=side*(30+vr()*48),cz=vr()*340; // деревенька: 3 разнокалиберных дома
  for(let h=0;h<3;h++)housePos.push({x:cx+(vr()-.5)*10,z:cz+(vr()-.5)*14,w:1.8+vr()*1.6,h:2.2+vr()*1.9,rh:1.2+vr()*1.2,ry:vr()*.8-.4});}
 {const cx=side*50,cz=vr()*340; // замок: три башни с шатрами + стены между
  for(let t=0;t<3;t++)towerPos.push({x:cx+(t-1)*5.5,z:cz+(t%2)*2.5,w:2+vr()*.8,h:7+vr()*4});
  for(let t=0;t<2;t++)wallPos.push({x:cx-2.75-t*5.5,z:cz+1.2+w(t),w:5.8,h:3.6});}}
function w(t){return t%2;} // кривизна стены повторяет шахматный сдвиг башен
// ---------- детали окружения: горы, камни, заборы, стога ----------
const YAX=new THREE.Vector3(0,1,0);
const NMtn=12,mtnMesh=new THREE.InstancedMesh(new THREE.ConeGeometry(1,1,5),noFog(M(0x60748A,.95)),NMtn),mtnPos=[];
for(let i=0;i<NMtn;i++){const s=i%2?1:-1;mtnPos.push({x:s*(85+Math.random()*95),z:Math.random()*680,r:26+Math.random()*42,h:20+Math.random()*28,ry:Math.random()*3});}
const NStone=160,stoneMesh=new THREE.InstancedMesh(new THREE.IcosahedronGeometry(1,0),noFog(M(0x8A8578,.95)),NStone),stonePos=[];
for(let i=0;i<NStone;i++){const s=Math.random()<.5?-1:1;
 stonePos.push({x:s*(LANE*1.7+.6+Math.random()*13),z:Math.random()*320,w:.16+Math.random()*.4,ry:Math.random()*3});}
const NFen=144,fenceMesh=new THREE.InstancedMesh(new THREE.BoxGeometry(1,1,1),noFog(M(0x7A5A38,.95)),NFen),fencePos=[];
for(let r=0;r<18;r++){const s=r%2?1:-1,x0=s*(24+Math.random()*54),z0=Math.random()*340,ang=(Math.random()-.5)*.7;
 for(let k=0;k<8;k++)fencePos.push({x:x0+Math.cos(ang)*k*1.15,z:z0+Math.sin(ang)*k*1.15});}
const NHay=24,hayMesh=new THREE.InstancedMesh(new THREE.CylinderGeometry(.8,1,1,8),noFog(M(0xD9A94E,.95)),NHay),hayPos=[];
for(let i=0;i<NHay;i++){const s=i%2?1:-1;hayPos.push({x:s*(26+Math.random()*50),z:Math.random()*340,w:.9+Math.random()*1.1,h:.8+Math.random()*1});}
scene.add(mtnMesh,stoneMesh,fenceMesh,hayMesh);
// ---------- ground + dirt path ----------
const ground=new THREE.Mesh(new THREE.PlaneGeometry(300,320),M(0x3E7A2A,.95)); ground.rotation.x=-Math.PI/2; ground.position.set(0,-.02,-110); scene.add(ground);
// ---------- forest ----------
const TREE_PALETTE=[0x3C7A2B,0x4C9C37,0x2F6B22,0x5CBA47];
const trunkM=M(0x5A3A1F,.95);trunkM.flatShading=true;
const canopyM=M(0xffffff,.85);canopyM.flatShading=true;
const NTree=150, trunks=new THREE.InstancedMesh(new THREE.CylinderGeometry(.35,.5,1,6),trunkM,NTree), canopies=new THREE.InstancedMesh(new THREE.IcosahedronGeometry(1,0),canopyM,NTree), treePos=[];
const tCol=new THREE.Color();
for(let i=0;i<NTree;i++){const s=Math.random()<.5?-1:1; const h=4+Math.random()*6;
 treePos.push({x:s*(LANE*1.8+.8+Math.random()*16),z:Math.random()*320,h,w:1+Math.random()*.5,canopyH:h*.6,canopyW:2+Math.random()*1.5});
 tCol.setHex(TREE_PALETTE[i%TREE_PALETTE.length]).convertSRGBToLinear(); canopies.setColorAt(i,tCol);}
scene.add(trunks); scene.add(canopies);
// тени деревьев: плоские тёмные лепёшки под кронами (shadow maps на телефоне не тащим)
const tShGeo=new THREE.CircleGeometry(1,10);tShGeo.rotateX(-Math.PI/2);
const treeSh=new THREE.InstancedMesh(tShGeo,new THREE.MeshBasicMaterial({color:0x14300E,transparent:true,opacity:.3,depthWrite:false}),NTree);
treeSh.renderOrder=1;scene.add(treeSh);
// ---------- bushes ----------
const bushM=M(0x3E8A26,.9);
const NBush=96, bushes=new THREE.InstancedMesh(new THREE.BoxGeometry(.8,.6,.8),bushM,NBush), bushPos=[];
for(let i=0;i<NBush;i++){const s=Math.random()<.5?-1:1;
 bushPos.push({x:s*(LANE*1.6+.35+Math.random()*.5),z:Math.random()*220,h:.5+Math.random()*.45});}
scene.add(bushes);
// ---------- фонари вдоль дороги: загораются вечером ----------
const NL=26,lanP=[];
for(let i=0;i<NL;i++){const s=i%2?1:-1;lanP.push({x:s*(3.05+Math.random()*.3),z:i/NL*220+Math.random()*4});}
const poles=new THREE.InstancedMesh(new THREE.CylinderGeometry(.05,.07,2.3,6),M(0x4A382A,.95),NL);
const flameMat=new THREE.MeshBasicMaterial({color:new THREE.Color(0x57432e).convertSRGBToLinear(),fog:false});
const flame=new THREE.InstancedMesh(new THREE.BoxGeometry(.17,.24,.17),flameMat,NL);
const glowMat=new THREE.MeshBasicMaterial({color:new THREE.Color(0xFFC46B).convertSRGBToLinear(),fog:false,transparent:true,opacity:0,depthWrite:false,blending:THREE.AdditiveBlending});
const glow=new THREE.InstancedMesh(new THREE.IcosahedronGeometry(.36,0),glowMat,NL);
scene.add(poles,flame,glow);
// лужи света на тропе: аддитивные блики видны издалека; плюс одна живая лампа-источник у ближайшего фонаря
const lpGeo=new THREE.CircleGeometry(2.3,12);lpGeo.rotateX(-Math.PI/2);
const lpCv=document.createElement('canvas');lpCv.width=lpCv.height=64;
{const g2=lpCv.getContext('2d');const rg=g2.createRadialGradient(32,32,2,32,32,30);
 rg.addColorStop(0,'rgba(255,190,110,1)');rg.addColorStop(1,'rgba(255,190,110,0)');
 g2.fillStyle=rg;g2.fillRect(0,0,64,64);}
const lampPool=new THREE.InstancedMesh(lpGeo,new THREE.MeshBasicMaterial({map:new THREE.CanvasTexture(lpCv),transparent:true,opacity:0,depthWrite:false,blending:THREE.AdditiveBlending,fog:false}),NL);
lampPool.renderOrder=2;scene.add(lampPool);
const lampPL=new THREE.PointLight(0xFFB25E,0,22,1.1),lampPL2=new THREE.PointLight(0xFFB25E,0,22,1.1);scene.add(lampPL,lampPL2);
// ---------- audio ----------
const AU={m:new Audio('audio/music.m4a'),pick:new Audio('audio/pickup.m4a'),
 bump:new Audio('audio/bump.m4a'),jump:new Audio('audio/jump.m4a')};
AU.m.loop=true; AU.m.volume=.13; AU.pick.volume=.55; AU.bump.volume=.7; AU.jump.volume=.35;
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
addEventListener('visibilitychange',()=>{if(document.hidden)AU.m.pause();else if(audioOn&&S.mode==='run')AU.m.play().catch(()=>{});});
addEventListener('pagehide',()=>AU.m.pause()); // Android WebView: activity pause может не дать visibilitychange

// ---------- player: blocky forest hero ----------
const skinM=M(0xF4D0B0,.8), tunicM=M(0x3FAE5A,.8), beltM=M(0x6B3E2A,.85), bootM=M(0x6B4A2F,.85), hatM=M(0x41C05E,.8), hairM=M(0xD9B45B,.9); // светлые волосы
const player=new THREE.Group(), rig=new THREE.Group(); player.add(rig); scene.add(player);
const HERO_K=.78; // общий масштаб героя
player.scale.setScalar(HERO_K); // герой был слишком крупным относительно дорог и препятствий
player.rotation.y=Math.PI;   // лицом вперёд (по ходу), спиной к зрителю; локальная -z теперь смотрит в камеру
// tapered-сегмент: зауженный цилиндр + шарнирные шарики — читается как сочленённая игрушка, не труба
function seg(rt,rb,len,m,ball){const g=new THREE.Group();const c=new THREE.Mesh(new THREE.CylinderGeometry(rt,rb,Math.max(.01,len-rt*1.4),10),m);c.position.y=-len/2;g.add(c);
 if(ball!==false){const b1=new THREE.Mesh(sph(rt*.95,8,6),m);b1.position.y=0;g.add(b1);}return g;}
function limb(w,h,m,pivotY){const p=new THREE.Group();p.position.y=pivotY;const segL=h*.52;
 const up=seg(w*.62,w*.5,segL,m);p.add(up);p.upper=up; // плечо: дельта-шар сверху, книзу уже
 const elbow=new THREE.Group();elbow.position.y=-segL;p.add(elbow);
 const fore=seg(w*.46,w*.36,h*.48,m);elbow.add(fore); // предплечье тоньше
 const wrist=new THREE.Group();wrist.position.y=-h*.48;elbow.add(wrist);p.elbow=elbow;p.wrist=wrist;return p;}
const sph=(r,sx=10,sy=8)=>new THREE.SphereGeometry(r,sx,sy);
var armL,armR,legL,legR, head, sword, tip;
const LIFT=.955*HERO_K; // -.907 локально + компенсация всегда-согнутых колен (замер: иначе подошва тонет на 4.5см)
{head=new THREE.Group(); head.position.set(0,.93,0); rig.add(head); // выше груди: шея читается
 const face=new THREE.Mesh(sph(.34),skinM); head.add(face);
 const hair=new THREE.Mesh(sph(.355),hairM); hair.scale.set(1,.5,.9); hair.position.set(0,.2,-.03); head.add(hair); // шапка волос
 const hairB=new THREE.Mesh(sph(.35),hairM); hairB.scale.set(.92,.7,.5); hairB.position.set(0,.02,-.2); head.add(hairB); // затылок
 const sideL=new THREE.Mesh(sph(.3),hairM); sideL.scale.set(.28,.7,.62); const sideR=sideL.clone();
 sideL.position.set(-.3,-.04,.04); sideR.position.set(.3,-.04,.04); head.add(sideL,sideR);  // пряди по бокам
 const earL=new THREE.Mesh(new THREE.ConeGeometry(.07,.22,5),skinM),earR=earL.clone();      // эльфы уши
 earL.rotation.z=1.25;earR.rotation.z=-1.25;earL.position.set(.3,.06,0);earR.position.set(-.3,.06,0);head.add(earL,earR);
 const eyeL=box(.07,.11,.05,M(0x2A4A1E,.6)); eyeL.position.set(-.13,.02,.31); const eyeR=eyeL.clone(); eyeR.position.x=.13; head.add(eyeL,eyeR);
 const nose=new THREE.Mesh(new THREE.ConeGeometry(.045,.12,5),skinM); nose.rotation.x=Math.PI/2; nose.position.set(0,-.06,.33); head.add(nose);
 const browL=box(.1,.03,.05,hairM); browL.position.set(-.13,.13,.31); head.add(browL); const browR=browL.clone(); browR.position.x=.13; head.add(browR);
 const mouth=box(.1,.022,.04,M(0xB5736A,.8)); mouth.position.set(0,-.17,.3); head.add(mouth);
 const bang=box(.4,.09,.07,hairM); bang.position.set(0,.26,.24); bang.rotation.x=.35; head.add(bang); // чёлка из-под шляпы
 const hat=new THREE.Mesh(new THREE.CylinderGeometry(.3,.36,.15,10),hatM); hat.position.set(0,.32,0); head.add(hat);
 tip=new THREE.Mesh(new THREE.ConeGeometry(.28,.62,6),hatM); tip.position.set(0,.62,-.12); tip.rotation.set(-.55,Math.PI/4,0); head.add(tip); // загнутый кончик назад
 const chest=seg(.235,.19,.34,tunicM); chest.position.set(0,.5,0); rig.add(chest); // грудь шире талии
 const waistM=new THREE.Mesh(new THREE.CylinderGeometry(.19,.2,.2,10),tunicM); waistM.position.set(0,.24,0); rig.add(waistM);
 const pelvis=new THREE.Mesh(new THREE.CylinderGeometry(.2,.215,.16,10),tunicM); pelvis.position.set(0,.06,0); rig.add(pelvis); // таз шире — штаны
 const skirt=new THREE.Mesh(new THREE.CylinderGeometry(.215,.27,.16,10,1,true),tunicM); skirt.position.set(0,-.03,0); rig.add(skirt); // подол туники клёш
 const neck=new THREE.Mesh(new THREE.CylinderGeometry(.085,.1,.12,8),skinM); neck.position.set(0,.66,0); rig.add(neck); // шея: голова больше не на плечах
 const deltL=new THREE.Mesh(sph(.115,8,6),tunicM),deltR=deltL.clone();deltL.position.set(-.3,.56,0);deltR.position.set(.3,.56,0);rig.add(deltL,deltR);
 const collar=new THREE.Mesh(new THREE.TorusGeometry(.19,.045,6,10),M(0xF2EFE2,.8)); collar.rotation.x=Math.PI/2; collar.position.set(0,.53,.02); rig.add(collar);
 const belt=new THREE.Mesh(new THREE.CylinderGeometry(.29,.29,.1,10),beltM); belt.position.set(0,.08,0); rig.add(belt);
 const buckle=box(.1,.09,.05,M(0xFFD24A,.5,.3)); buckle.position.set(0,.08,.28); rig.add(buckle);
 const strap=new THREE.Mesh(new THREE.TorusGeometry(.3,.035,5,12),M(0x6B3E2A,.85)); strap.rotation.set(1.2,.45,0); strap.position.set(0,.34,0); rig.add(strap); // перевязь через грудь
 const pouch=box(.17,.14,.1,M(0x8A5A33,.85)); pouch.position.set(.22,.03,-.24); pouch.rotation.y=-.3; rig.add(pouch); // сумка на поясе сзади
 armL=limb(.17,.52,tunicM,.58); armR=limb(.17,.52,tunicM,.58); armL.position.set(-.31,.56,0); armR.position.set(.31,.56,0); rig.add(armL,armR); // плечи на уровне дельт
 const handL=new THREE.Mesh(sph(.08,8,6),skinM); handL.scale.set(1,1.15,.7); const handR=handL.clone(); handL.position.y=-.52; handR.position.y=-.52; armL.add(handL); armR.add(handR); // кисти вытянутые
 const wristL=new THREE.Mesh(new THREE.CylinderGeometry(.095,.1,.09,6),M(0xF2EFE2,.8)); wristL.position.y=-.47; armL.add(wristL); const wristR=wristL.clone(); armR.add(wristR); // наручи
 // ноги: таз -> бедро -> колено -> голень -> ботинок; ботинки часть ноги, качаются вместе с ней
 function makeLeg(side){const hip=new THREE.Group();hip.position.set(side*.15,-.02,0); // тазовый шарнир прямо под ремнем - ноги прикреплены к заднице
  const thMat=M(0x2E5E3E,.8);
  const thigh=seg(.125,.1,.36,thMat);thigh.position.y=-.19;hip.add(thigh); // штаны: бедро к колену уже
  const knee=new THREE.Group();knee.position.y=-.36;hip.add(knee);
  const kp=new THREE.Mesh(sph(.095,8,6),skinM);knee.add(kp); // коленная чашечка
  const shin=seg(.09,.065,.34,skinM);shin.position.y=-.19;knee.add(shin); // голень к щиколотке тонкая
  const ankle=new THREE.Group();ankle.position.y=-.4;knee.add(ankle); // голеностоп
  const sock=new THREE.Mesh(new THREE.CylinderGeometry(.085,.095,.16,6),M(0xF2EFE2,.8));sock.position.y=-.03;ankle.add(sock);
  const boot=new THREE.Mesh(sph(.12,8,6),bootM);boot.scale.set(1,.62,1.5);boot.position.set(0,-.05,.06);ankle.add(boot); // носок длиннее
  const sole=new THREE.Mesh(new THREE.BoxGeometry(.2,.045,.34),M(0x4A3320,.9));sole.position.set(0,-.1,.07);ankle.add(sole); // подошва
  return{hip,knee,ankle};}
 {const L=makeLeg(-1),R=makeLeg(1);legL=L.hip;legR=R.hip;legL.knee=L.knee;legR.knee=R.knee;legL.ankle=L.ankle;legR.ankle=R.ankle;rig.add(legL,legR);}
 // carried sword on the back
 sword=new THREE.Group();
 const blade=box(.08,.62,.04,M(0xC4CBD2,.35,.75)); blade.position.y=.27;
 const guard=box(.22,.05,.05,M(0x6B3E2A,.8));
 const grip=box(.06,.14,.06,M(0x6B3E2A,.8)); grip.position.y=-.1;
 sword.add(blade,guard,grip); sword.position.set(0,.3,-.3); sword.rotation.x=.42; rig.add(sword); // на спине; в подкате прижимается (animRig)
}
// ---------- LinkHero: самопечный jointed GLB (суставы-ноды, FK-походка ниже); примитивный герой — офлайн-фолбэк ----------
const heroM=new THREE.Group(); scene.add(heroM);
const VER='4.39.2';
let MJ=null,mReady=false,deathPlayed=false,heroCOM=new THREE.Vector3(.02,.86,0),rollRoot=null;
{let gtries=0;
 const failBanner=()=>{let b=document.getElementById('gfail');if(!b){b=document.createElement('div');b.id='gfail';b.style.cssText='position:fixed;top:8px;left:8px;right:8px;padding:6px 10px;background:#8B1A1A;color:#fff;font-size:13px;border-radius:6px;z-index:99';b.textContent='⚠ Модель не загрузилась - бегу примитивом';document.body.appendChild(b);}};
 const loadGLB=()=>{const L=(typeof THREE.GLTFLoader==='function')&&new THREE.GLTFLoader();
  if(!L){mReady=false;failBanner();return;}
  const start=g=>window.LINK_GLB_B64?L.parse(b64buf(window.LINK_GLB_B64),'',g,e=>onGLBErr()):L.load('../models/link.glb'+(gtries?'?r='+gtries:''),g,undefined,onGLBErr());
  start(gltf=>{
   const root=gltf.scene; MJ={};
   root.traverse(o=>{MJ[o.name]=o;if(o.isMesh)o.frustumCulled=false;});
   if(!MJ.rig||!MJ.head){mReady=false;failBanner();return;}
   const b=new THREE.Box3().setFromObject(root); const H=b.max.y-b.min.y;
   root.scale.setScalar(1.7/H); // рост с колпаком; тело ~1.45 как у примитива
   const b2=new THREE.Box3().setFromObject(root); root.position.y=-b2.min.y;
   root.rotation.y=Math.PI;     // лицом вперёд по тропе
   rollRoot=new THREE.Group(); rollRoot.add(root); heroM.add(rollRoot); heroCOM.set((b2.max.x+b2.min.x)/2,(b2.max.y+b2.min.y)/2,(b2.max.z+b2.min.z)/2); mReady=true; player.visible=false;
   const fb=document.getElementById('gfail');if(fb)fb.remove();
   if($('ver'))$('ver').textContent='v'+VER+' · GLB';
  });
 };
 function onGLBErr(){mReady=false;if(gtries++<2)setTimeout(loadGLB,400);else failBanner();}
 function b64buf(s){const bin=atob(s),u=new Uint8Array(bin.length);for(let i=0;i<bin.length;i++)u[i]=bin.charCodeAt(i);return u.buffer;}
 loadGLB();
}

function driveHero(dt){
 const root=rollRoot||heroM; const j=MJ, ML=[j.rig,j.head,j.hat,j.armL,j.armR,j.elbowL,j.elbowR,j.legL,j.legR,j.kneeL,j.kneeR,j.ankleL,j.ankleR];
 if(!j.__cl){j.__cl=[-.35,1.62];}
 springTip(0,dt); // колпак догоняет через пружинную цель в ветрах поз (прямая запись ломала прыжок)
 const T=tgt;
 if(S.mode==='over'){ // рогдолл смерти: limp-суставы, ничком, болтанка
  if(S.y>0){S.vy-=GRAV*dt;S.y+=S.vy*dt;if(S.y<0){S.y=0;S.vy=Math.abs(S.vy)*.3;if(S.vy<.6)S.vy=0;}}
  heroM.position.set(S.x,S.y,0);
  heroM.scale.set(1,.82,1);
  for(const o of ML)for(const ax of['x','y','z'])T(o,ax,0);
  T(j.rig,'x',.8);T(j.rig,'z',S.dz||.4);j.rig.position.y=.1;
  T(j.armL,'x',-2.1);T(j.armR,'x',-1.7);T(j.armL,'z',1.1);T(j.armR,'z',-1.3);
  T(j.elbowL,'x',-1.2);T(j.elbowR,'x',-.7);
  T(j.legL,'x',.9);T(j.legR,'x',.5);T(j.kneeL,'x',-1.2);T(j.kneeR,'x',-.8);
  T(j.head,'x',.5);T(j.hat,'x',-1.9);
  springs(dt,88,8.5,ML); return;}
 if(S.roll>0){ // КУВЫРОК: собираем компактную позу, центрируем её вокруг COM, затем вращаем вокруг точки касания
  const p=1-S.roll/.62;
  const tk=Math.min(1,p*8,Math.max(0,(.86-p)/.14)), q=Math.min(1,Math.max(0,(p-.12)/.78)), x=q*Math.PI*2; // оборот завершается к p=.9: кувырок докручивается в стойку
  const rt=j.rig.parent; if(rt.__by===undefined)rt.__by=rt.position.y;
  rt.position.y=rt.__by;
  const w=p>.82?Math.min(1,(p-.82)/.18):0;
  j.rig.position.set(0,0,0);
  j.rig.rotation.x=1.4*tk;
  j.head.rotation.x=(1.2*tk)*(1-w)+.1*w;
  j.hat.rotation.x=(-3.2*tk)*(1-w)-.25*w;j.hat.rotation.z=.12*tk;
  j.armL.rotation.x=(-2.45*tk)*(1-w)-.35*w;j.armR.rotation.x=(-2.35*tk)*(1-w)+.18*w;
  j.elbowL.rotation.x=2.15*tk;j.elbowR.rotation.x=2.1*tk;
  j.armL.rotation.z=.5*tk;j.armR.rotation.z=-.5*tk;
  j.legL.rotation.x=(-2.9*tk)*(1-w)-.28*w;j.legR.rotation.x=(-2.8*tk)*(1-w)+.45*w;
  j.kneeL.rotation.x=(2.9*tk)*(1-w)+.22*w;j.kneeR.rotation.x=(2.85*tk)*(1-w)+.12*w;
  j.ankleL.rotation.x=-1.3*tk*(1-w)+.1*w;j.ankleR.rotation.x=-1.2*tk*(1-w)+.05*w;
  root.rotation.set(0,0,0); root.scale.set(1,1,1); root.position.set(0,0,0);
  heroM.position.set(S.x,S.rollY,0);
  if(!heroM.__lb)heroM.__lb=new THREE.Box3();
  if(!heroM.__com)heroM.__com=new THREE.Vector3();
  heroM.updateMatrixWorld(true);
  heroM.__lb.setFromObject(heroM); heroM.__lb.getCenter(heroM.__com);
  // фиксируем pivot: смещение локальное (мировой центр содержит x полосы - иначе кувырок телепортирует на центральную полосу)
  root.position.set(-(heroM.__com.x-S.x),-(heroM.__com.y-S.rollY),-heroM.__com.z);
  heroM.updateMatrixWorld(true);
  heroM.__lb.setFromObject(heroM);
  const dy=(heroM.__lb.max.y-heroM.__lb.min.y)*.5;
  heroM.position.set(S.x,S.rollY+dy,0);
  root.rotation.set(-x,0,0);
  // visual tuck: компактный масштаб только в собранной фазе, чтобы колпак/конечности не растягивали габарит на повороте
  heroM.scale.setScalar(1-(1-.84)*tk);
  heroM.updateMatrixWorld(true);
  heroM.__lb.setFromObject(heroM);
  heroM.position.y+=Math.max(0,-heroM.__lb.min.y)+.012;
  return;}
 heroM.position.set(S.x,S.y,0);
 if(root&&root.rotation.x!==0||root.position.lengthSq()>1e-9){root.rotation.set(0,0,0);root.position.set(0,0,0);const rt=MJ.rig.parent;if(rt.__by!==undefined)rt.position.y=rt.__by;}
 if(S.air){const up=S.vy>0; // отрыв - группировка, снижение - распластка
  j.rig.position.y=0;
  T(j.rig,'x',up?.14:-.08);
  T(j.armL,'x',up?-2.3:-1.1);T(j.armR,'x',up?-2.3:-1.1);T(j.armL,'z',.15);T(j.armR,'z',-.15);
  T(j.elbowL,'x',up?-.9:-.4);T(j.elbowR,'x',up?-.9:-.4);
  T(j.legL,'x',up?-.9:-.1);T(j.legR,'x',up?-.2:.5);
  T(j.kneeL,'x',up?1.5:.15);T(j.kneeR,'x',up?.4:.1);
  T(j.ankleL,'x',up?.5:-.2);T(j.ankleR,'x',up?.5:-.2);
  T(j.head,'x',up?.1:-.05);
  T(j.hat,'x',(up?-.3:.15)+tipQ*.6); // в прыжке колпак мягко откидывается, не выворачивается
  tipV+=Math.sin(S.t*9)*(up?.9:-1.1)*dt*22;
  springs(dt,105,11,ML); return;}
 // ---------- 4-фазная походка на GLB-суставах: FK-цепочки, наклон, таз, контр-вращения ----------
 const a=S.anim*2,sw=Math.sin(a),sq=1-.13*land;
 const spd=Math.min(1,(S.speed-18)/26); // бег, а не Семенка: бедро ±(0.8..1.2), колено поджимается в замахе, носок тянется на отрыве
 const A=.8+spd*.4;
 const legPh=ph=>{const s0=Math.sin(a+ph),rec=Math.max(0,-Math.sin(a+ph-.7));
  return{thigh:-A*s0,knee:.22+1.4*rec*rec,ankle:.55*Math.sin(a+ph+1.9)-.08};};
 const lp=legPh(0),rp=legPh(Math.PI);
 const armPh=ph=>{const s0=Math.sin(a+ph);return{shoulder:-1.05*s0-.15,elbow:-.55-.7*Math.max(0,-s0)};};
 const alp=armPh(Math.PI),arp=armPh(0); // руки в противофазе с ногами
 // боковой шаг = полноценный шаг: половина цикла ног (5.5 м пути), перенос ноги по фазе, а не таймером
 const sp=(S.stepPh!==undefined&&S.stepPh!==null)?1-Math.min(1,(S.anim-S.stepPh)/Math.PI):0,st=sp>0&&sp<1?Math.sin(Math.PI*(1-sp)):0,sd=S.stepD||0;
 const stepU=(sp>0&&sp<1)?1-sp:0; // 0..1 прогресс переноса; боковой вынос идёт по stepU, а lift и forward — по фазе a, т.е. реальному замаху
 heroM.scale.set(1+.09*land,sq,1+.09*land); // squash при приземлении
 T(j.rig,'x',.14+spd*.07+Math.sin(a*2+Math.PI*.2)*.03); // наклон вперёд + дыхание
 j.rig.position.y=-.02+Math.abs(sw)*.075; // подпрыгивание в такт шагам: два на цикл
 T(j.rig,'z',Math.sin(a)*.05-sd*.24*st);T(j.rig,'y',Math.sin(a)*.09+sd*.3*st);
 T(j.legL,'x',lp.thigh);T(j.kneeL,'x',lp.knee);T(j.ankleL,'x',lp.ankle);
 T(j.legR,'x',rp.thigh);T(j.kneeR,'x',rp.knee);T(j.ankleR,'x',rp.ankle);
 T(j.armL,'x',alp.shoulder);T(j.armR,'x',arp.shoulder);
 T(j.elbowL,'x',alp.elbow);T(j.elbowR,'x',arp.elbow);
 T(j.armL,'z',.12-Math.max(0,alp.shoulder)*.08);T(j.armR,'z',-.12+Math.max(0,arp.shoulder)*.08);
 if(st>0){ // приставной шаг: нога поднимается и уходит в сторону по stepU; z=forward остаётся в фазе a
  const lead=sd>0?j.legR:j.legL,leadK=sd>0?j.kneeR:j.kneeL,other=sd>0?j.legL:j.legR;
  const lt=sd>0?rp:lp,ot=sd>0?lp:rp;
  const lift=Math.sin(stepU*Math.PI),side=sd*stepU;
  T(lead,'x',lt.thigh*.4+.25*lift);T(lead,'z',-sd*.3*lift);
  T(leadK,'x',lt.knee*.4+.35*lift);
  T(other,'x',ot.thigh*.45+.1*lift);T(other,'z',sd*.1*lift);
  T(j.rig,'z',Math.sin(a)*.05-side*.18);T(j.rig,'x',.14+spd*.07+Math.sin(a*2+Math.PI*.2)*.03);
  T(j.armL,'z',(sd>0?.5:.05)*lift);T(j.armR,'z',(sd<0?-.5:-.05)*lift);
  T(j.rig,'y',Math.sin(a)*.09+side*.12);
  T(lead,'y',sd*.65*lift); /* local +y = world +x после root rotY=pi; lift и side идут вместе, не через thigh */ }
 T(j.head,'z',-Math.sin(a)*.03);T(j.head,'y',Math.sin(a)*.09+Math.sin(S.t*.8)*.06); // стабилизация взгляда
 T(j.head,'x',.04+Math.sin(a*2+Math.PI*.3)*.025);
 tipV+=Math.sin(a*2+Math.PI*.3)*dt*24; // импульс колпака от ускорения головы
 T(j.hat,'x',-.05+Math.sin(a*2)*.03+tipQ*.5);
 heroM.position.y+=st*.16; // подскок приставного шага
 // кукла-марионетка: корпус ведёт жёстко, конечности догоняют мягче - цепь читается связанной, а не 13 шарниров
 // пружины ног/рук: k выше квадрата частоты шага (на 44 м/с ω≈50) - иначе амплитуда срезается в разы;
 // мягкость держим недокрученным демпфированием (d<2*sqrt(k)): фаза отстает - силуэт «дышит»
 springs(dt,165,14,[j.rig,j.head]); springs(dt,2400,64,[j.armL,j.armR,j.elbowL,j.elbowR,j.legL,j.legR,j.kneeL,j.kneeR,j.ankleL,j.ankleR]);
}

// ---------- templates: Korok Forest obstacles (BotW lore) ----------
// low-poly камень: икосаэдр с детерминированным дрожжем вершин (тот же seed = та же форма)
function rockGeo(seed){const g=new THREE.IcosahedronGeometry(1,1);const p=g.attributes.position;
 // хэш от координат вершины (геометрия non-indexed): одинаковые координаты соседних граней дают одинаковый сдвиг - иначе между гранями дырки ("прозрачные валуны")
 const hsh=(x,y,z)=>{const v=Math.sin((x*127.1+y*311.7+z*74.7)*2.13+seed)*43758.5453;return v-Math.floor(v);};
 for(let i=0;i<p.count;i++){const k=1+(hsh(p.getX(i),p.getY(i),p.getZ(i))-.5)*.38;p.setXYZ(i,p.getX(i)*k,p.getY(i)*k,p.getZ(i)*k);}
 g.computeVertexNormals();return g;}
const rockMs=[M(0x8F8D89,.9),M(0x7A7875,.9),M(0x9E9C98,.9)];rockMs.forEach(m=>m.flatShading=true);
const rockGeos=[rockGeo(11),rockGeo(777),rockGeo(31337)];
const leafM=M(0x5CBA47,.85),rootM=M(0x6B4A2F,.9),vineM=M(0x3FAE5A,.85);
leafM.flatShading=rootM.flatShading=vineM.flatShading=true;
const icoG=new THREE.IcosahedronGeometry(1,0);icoG.computeVertexNormals();
function tLow(){ // Deku burr: кочка репьев у земли (прыжок)
 const g=new THREE.Group();
 const b=new THREE.Mesh(new THREE.IcosahedronGeometry(.5,1),M(0x3E8A26,.9));b.scale.set(1,.7,.6);b.position.y=.3;b.flatShading=true;g.add(b);
 for(let i=0;i<3;i++){const s=new THREE.Mesh(icoG,leafM);s.scale.set(.26,.1,.26);
  s.position.set((i-1)*.32,.62,(i%2?-.1:.1));s.rotation.y=i*1.3;g.add(s);}
 return g;}
function tBar(){ // корень Великого Дерека над тропой + лианы (подкат)
 const g=new THREE.Group();
 const b=new THREE.Mesh(new THREE.CylinderGeometry(.2,.24,1.24,8),rootM);b.rotation.z=Math.PI/2;b.position.y=1.32;g.add(b);
 const s=new THREE.Mesh(icoG,leafM);s.scale.set(1.1,.16,.5);s.position.y=1.06;g.add(s);
 for(let i=0;i<3;i++){const v=new THREE.Mesh(new THREE.CylinderGeometry(.02,.03,.5,5),vineM);
  v.position.set((i-1)*.34,.82,.05);v.rotation.z=(i-1)*.15;g.add(v);}
 const l1=new THREE.Mesh(new THREE.CylinderGeometry(.07,.09,1.06,6),rootM),l2=l1.clone();
 l1.position.set(-.53,.53,0);l2.position.set(.53,.53,0);g.add(l1,l2);return g;}
function tWall(){ // катящийся валун (BotW rolling rock) - взамен стога "вагонов"
 const g=new THREE.Group();const R=1.15;
 const core=new THREE.Mesh(rockGeos[(Math.random()*3)|0],rockMs[(Math.random()*3)|0]);
 core.scale.setScalar(R);core.position.y=R;core.rotation.set(Math.random()*2,Math.random()*2,0);g.add(core);
 g.userData.spin=core;g.userData.R=R;
 for(let i=0;i<2;i++){const p=new THREE.Mesh(rockGeos[(i+1)%3],rockMs[i]);p.scale.setScalar(.28);
  p.position.set((i?1:-1)*.85,.25,.5);p.rotation.y=i*2;g.add(p);}
 return g;}
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
function reset(){S.mode='run';S.t=0;S.dist=0;S.speed=18;S.coins=0;S.score=0;S.lane=1;S.x=0;S.y=0;S.vy=0;S.stepT=0;S.stepPh=null;
 S.air=false;S.roll=0;S.rollY=0;S.rollV=0;S.anim=0;S.dead=0;S.shake=0;S.flash=0;S.ts=1;
 for(const o of S.obs)scene.remove(o.m);for(const c of S.coinsArr)scene.remove(c.m);
 S.obs=[];S.coinsArr=[];nextSpawn=30;S.missed=0;deathPlayed=false;}
function addObs(type,lane){const m=TPL[type]();m.position.set((lane-1)*LANE,0,SPAWN_Z);scene.add(m);S.obs.push({type,lane,z:SPAWN_Z,m});}
function spawn(){
 const diff=Math.min(1,S.dist/3000), r=Math.random();
 const p=Math.random()<.45; // мечи не к каждому препятствию (частота снижена в ~1.5 раза)
 if(r<.3){const l=(Math.random()*3)|0;addObs('low',l);
  if(p)swordRun(l,4,SPAWN_Z-3.3,i=>.95+Math.sin((i+.5)/4*Math.PI)*.8);} // арка над репьем - собирай в прыжке
 else if(r<.54){const l=(Math.random()*3)|0;addObs('bar',l);
  if(p)swordRun(l,3,SPAWN_Z-6.6,()=>.45);} // низкая линия за корнем - собирается в подкате
 else if(r<.84){const bl=new Set();const n=diff>.4&&Math.random()<.5?2:1;   // валуны чаще
  while(bl.size<n)bl.add((Math.random()*3)|0);bl.forEach(l=>addObs('wall',l));
  const free=[0,1,2].find(l=>!bl.has(l)); // рядом, но в заведомо свободной полосе
  if(p&&free!==undefined)swordRun(free,4,SPAWN_Z,()=>.95);}
 else{const l=(Math.random()*3)|0,aerial=Math.random()<.3;
  if(Math.random()<.65)swordRun(l,6,SPAWN_Z,i=>aerial?1.5+Math.sin(i*.9)*.4:.95);}
 nextSpawn=S.dist+16+Math.random()*14-diff*8;}
function swordRun(lane,n,z0,yFn){for(let i=0;i<n;i++){const m=tCoin();const y=yFn(i);
 m.position.set((lane-1)*LANE,y,z0-i*2.2);m.rotation.z=.38; // наклон к вертикали, чтобы читался клинок
 scene.add(m);S.coinsArr.push({lane,z:z0-i*2.2,y,m,taken:false});}}
// ---------- particles ----------
const NP=260,ppos=new Float32Array(NP*3),pcol=new Float32Array(NP*3),pvel=new Float32Array(NP*3),plife=new Float32Array(NP);
let pPts;
{const g=new THREE.BufferGeometry();g.setAttribute('position',new THREE.BufferAttribute(ppos,3));
 g.setAttribute('color',new THREE.BufferAttribute(pcol,3));
 for(let i=1;i<NP*3;i+=3)ppos[i]=-50; // мёртвые частицы уводим под землю: иначе 260 точек лежат в (0,0,0) - та самая точка в центре тропы
 pPts=new THREE.Points(g,new THREE.PointsMaterial({size:.14,vertexColors:true,transparent:true,fog:false,depthWrite:false}));
 pPts.geometry.attributes.position.needsUpdate=true;
 scene.add(pPts);}
let pCur=0;
 // звёзды при подборе меча: пучокInstanced-звёздочек с разлётом и гравитацией
const starGeo=(()=>{const s=new THREE.Shape();for(let i=0;i<10;i++){const r=i%2?.11:.27,a=i/10*Math.PI*2-Math.PI/2;
 const px=Math.cos(a)*r,py=Math.sin(a)*r;i?s.lineTo(px,py):s.moveTo(px,py);}return new THREE.ShapeGeometry(s);})();
const stars=new THREE.InstancedMesh(starGeo,new THREE.MeshBasicMaterial({color:0xffffff,transparent:true,fog:false,side:THREE.DoubleSide,depthWrite:false}),64);
stars.renderOrder=2; scene.add(stars);
{const cA=new THREE.Color(0xFFD75E).convertSRGBToLinear(),cB=new THREE.Color(0xFFF2C0).convertSRGBToLinear();
 for(let i=0;i<64;i++){stars.setColorAt(i,i%3?cA:cB);}}
const starP=[],ZAX=new THREE.Vector3(0,0,1);
function sparkle(x,y,z){
 for(let i=0;i<24;i++){const a=Math.random()*6.283,v=2.4+Math.random()*3.2;
  starP.push({x,y,z,vx:Math.cos(a)*v,vy:1.4+Math.random()*3.4,vz:Math.sin(a)*v*.3,rot:Math.random()*6.283,vr:(Math.random()-.5)*16,t:0,life:.45+Math.random()*.3});}
 while(starP.length>64)starP.shift();}
function updPickFx(dt){
 for(let j=starP.length-1;j>=0;j--){const p=starP[j];p.t+=dt;if(p.t>=p.life){starP.splice(j,1);continue;}
  p.vy-=7.5*dt;p.x+=p.vx*dt;p.y+=p.vy*dt;p.z+=p.vz*dt;p.rot+=p.vr*dt;if(p.y<.04)p.y=.04;}
 for(let i=0;i<64;i++){const p=starP[i];
  if(!p){V0.set(0,0,0);M4.compose(V1.set(0,-50,0),Q0.identity(),V0);}
  else{const k=1-p.t/p.life,s=k*k*1.1;Q0.setFromAxisAngle(ZAX,p.rot);V1.set(p.x,p.y,p.z);V0.set(s,s,1);M4.compose(V1,Q0,V0);}
  stars.setMatrixAt(i,M4);}
 stars.instanceMatrix.needsUpdate=true;}
function burst(x,y,z,col,n){const c=new THREE.Color(col).convertSRGBToLinear();
 for(let i=0;i<n;i++){const k=pCur++%NP;ppos[k*3]=x;ppos[k*3+1]=y;ppos[k*3+2]=z;
  pvel[k*3]=(Math.random()-.5)*6;pvel[k*3+1]=Math.random()*5+1;pvel[k*3+2]=(Math.random()-.5)*6;
  pcol[k*3]=c.r;pcol[k*3+1]=c.g;pcol[k*3+2]=c.b;plife[k]=.7;}
 pPts.geometry.attributes.position.needsUpdate=pPts.geometry.attributes.color.needsUpdate=true;}
// ---------- открытая дорога <-> тоннели + погода ----------
const SEG={start:260,len:140,m:0}; // ближайший тоннель: окно [start,start+len]; стоит на дороге заранее - виден с горизонта
const WX={st:0,next:150,night:0},GRAY=new THREE.Color(.62,.62,.6);
const _cA=new THREE.Color();
const _SKY_D=new THREE.Color(1,1,1),_SKY_C=new THREE.Color(.05,.04,.035);
const _FOG_D=new THREE.Color(HZ).convertSRGBToLinear(),_FOG_C=new THREE.Color(0x171310).convertSRGBToLinear();
const _SL_D=new THREE.Color(0xFFF3D6).convertSRGBToLinear(),_SL_C=new THREE.Color(0x7d8fc0).convertSRGBToLinear();
// пещера: непрерывная труба с гуляющим радиусом и центром (неровные стены), перестраивается по изгибу дороги
const TSEG=56,TRING=16;
const tunG=new THREE.BufferGeometry();
const tunV=new Float32Array(TSEG*TRING*3);
tunG.setAttribute('position',new THREE.BufferAttribute(tunV,3));
{const idx=[];for(let i=0;i<TSEG-1;i++)for(let k=0;k<TRING;k++){const a0=i*TRING+k,b0=i*TRING+(k+1)%TRING,c0=(i+1)*TRING+k,d0=(i+1)*TRING+(k+1)%TRING;idx.push(a0,c0,b0,b0,c0,d0);}tunG.setIndex(idx);}
const tun=new THREE.Mesh(tunG,new THREE.MeshStandardMaterial({color:new THREE.Color(0x6b5a48).convertSRGBToLinear(),roughness:.95,flatShading:true,side:THREE.DoubleSide}));
scene.add(tun);
// грудa валунов вокруг трубы: снаружи читается как каменистая насыпь, а не труба
const NRK=64,tunRidge=new THREE.InstancedMesh(rockGeos[1],new THREE.MeshStandardMaterial({color:new THREE.Color(0x5d4d3e).convertSRGBToLinear(),roughness:.95,flatShading:true}),NRK);
scene.add(tunRidge);
let tunSeed=Math.random()*97+3;
const OFFC=new THREE.Color(.34,.26,.17),ONC=new THREE.Color(1,.78,.42);
function dayFx(dt){
 if(S.dist>SEG.start+SEG.len){SEG.start=S.dist+260+Math.random()*260; // следующий портал выставляется впереди, чтобы расти из горизонта
  SEG.len=110+Math.random()*90;WX.next=S.dist;tunSeed=Math.random()*97+3;}
 const d=S.dist-SEG.start; let mt;
 if(d<0)mt=Math.max(0,1+d/14);else if(d<SEG.len*.7)mt=1;else mt=Math.max(0,1-(d-SEG.len*.7)/(SEG.len*.3));
 SEG.m+=(mt-SEG.m)*Math.min(1,dt*8);
 const m=SEG.m;WX.night=m;
 if(S.dist>WX.next){const r=Math.random();WX.st=r<.55?0:r<.85?1:2;WX.next=S.dist+120+Math.random()*180;}
 let si=1.15*(1-m)+.14*m,hi=.9*(1-m)+.3*m;
 _cA.copy(_SKY_D).lerp(_SKY_C,m);sunL.color.copy(_SL_D).lerp(_SL_C,m);
 if(WX.st===1){si*=.6;_cA.lerp(GRAY,.35);}
 if(WX.st===2&&m<.3){si*=.35;hi*=.8;_cA.lerp(GRAY,.55);
  for(let n=dt*80|0;n>0;n--){const k=pCur++%NP,ix=k*3;
   ppos[ix]=S.x+(Math.random()-.5)*16;ppos[ix+1]=6+Math.random()*3;ppos[ix+2]=-14+Math.random()*22;
   pvel[ix]=0;pvel[ix+1]=-16;pvel[ix+2]=0;
   pcol[ix]=.66;pcol[ix+1]=.78;pcol[ix+2]=.88;plife[k]=.5;}
  pPts.geometry.attributes.position.needsUpdate=pPts.geometry.attributes.color.needsUpdate=true;}
 SKYDOME.material.color.copy(_cA);scene.fog.color.copy(_FOG_D).lerp(_FOG_C,m);
 scene.fog.near=45-33*m;scene.fog.far=120-70*m;
 sunL.intensity=si;HEMI.intensity=hi;
 const on=Math.min(1,m*1.6);
 flameMat.color.copy(OFFC).lerp(ONC,on);glowMat.opacity=on*.65;lampPool.material.opacity=on*.55;
 houseWin.material.color.setHex(0xFFCE6B).convertSRGBToLinear().multiplyScalar(.6+on*.9);
 let lampZ=-99,lampX=0,lampY=2.2;
 for(let i2=0;i2<NL;i2++){const b2=lanP[i2],z=-150+(((b2.z+S.dist)%220)+220)%220,x=b2.x+bend(z);
  V1.set(x,1.15,z);V0.set(1,1,1);Q0.identity();M4.compose(V1,Q0,V0);poles.setMatrixAt(i2,M4);
  V1.set(x,2.3,z);M4.compose(V1,Q0,V0);flame.setMatrixAt(i2,M4);
  const fl=.9+.18*Math.sin(S.t*7+i2*2.7);V0.set(fl*(.6+on*.7),fl*(.6+on*.7),fl*(.6+on*.7));
  M4.compose(V1,Q0,V0);glow.setMatrixAt(i2,M4);
  V1.set(x-Math.sign(b2.x)*1.6,.02,z);V0.set(1.55+on*1.3,1,1.55+on*1.3);lampPool.instanceMatrix.needsUpdate=true;lampPool.setMatrixAt(i2,M4);
  if(z<1&&z>-24&&z>lampZ){lampZ=z;lampX=x;lampY=2.2;}}
 // огибающая: разгорается за 5м до, гаснет за 4.5м до прохождений - с перекрытием соседних ламп
 const envZ=z16=>Math.max(0,Math.min((-z16-1)/4,(z16+19)/6,1));
 // сглаживание: цель выбирается дискретно (ближайший фонарь), а позиция/яркость догоняют - никаких щелчков при смене
 {const k=Math.min(1,dt*7);const tx=lampX,ty=lampY,tz=lampZ,ti=on*4.5*envZ(lampZ);
  lampPL.position.set(lampPL.position.x+(tx-lampPL.position.x)*k,ty,lampPL.position.z+(tz-lampPL.position.z)*k);
  lampPL.intensity+=(ti-lampPL.intensity)*k;}
 // вторая лампа: следующий фонарь — чтобы тропы между ними не были пустыми
 lampPL2.__ti=0;
 for(let j=0;j<NL;j++){const b2=lanP[j],z=-150+(((b2.z+S.dist)%220)+220)%220;if(z<=lampZ-2&&z>lampZ-20){lampPL2.__tx=b2.x+bend(z)-Math.sign(b2.x)*.5;lampPL2.__tz=z;lampPL2.__ti=on*3.8*envZ(z);break;}}
 {const k=Math.min(1,dt*7);
  lampPL2.position.set(lampPL2.position.x+((lampPL2.__tx||lampPL2.position.x)-lampPL2.position.x)*k,2.2,lampPL2.position.z+((lampPL2.__tz!==undefined?lampPL2.__tz:lampPL2.position.z)-lampPL2.position.z)*k);
  lampPL2.intensity+=((lampPL2.__ti||0)-lampPL2.intensity)*k;}
 poles.instanceMatrix.needsUpdate=flame.instanceMatrix.needsUpdate=glow.instanceMatrix.needsUpdate=lampPool.instanceMatrix.needsUpdate=tunRidge.instanceMatrix.needsUpdate=true;
 // сечения пещеры: радиус и центр «дышат» вдоль длины, раструб у входов
 for(let i=0;i<TSEG;i++){const s=i/(TSEG-1)*SEG.len,zw=SEG.start+s,z=S.dist-zw; // вперёд = -z
  const e=Math.min(1,s/18,(SEG.len-s)/18); // стены неровные только в середине; у входов и выхода гладко
  const r=4.6+(1.15*Math.sin(s*.11+tunSeed)+.75*Math.sin(s*.27+tunSeed*2))*e;
  const cx=bend(z),cy=1.75+.4*Math.sin(s*.06+tunSeed*5)*e; // центр строго по тропе: неровность только в радиусе, стены не лезут на дорогу
  for(let k=0;k<TRING;k++){const ang=k/TRING*6.2832,ix=(i*TRING+k)*3;
   tunV[ix]=cx+r*Math.cos(ang);tunV[ix+1]=cy+r*.92*Math.sin(ang);tunV[ix+2]=z;}
  const ri=i>>1;if(ri*2<NRK)for(let q=0;q<2;q++){const n=ri*2+q,h=((n*53)%29)/29,h2=((n*17)%11)/11;
   const ang=Math.PI*(.05+.9*h),rO=r+.25+h2*1.2; // валуны по верхней дуге и бокам, частично вросшие
   Q0.setFromAxisAngle(YAX,h*6.28);V1.set(cx+rO*Math.cos(ang),cy+rO*.92*Math.sin(ang)-.25,z);
   const sc=1.4+h2*2.1;V0.set(sc,sc*.85,sc);M4.compose(V1,Q0,V0);tunRidge.setMatrixAt(n,M4);}}
 tunG.attributes.position.needsUpdate=true;tunG.computeVertexNormals();
 tun.visible=true;tunRidge.visible=true;}
// ---------- collision ----------
const laneHit=o=>o.lane===S.lane&&Math.abs(S.x-(o.lane-1)*LANE)<LANE*.6;
function collide(dt){const h=S.roll>0?1.0:1.62, // хитбокс = измеренный клубок .90 + зазор
 feet=S.roll>0?S.rollY:S.y,top=feet+h; // под кувырком прогибаемся (как подкатом v4.34): bar закрывает щель, не ломая физику прыжка
 for(const o of S.obs){if(!laneHit(o))continue;
  const near=Math.abs(o.z)<(o.type==='wall'?1.35:.95); // валун круглый R1.15, а не вагон 4.2 длиной
  if(!near)continue;
  if(o.type==='low'&&feet<.7)return die();
  if(o.type==='bar'&&top>1.05)return die();
  if(o.type==='wall')return die();}
 for(const c of S.coinsArr){if(!c.taken&&Math.abs(c.z)<.9&&c.lane===S.lane&&Math.abs(S.y+.6-c.y)<.9){
  c.taken=true;scene.remove(c.m);S.coins++;sfx('pick');S.coinsArr=S.coinsArr.filter(x=>x!==c);}}
 updPickFx(dt);}
function die(){if(MJ&&MJ.rig){heroM.rotation.set(0,0,0);if(rollRoot){rollRoot.rotation.set(0,0,0);rollRoot.position.set(0,0,0);}const rt=MJ.rig.parent;if(rt.__by!==undefined)rt.position.y=rt.__by;}S.mode='over';S.dead=.001;S.dz=(Math.random()<.5?-1:1)*(.35+Math.random()*.3);S.shake=1;S.flash=.4;S.ts=.32;sfx('bump');
 S.best=Math.max(S.best,Math.floor(S.score));localStorage.setItem('nr_best',S.best);
 S.bestC=Math.max(S.bestC,S.coins);localStorage.setItem('nr_bestc',S.bestC);
 burst(S.x+bend(0),S.y+.8,0,0xF5F5F5,30);showOver();}
// ---------- input ----------
function move(d){const l=Math.max(0,Math.min(2,S.lane+d));if(l!==S.lane&&S.mode==='run'){S.stepPh=S.anim;S.stepT=5.5/S.speed;S.stepD=Math.sign(l-S.lane);}S.lane=l;}
function jump(){if(!S.air&&S.roll<=0){S.vy=JUMP_V;S.air=true;sfx('jump');}}
function doRoll(){if(S.roll>0)return;S.roll=.62;S.rollAir=S.air;if(S.air){S.rollY=S.y;S.rollV=Math.min(S.vy,-2);S.vy=0;sfx('jump');}else{S.rollY=0;S.rollV=0;}}
function act(a){if(window.dlog&&a!=='T'&&a!=='O')dlog.swipe();
 if(S.mode==='menu'){reset();showRun();if(window.dlog)dlog.run();return;}
 if(S.mode==='over'){if(S.dead>1.05)reset(),showRun();return;}
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
function showMenu(){$('menu').style.display='flex';$('over').style.display='none';$('hud').style.display='none';$('meta').style.display='none';
 let v=$('ver');if(!v){v=document.createElement('div');v.id='ver';v.style.cssText='position:fixed;bottom:6px;left:0;right:0;text-align:center;font-size:11px;opacity:.5';document.body.appendChild(v);}
 v.style.display='block';v.textContent='v'+VER+' · '+(mReady?'GLB':'FALLBACK');}
function showRun(){$('menu').style.display='none';$('over').style.display='none';$('hud').style.display='flex';$('meta').style.display='block';if($('ver'))$('ver').style.display='none';}
function showOver(){$('over').style.display='flex';$('hud').style.display='none';
 $('ostats').textContent=fmt(Math.floor(S.score))+' · ⚔ '+S.coins+' · '+Math.floor(S.dist)+'m';
 $('obest').textContent='РЕКОРД '+fmt(S.best)+' · ⚔ '+S.bestC;
 setTimeout(()=>{$('ogo').style.visibility='visible';},800);}
showMenu();
// ---------- update ----------
const M4=new THREE.Matrix4(),Q0=new THREE.Quaternion(),V1=new THREE.Vector3(),V0=new THREE.Vector3(1,1,1);
function updInst(){
 bendStrip(ground);bendStrip(path);
 for(let i=0;i<10;i++){const c=cloudPos[i],z=-260+(((c.z+S.dist*.35)%320)+320)%320;
  for(let k=0;k<3;k++){const f=CLOUD_P[k];
   V1.set(c.x+f[0]*c.s,c.y+f[1]*c.s,z+f[2]*c.s*.5);V0.set(c.s*.6*f[2],c.s*.32*f[2],c.s*.45*f[2]);
   M4.compose(V1,Q0.identity(),V0);clouds.setMatrixAt(i*3+k,M4);}}
 clouds.instanceMatrix.needsUpdate=true;
 {for(let i=0;i<NMtn;i++){const v=mtnPos[i],z=-560+(((v.z+S.dist*.2)%680)+680)%680; // горы: самый дальний слой
  Q0.setFromAxisAngle(YAX,v.ry);V1.set(v.x,v.h/2,z);V0.set(v.r,v.h,v.r);M4.compose(V1,Q0,V0);mtnMesh.setMatrixAt(i,M4);}
  mtnMesh.instanceMatrix.needsUpdate=true;}
 {const AXY=new THREE.Vector3(0,1,0); // дальние поселения: параллакс .55 (медленнее деревьев - дальше), цикл 340
  for(let i=0;i<18;i++){const v=housePos[i],z=-300+(((v.z+S.dist*.55)%340)+340)%340;
   Q0.setFromAxisAngle(AXY,v.ry);V1.set(v.x,v.h/2,z);V0.set(v.w,v.h,v.w);M4.compose(V1,Q0,V0);houseBody.setMatrixAt(i,M4);
   V1.y=v.h+v.rh/2;V0.set(v.w*1.25,v.rh,v.w*1.25);M4.compose(V1,Q0,V0);houseRoof.setMatrixAt(i,M4);
   for(let k=0;k<2;k++){const ox=-Math.sign(v.x)*v.w*.52,oz=(k-.5)*v.w*.5,ca=Math.cos(v.ry),sa=Math.sin(v.ry);
    Q0.setFromAxisAngle(AXY,v.ry);V1.set(v.x+ox*ca+oz*sa,v.h*.55,z-ox*sa+oz*ca);
    V0.set(v.w*.2,v.h*.28,1);M4.compose(V1,Q0,V0);houseWin.setMatrixAt(i*2+k,M4);}}
  for(let i=0;i<6;i++){const v=towerPos[i],z=-300+(((v.z+S.dist*.55)%340)+340)%340;
   Q0.identity();V1.set(v.x,v.h/2,z);V0.set(v.w,v.h,v.w);M4.compose(V1,Q0,V0);towerMesh.setMatrixAt(i,M4);
   V1.y=v.h+v.w*.55;M4.compose(V1,Q0.setFromAxisAngle(AXY,.5),V0.set(v.w*1.15,v.w*1.1,v.w*1.15));towerTop.setMatrixAt(i,M4);
   const apex=v.h+v.w*1.1; // флагшток из-под верхушки шатра
   V1.set(v.x,apex+.7,z);V0.set(.07,1.4,.07);M4.compose(V1,Q0.identity(),V0);poleMesh.setMatrixAt(i,M4);
   V1.set(v.x+.48,apex+1.15,z);V0.set(.9,.5,.06);M4.compose(V1,Q0.identity(),V0);flagMesh.setMatrixAt(i,M4);}
  for(let i=0;i<4;i++){const v=wallPos[i],z=-300+(((v.z+S.dist*.55)%340)+340)%340;
   V1.set(v.x,v.h/2,z);V0.set(v.w,v.h,1);M4.compose(V1,Q0.identity(),V0);castleWall.setMatrixAt(i,M4);
   for(let k=0;k<5;k++){V1.set(v.x+(k-2)*v.w/5,v.h+.16,z);V0.set(v.w/9,.32,.55);M4.compose(V1,Q0,V0);merlon.setMatrixAt(i*5+k,M4);}}
  for(let i=0;i<NHay;i++){const v=hayPos[i],z=-300+(((v.z+S.dist*.55)%340)+340)%340;
   Q0.identity();V1.set(v.x,v.h/2,z);V0.set(v.w,v.h,v.w);M4.compose(V1,Q0,V0);hayMesh.setMatrixAt(i,M4);}
  for(let i=0;i<NFen;i++){const v=fencePos[i],z=-300+(((v.z+S.dist*.55)%340)+340)%340;
   Q0.identity();V1.set(v.x,.45,z);V0.set(.13,.9,.13);M4.compose(V1,Q0,V0);fenceMesh.setMatrixAt(i,M4);}
  houseBody.instanceMatrix.needsUpdate=houseRoof.instanceMatrix.needsUpdate=true;
  hayMesh.instanceMatrix.needsUpdate=fenceMesh.instanceMatrix.needsUpdate=true;
  houseWin.instanceMatrix.needsUpdate=merlon.instanceMatrix.needsUpdate=true;
  poleMesh.instanceMatrix.needsUpdate=flagMesh.instanceMatrix.needsUpdate=true;
  towerMesh.instanceMatrix.needsUpdate=towerTop.instanceMatrix.needsUpdate=castleWall.instanceMatrix.needsUpdate=true;}
 for(let i=0;i<NTree;i++){const b=treePos[i],z=-260+(((b.z+S.dist*.85)%320)+320)%320;
  V1.set(b.x+bend(z),b.h/2,z);V0.set(b.w,b.h,b.w);M4.compose(V1,Q0,V0);trunks.setMatrixAt(i,M4);
  V1.set(b.x+bend(z),b.h+b.canopyH/2,z);V0.set(b.canopyW,b.canopyH,b.canopyW);M4.compose(V1,Q0,V0);canopies.setMatrixAt(i,M4);
  V1.set(b.x+bend(z)+b.canopyW*.35,.025,z);V0.set(b.canopyW*1.15,1,b.canopyW*1.15);M4.compose(V1,Q0,V0);treeSh.setMatrixAt(i,M4);}
 treeSh.instanceMatrix.needsUpdate=true;
 trunks.instanceMatrix.needsUpdate=true;canopies.instanceMatrix.needsUpdate=true;V0.set(1,1,1);
 for(let i=0;i<NStone;i++){const v=stonePos[i],z=-260+(((v.z+S.dist*.85)%320)+320)%320; // камни у дороги
  Q0.setFromAxisAngle(YAX,v.ry);V1.set(v.x+bend(z),v.w*.5,z);V0.set(v.w,v.w*.7,v.w);M4.compose(V1,Q0,V0);stoneMesh.setMatrixAt(i,M4);}
 stoneMesh.instanceMatrix.needsUpdate=true;Q0.identity();
 for(let i=0;i<NBush;i++){const b=bushPos[i],z=-200+(((b.z+S.dist)%220)+220)%220;
  V1.set(b.x+bend(z),b.h/2,z);V0.set(.9,b.h,.9);M4.compose(V1,Q0,V0);bushes.setMatrixAt(i,M4);}
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
 S.speed=Math.min(44,18+S.dist/240);
 const dz=S.speed*dt;S.dist+=dz;S.score+=dz;
 dayFx(dt);
 S.stepT=Math.max(0,(S.stepT||0)-dt);
 S.x+=((S.lane-1)*LANE-S.x)*Math.min(1,dt*(S.stepT>0?9:14));
 if(S.roll<=0){S.vy-=GRAV*dt;S.y+=S.vy*dt;
 if(S.y<=0){if(S.air){burst(S.x,.1,0,0x6B4E2E,6);land=1;}S.y=0;S.vy=0;S.air=false;}}
 land=Math.max(0,land-dt*7); // сплющивание при приземлении гаснет за ~0.14с
 if(S.roll>0){ // кувырок: по земле катится на месте (ось постоянна); с воздуха - пикирование с приземлением
  if(S.rollAir){S.rollV-=GRAV*.9*dt;S.rollY+=S.rollV*dt;
   if(S.rollY<=0){burst(S.x,.05,.3,0x6B4E2E,10);S.shake=Math.max(S.shake,.25);tipV+=2.4;S.rollY=0;S.rollV=0;S.rollAir=false;S.air=false;S.y=0;if(S.roll>.16)S.roll=.16;} // долетает оборот за .16c, не замирая
   S.y=S.rollY;} // физика прыжка живёт в rollY до приземления
  if(Math.random()<dt*20&&S.rollY<.06)burst(S.x,.03,.2,0x6B4E2E,1);
  S.roll-=dt;if(S.roll<=0)S.roll=0;}
 else if(S.roll>0)S.roll-=dt;
 S.anim+=(S.air||S.roll>0)?4*dt:dz*(Math.PI*2/11); // цикл ног = 5.5 м грунта: 3.3 шага/с на 18 м/с, без «бегства на месте»
 for(const o of S.obs){
  const vz=dz+(o.type==='wall'?5.5*dt:0); // валун катится НАВСТРЕЧУ: свой ход поверх скролла сцены
  o.z+=vz;o.m.position.z=o.z;o.m.position.x=(o.lane-1)*LANE+bend(o.z);
  if(o.type==='wall')o.m.userData.spin.rotation.x+=vz/o.m.userData.R*.08;} // валун катится К игроку: верх камня едет в +z; спин ×.08 - на 30+ м/с честное вращение = мясорубка
 for(const c of S.coinsArr){c.z+=dz;c.m.position.z=c.z;c.m.position.x=(c.lane-1)*LANE+bend(c.z);c.m.rotation.y+=dt*.9;} // медленное вращение - клинок можно разглядеть
 S.obs=S.obs.filter(o=>o.z<DESPAWN_Z||(scene.remove(o.m),false));
 S.coinsArr=S.coinsArr.filter(c=>c.z<DESPAWN_Z&&!c.taken||(scene.remove(c.m),false));
 if(S.dist>=nextSpawn)spawn();
 collide(dt);}
// ---------- rig + camera ----------
// ---------- secondary spring for hat tip ----------
var tipQ=0,tipV=0;
function springTip(target,dt){const k=42,d=7;tipV+=(target-tipQ)*k*dt-d*tipV*dt;tipQ+=tipV*dt;}
// пружинные суставы: углы догоняют цель через PD-регулятор -> связность, инерция и лёгкий перелёт куклы
function tgt(o,ax,v){(o.__t||(o.__t={}))[ax]=v;}
const SKIN=()=>[rig,head,armL,armR,armL.elbow,armR.elbow,legL,legR,legL.knee,legR.knee,legL.ankle,legR.ankle];
function springs(dt,k,d,list){for(const j of (list||SKIN())){const t=j.__t;if(!t)continue;const s=j.__s||(j.__s={});
 for(const ax in t){let st=s[ax];if(!st)st=s[ax]={v:0,c:j.rotation[ax]};
  st.v+=((t[ax]-st.c)*k-d*st.v)*dt;st.c+=st.v*dt;
  const cl=j.__cl||(j===rig?[-1.62,.35]:null); // корпус не проваливается сквозь землю при падении
  if(cl&&ax==='x')st.c=Math.max(cl[0],Math.min(cl[1],st.c));
  j.rotation[ax]=st.c;}}}
function allRelax(){for(const j of SKIN())for(const ax of['x','y','z'])if(j.__t)j.__t[ax]=0;}
function animRig(dt){
 springTip(0,dt);tip.rotation.x=-.55+tipQ;
 if(S.mode==='over'){ // рогдолл смерти: суставы limp, гравитация роняет ничком, конечности болтаются
  if(S.y>0){S.vy-=GRAV*dt;S.y+=S.vy*dt;if(S.y<0){S.y=0;S.vy=Math.abs(S.vy)*.3;if(S.vy<.6)S.vy=0;}} // допадание с отскоком
  player.position.set(S.x,S.y+LIFT*.6,0);
  allRelax();
  tgt(rig,'x',-1.5);tgt(rig,'z',S.dz||.4); // ничком + вбок (сторона задана в die)
  tgt(armL,'x',2.1);tgt(armR,'x',1.7);tgt(armL,'z',1.1);tgt(armR,'z',-1.3); // руки вскинуты и врозь
  tgt(armL.elbow,'x',-1.6);tgt(armR.elbow,'x',-.9);
  tgt(legL,'x',-1.1);tgt(legR,'x',-.7);tgt(legL.knee,'x',1.4);tgt(legR.knee,'x',.9); // ноги подброшены
  tgt(head,'x',.7);tgt(head,'y',(Math.random()-.5)*.01); // голова мотыляется
  springs(dt,88,8.5); // мягко, с болтанкой
  return;}
 if(S.roll>0){const s=Math.min(1,Math.min(S.roll,.45-S.roll)*10); // плавный вход/выход позы
  player.scale.setScalar(HERO_K);
  player.position.set(S.x,S.y+LIFT*(1-s),0);
  tgt(rig,'x',-1.25*s);tgt(rig,'y',0);tgt(rig,'z',-.08*s);rig.position.y=.48*s;
  tgt(armL,'x',1.4*s);tgt(armR,'x',1.4*s);tgt(armL.elbow,'x',.6*s);tgt(armR.elbow,'x',.6*s);
  tgt(legL,'x',-1.2*s);tgt(legR,'x',-1.2*s);
  tgt(legL.knee,'x',1.2*s);tgt(legR.knee,'x',1.2*s);
  tgt(legL.ankle,'x',-.6*s);tgt(legR.ankle,'x',-.6*s);
  tgt(head,'x',-.4*s);tgt(head,'y',0);tgt(head,'z',0);
  springs(dt,300,27); // жёстко: подкат читается чётко
  tip.rotation.x=-.55-.9*s;tipV=0; // hat folds, kill spring impulse
  sword.rotation.x=.42+1.3*s;
  return;}
 player.scale.setScalar(HERO_K);
 player.position.set(S.x,S.y+LIFT,0);
 rig.position.y=0;sword.rotation.x=.42;sword.rotation.z=0;
 armL.wrist.rotation.x=armR.wrist.rotation.x=0;
 if(S.air){const up=S.vy>0; // фаза полёта: отрыв - группировка, снижение - распластка для приземления
  tgt(rig,'x',up?-.14:.08);
  tgt(armL,'x',up?-2.3:-1.1);tgt(armR,'x',up?-2.3:-1.1);tgt(armL,'z',.15);tgt(armR,'z',-.15);
  tgt(armL.elbow,'x',up?-.9:-.4);tgt(armR.elbow,'x',up?-.9:-.4);
  tgt(legL,'x',up?-.9:-.1);tgt(legR,'x',up?-.2:.5);
  tgt(legL.knee,'x',up?1.5:.15);tgt(legR.knee,'x',up?.4:.1);
  tgt(legL.ankle,'x',up?.5:-.2);tgt(legR.ankle,'x',up?.5:-.2);
  tgt(head,'x',up?.1:-.05);
  tipV+=Math.sin(S.t*9)*(up?.9:-1.1)*dt*22;
  springs(dt,150,15); // средняя жёсткость: в полёте кукла чуть болтается
  return;}
 // ---------- procedural ground-run gait: 4-phase cycle with FK chain, anticipation and follow-through ----------
 const a=S.anim*2, sw=Math.sin(a), cw=Math.cos(a), sq=1-.13*land;
 const spd=Math.min(1,(S.speed-26)/26); // ускорение с набором скорости
 const amp=.95+spd*.25; // амплитуда растёт со скоростью
 const legPh=(ph)=>{ // фазы ноги: swing (мах вперёд), contact (контакт), drive (отталкивание), lift (подъём)
  const s0=Math.sin(a+ph), s1=Math.sin(a+ph+Math.PI*.5);
  const thigh=-amp*s0*.55; // бедро: максимальный вымах при входе
  const knee=Math.max(0,.75*(.5-.5*s1)+.2); // колено: поджимается на махе, разгибается на контакте
  const ankle=.2*s1; // стопа: тыльное сгибание на махе, плантарное на отталкивании
  return {thigh,knee,ankle};};
 const legLp=legPh(0), legRp=legPh(Math.PI);
 const armPh=(ph)=>{const s0=Math.sin(a+ph);return {shoulder:-s0*.85-.1,elbow:-.35-.3*Math.max(0,s0)};};
 const armLp=armPh(Math.PI), armRp=armPh(0); // руки в противофазе с ногами
 player.scale.set(HERO_K*(1+.09*land),HERO_K*sq,HERO_K*(1+.09*land));
 player.position.y=S.y+LIFT*sq;
 // body: forward lean, pelvic drop/roll, breathing
 tgt(rig,'x',-.12-spd*.04+Math.sin(a*2+Math.PI*.2)*.03); // наклон вперёд + дыхательный цикл
 rig.position.y=-.02+Math.abs(sw)*.045+Math.sin(a)*.018; // вертикальный осциллятор таза + перенос веса
 tgt(rig,'z',Math.sin(a)*.05); // боковой наклон таза
 tgt(rig,'y',Math.sin(a+Math.PI)*.07); // контр-вращение грудного отдела
 tgt(legL,'x',legLp.thigh);tgt(legL.knee,'x',legLp.knee);tgt(legL.ankle,'x',legLp.ankle);
 tgt(legR,'x',legRp.thigh);tgt(legR.knee,'x',legRp.knee);tgt(legR.ankle,'x',legRp.ankle);
 tgt(armL,'x',armLp.shoulder);tgt(armR,'x',armRp.shoulder);
 tgt(armL.elbow,'x',armLp.elbow);tgt(armR.elbow,'x',armRp.elbow);
 tgt(armL,'z',.12-Math.max(0,armLp.shoulder)*.08);tgt(armR,'z',-.12+Math.max(0,armRp.shoulder)*.08);
 armL.wrist.rotation.x=.1;armR.wrist.rotation.x=.1;
 tgt(head,'z',-Math.sin(a)*.03);tgt(head,'y',-Math.sin(a+Math.PI)*.09+Math.sin(S.t*.8)*.06);
 tgt(head,'x',.04+Math.sin(a*2+Math.PI*.3)*.025);
 tipV+=Math.sin(a*2+Math.PI*.3)*dt*24; // импульс инерции шапки от ускорения головы
 springs(dt,170,15.5); // недокритичное демпфирование: суставы догоняют с живым перелётом
 // sword: gentle sway with thoracic rotation
 sword.rotation.z=Math.sin(a+Math.PI)*.04;
}
let camX=0,camF=70,land=0;
function animCam(dt){
 camX+=(S.x*.55-camX)*Math.min(1,dt*6);
 camera.position.set(camX+(Math.random()-.5)*.5*S.shake,2.45+S.y*.22+(Math.random()-.5)*.5*S.shake,7.1);
 camera.lookAt(S.x*.7,1.05,-9); // масштаб героя .78 -> центр кадра ниже
 camera.rotation.z+=(S.x*.55-camera.position.x)*.04-camera.rotation.z;
 const tf=70+(S.speed-26)*.5;camF+=(tf-camF)*Math.min(1,dt*3);
 if(Math.abs(camera.fov-camF)>.05){camera.fov=camF;camera.updateProjectionMatrix();}}
 // ---------- road curve: редкие широкие дуги; изгиб = смещение x по глубине ----------
 const curveAt=p=>Math.sin(p*.008)*2.2+Math.sin(p*.0035+1.3)*3; // длины волн ~800/1800 м - тропa виляет нечасто
 const bend=z=>curveAt(S.dist+z)-curveAt(S.dist); // у игрока (z=0) равно 0
 function bendStrip(m){ // m.rotation.x=-PI/2: локальный y плоскости = -world z
  const a=m.geometry.attributes.position;
  for(let i=0;i<a.count;i++)a.setX(i,m._bx[i]+bend(-a.getY(i)+m.position.z));
  a.needsUpdate=true;}
 {ground.geometry.dispose();ground.geometry=new THREE.PlaneGeometry(300,320,1,80); // без rotateX: поворот остаётся на mesh
  const a=ground.geometry.attributes.position;ground._bx=new Float32Array(a.count);
  for(let i=0;i<a.count;i++)ground._bx[i]=a.getX(i);}
 // тропa = непрерывная земляная полоса поверх травы, шпал нет
 const path=new THREE.Mesh(new THREE.PlaneGeometry(LANE*3+.6,320,1,80),M(GR,.95));
 path.rotation.x=-Math.PI/2;path.position.set(0,-.01,-110);scene.add(path);
 {const a=path.geometry.attributes.position;path._bx=new Float32Array(a.count);
  for(let i=0;i<a.count;i++)path._bx[i]=a.getX(i);} // x до изгиба
addEventListener('resize',()=>{renderer.setSize(innerWidth,innerHeight);
 camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();});
// ---------- loop ----------
let last=performance.now();
function loop(now){const dt=Math.min(.05,(now-last)/1000);last=now;
 update(dt*S.ts);updInst();if(mReady)driveHero(dt);else animRig(dt);animCam(dt);
 $('score').textContent=fmt(Math.floor(S.score));
 $('coins').textContent='⚔ '+S.coins;
 $('best').textContent=fmt(S.best);
 $('meta').textContent=Math.floor(S.dist)+' м \u00b7 '+Math.floor(S.speed)+' км/ч';
 renderer.render(scene,camera);
 requestAnimationFrame(loop);}
requestAnimationFrame(loop);