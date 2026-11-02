// Neon Runner -> Subway-style runner, day palette, no bloom. three.js r147 UMD.
'use strict';
const SKY=0x4FB8F5, HZ=0xBFE6FA, GR=0x8A7A66, GO=0xFFD24A, OR=0xFF8C42;let SKYDOME=null;
const LANE=1.725, GRAV=26, JUMP_V=8.6, SPAWN_Z=-70, DESPAWN_Z=6, TAU=Math.PI*2; // LANE x1.5: тропа - основная игровая зона
const $=id=>document.getElementById(id);
let renderer,scene,camera;
try{
  renderer=new THREE.WebGLRenderer({canvas:$('c'),antialias:false,powerPreference:'high-performance',preserveDrawingBuffer:true}); // keep buffer: toDataURL shots + Android capture
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
// камни только за тропой: |x| >= LANE*1.7+0.6 иначе катятся по полосам вместе с игроком
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
// ---------- трава: пучки из трёх травяных лезвий (пирамидки), не кубики ----------
const bushM=M(0x3E8A26,.9);bushM.flatShading=true;
const bladeG=new THREE.ConeGeometry(.09,.85,3); // узкая пирамида-травинка
{const grp=new THREE.Group();
 for(let q=0;q<3;q++){const m=new THREE.Mesh(bladeG);m.position.set((Math.random()-.5)*.28,.42,(Math.random()-.5)*.2);m.rotation.set((Math.random()-.5)*.5,q*2.1,(Math.random()-.5)*.35);grp.add(m);}
 // слить в одну геометрию
 const g=new THREE.BufferGeometry();const pos=[],nor=[],idx=[];let base=0;
 grp.updateMatrixWorld(true);
 grp.children.forEach(m=>{m.updateMatrix();const pg=m.geometry.attributes.position,ng=m.geometry.attributes.normal;
  const v=new THREE.Vector3(),nn=new THREE.Vector3();const nm=new THREE.Matrix3().setFromMatrix4(m.matrix);
  for(let i=0;i<pg.count;i++){pos.push(v.fromBufferAttribute(pg,i).applyMatrix4(m.matrix).x,v.y,v.z);nor.push(nn.fromBufferAttribute(ng,i).applyMatrix3(nm).normalize().x,nn.y,nn.z);}
  const ix=m.geometry.index;for(let i=0;i<ix.count;i++)idx.push(base+ix.getX(i));base+=pg.count;});
 g.setAttribute('position',new THREE.Float32BufferAttribute(pos,3));
 g.setAttribute('normal',new THREE.Float32BufferAttribute(nor,3));g.setIndex(idx);
 var bushG=g;}
const NBush=140, bushes=new THREE.InstancedMesh(bushG,bushM,NBush), bushPos=[];bushes.frustumCulled=false; // пучки гуляют за bounds - не отсекать пачкой
for(let i=0;i<NBush;i++){const s=Math.random()<.5?-1:1;
 bushPos.push({x:s*(LANE*1.6+.35+Math.random()*.5),z:Math.random()*220,h:.5+Math.random()*.45,ry:Math.random()*6.28,sw:Math.random()*6.28});}
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
// хлопок тетивы: синтез one-shot (WebAudio), звуковых файлов в APK нет
let actx;function twang(){try{actx=actx||new(window.AudioContext||window.webkitAudioContext)();if(actx.state==='suspended')actx.resume();
 const t=actx.currentTime,o1=actx.createOscillator(),o2=actx.createOscillator(),g=actx.createGain();
 o1.type='triangle';o1.frequency.setValueAtTime(420,t);o1.frequency.exponentialRampToValueAtTime(95,t+.09); // щипок струны с падением
 o2.type='square';o2.frequency.value=60; // низкочастотный шлепок
 const g2=actx.createGain();g2.gain.setValueAtTime(.18,t);g2.gain.exponentialRampToValueAtTime(.001,t+.04); // .square глуше и короче
 o1.connect(g);o2.connect(g2);g.connect(g2).connect(actx.destination); // цепочка: оба сигнала сливаются в один огибающий выход
 g.gain.setValueAtTime(.5,t);g.gain.exponentialRampToValueAtTime(.001,t+.13);
 o1.start(t);o2.start(t);o1.stop(t+.15);o2.stop(t+.15);}catch(e){}}
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
const VER='4.46.2';
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
  heroBow=makeBow(); heroBow.position.set(0,-.44,.06); heroBow.scale.setScalar(.8); MJ.elbowL.add(heroBow); // grip хвастается empirically: world-точка grip совпала с distal концом предплечья (z -.53 vs BB -.51) // лук в КИСТИ: кисть = distal-конец предплечья elbowL (local -y ~.30); ориентация - в driveHero
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
 if(S.mode==='over'&&(heroM.__tuck||heroM.__piv)){heroM.__tuck=false;heroM.__piv=null;heroM.__shell=null;heroM.__rest=null;if(rollRoot)rollRoot.position.set(0,0,0);root.position.set(0,0,0);root.rotation.set(0,0,0);} // deathMidRoll: __t обратно пружинам, иначе limp-ветка пишет Т в мёртвые __t (см. ниже), а roll-ветка их перетирает CL => limp не работает = «хвост улетает в палку»
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
 if(S.roll===0&&(heroM.__piv||heroM.__tuck||heroM.__shell)){heroM.__piv=null;heroM.__shell=null;heroM.__tuck=false;heroM.__rest=null;if(rollRoot){rollRoot.position.set(0,0,0);rollRoot.rotation.set(0,0,0);}root.position.set(0,0,0);root.rotation.set(0,0,0);heroM.scale.set(1,1,1);if(heroBow)heroBow.visible=true; // scale-возврат: roll-ветка писала scale .84, ран-ветка scale НЕ трогает => без возврата герой навсегда оставался на 16% меньше с парящими над дорогой ногами (виз-аудит post-exit)
   const runT={rig:{x:.12,z:0},head:{x:.1,z:0},hat:{x:.05,z:0},armL:{x:-.35,z:.12},armR:{x:-.2,z:-.1},elbowL:{x:-.7},elbowR:{x:-.55},legL:{x:0},legR:{x:0},kneeL:{x:.1},kneeR:{x:.05},ankleL:{x:0},ankleR:{x:0}}; // exit: __t = ран-цели (не нули): relax-пружина тащит из club в бег; шаг-цикл ниже перетирает ноги как обычно
   for(const b in runT){const o=j[b];if(!o)continue;o.__t||(o.__t={});for(const ax in runT[b])o.__t[ax]=runT[b][ax];}}
 if(S.roll>0){ // КУВЫРОК v4.44.4 (ground-contact): КАТИМЫЙ клубок = видимый bbox собранной club-позы; ЦЕНТР дуги в heroM поднят так, чтобы НИЖНЯЯ точка дуги легла на G=-0.52 (уровень ступней бега = асфальт в heroM-рамке, world -0.01): heroY = G - bbox.min.y + bbox.cy, инвариантно к углу вращения. Раньше центр поднимался по вписанному шару (cy=-0.18 r=0.73 club-позы: центр дуги на G+r=0.21 при bbox.cy=-0.18 => дуга ушла на -0.92, ступня протыкала дорогу) = «провал». Расплетение вшито в хвост последнего оборота (обратная рампа tk), tk=0 = чистая поза/позиция бега = непрерывные вход/выход (обрыв позы по таймеру = «телепорт на полкорпуса»). Лук на время кувырка скрыт (дуга торчала из клуба)
  const p=1-S.roll/.62,base=(S.rollAir?Math.max(0,S.rollY):0);
    const tk=Math.min(1,p*4,Math.max(0,(1-p)/.28)), q=Math.min(1,Math.max(0,(p-.08)/.6)), x=q*Math.PI*2; // расплетение .72->1.0 внутри вращения (q=1 с .68): подъём/ось/поза снимаются синхронно с последним полуоборотом; tk=0 ровно при p=1 => exit-кадр = ран-база (bbox не прыгает); последняя 1/8 = шаги по земле
  const CL={rig:{x:1.4},head:{x:1.2},hat:{x:-3.2,z:.12},armL:{x:-2.45,z:.5},armR:{x:-2.35,z:-.5},elbowL:{x:2.15},elbowR:{x:2.1},legL:{x:-2.9},legR:{x:-2.8},kneeL:{x:2.9},kneeR:{x:2.85},ankleL:{x:-1.3},ankleR:{x:-1.2}};
  if(!heroM.__tuck){heroM.__tuck=true; // ПЕРЕЗАХВАТ на старте: resting-рог (rig x.8/z.4 + .1 lift) в roll-ветке НЕ действовал (club писал абсолютку), но метрика и хвост жили с ним => центр/низ гуляли. Снапшот resting-позы = база бленда rest+(club-rest)*tk, метрика и x-ось считаются на НЕЙ => вход/выход непрерывны
    heroM.__rest={};for(const b in CL){const o=j[b];if(!o)continue;heroM.__rest[b]={x:o.rotation.x,z:o.rotation.z};}
    heroM.__restLift=j.rig.position.y;
    for(const b in CL){const o=j[b];if(!o)continue;o.__t||(o.__t={});const ss=o.__s||(o.__s={});
      for(const ax in CL[b]){o.__t[ax]=CL[b][ax];const st=ss[ax]||(ss[ax]={v:0,c:o.rotation[ax]});st.v=0;}}
    heroM.__piv=null;}

  const RS=heroM.__rest||{};
  const C=(b,ax,v)=>{const o=j[b],r=(RS[b]||{x:0,z:0})[ax]||0;o.rotation[ax]=r+(v-r)*tk;if(tk>=1)o.rotation[ax]=v;}; // club-абсолютка при tk=1 (HEAD-эталон). rest-примесь давала ран-остатки (rig.x .14 swing) поверх собранного клуба => скелет на 5-8° мимо валидированной позы => метрика и live-bbox ехали, низ гулял ±0.4; tk=0 всё ещё resting-рог = ран-база (вход непрерывен)
  C('rig','x',1.4);j.rig.position.y=(heroM.__restLift||.1)*(1-tk);
  C('head','x',1.2);
  C('hat','x',-3.2);C('hat','z',.12);
  C('armL','x',-2.45);C('armR','x',-2.35);
  C('elbowL','x',2.15);C('elbowR','x',2.1);
  C('armL','z',.5);C('armR','z',-.5);
  C('legL','x',-2.9);C('legR','x',-2.8);
  C('kneeL','x',2.9);C('kneeR','x',2.85);
  C('ankleL','x',-1.3);C('ankleR','x',-1.2);
  if(heroBow)heroBow.visible=false; // в кувырке лук прячем (иначе дуга лука торчит из клубка = «палка из клубка», bbox +0.85)
  heroM.position.set(S.x,S.rollY,0); // без x-смещения: раньше корень ролла сдвигался на метрический пивот центра клубка = «уход влево» при кувырке; клубок центрируется позой
  heroM.scale.set(1-(1-.84)*tk,1,1-(1-.84)*tk); // tuck scale .84 только X/Z: y-scale squash сплющивал ран-позу после выхода (exit scale .92 при ран-росте 1.8 = «коротышка с парящими ногами», viz post-exit)
  root.rotation.set(0,0,0); root.scale.set(1,1,1); root.position.set(0,0,0);
  if(!heroM.__lb)heroM.__lb=new THREE.Box3();
  const visB=(()=>{const bb=new THREE.Box3(),pt=new THREE.Vector3();return()=>{ // ТОЧНЫЕ углы geometry.boundingBox*matrixWorld. expandByObject на ПОВЁРНУТЫХ мешах строит AABB-вокруг-AABB: на 45° завышал bbox на ±0.25 => метрика self-referential (низ дуги гулял ±0.5 = «висит/топится»)
    heroM.__lb.makeEmpty();heroM.traverse(c=>{if(c.isMesh&&(!heroBow||c!==heroBow)){let q=c,v=true;while(q){if(!q.visible){v=false;break;}q=q.parent;}if(!v||!c.geometry)return;
      if(!c.geometry.boundingBox)c.geometry.computeBoundingBox();bb.copy(c.geometry.boundingBox);
      for(let i=0;i<8;i++){pt.set(i&1?bb.max.x:bb.min.x,i&2?bb.max.y:bb.min.y,i&4?bb.max.z:bb.min.z).applyMatrix4(c.matrixWorld);heroM.__lb.expandByPoint(pt);}}});};})();
  const G=-.02; // ЗЕМЛЯ heroM-рамки = верх асфальта (road plane -0.01..0; низ bbox в ран-позе = ботинки -0.51 проваливаются под плоскость дороги = артефакт GLB-посадки; раньше G=-0.52 по ступням = провал клубка)
    // КАТИМЫЙ клубок (v4.45.2): dуга КАЧЕНИЯ вокруг точки касания дороги (не вокруг origin скелета — тот на 0.58 выше центра = «непонятная далёкая ось», и не вокруг вписанного шара — club-поза не шар, низ гулял +0.5..-0.44 = «висит/тонет»). heroM поднят так, чтобы центр клуба был на G+r; pivot T = P - R*P (P = live-центр) => world-центр инвариантен к углу при tk=1, нижняя точка инвариантна на G; tk=0 => T=0 и heroM на базе = непрерывные вход/выход
  // метрика при heroM.y=0 (HEAD-эталон): world-центр = heroM.y + cy (root=0 при tk-записях) => подъём G+r-cy кладёт НИЖНЮЮ точку видимого bbox ровно на G, без live-петли
  const baseY=heroM.position.y; // ран-позиция — точка непрерывности входа (ран-ветка ставит герой на baseY)
  // === КАТИМЫЙ клубок v4.45.2 (окончательно): три ошибки исправлены по очереди: (1) метрика зависела от собственного угла => меряем при rollRoot.rotation=0; (2) pivot писался в root при root==rollRoot (цикл add) => пишем в rollRoot; (3) pivot вращается ВНУТРИ scale heroM => world = heroM.y + SC*(T+R·p): sup из world-box уже со scale, но T/оффсеты надо делить на SC при переводе в world; ниже sup используется в world-единицах напрямую
  const SC=heroM.scale.x;
  heroM.position.y=0;
  rollRoot.rotation.set(0,0,0);rollRoot.position.set(0,0,0);root.rotation.set(0,0,0);heroM.updateMatrixWorld(true);visB(); // world-box текущей tk-смешанной позы (scale включён, heroM на 0)
  const y0=heroM.__lb.min.y,y1=heroM.__lb.max.y,z0=heroM.__lb.min.z,z1=heroM.__lb.max.z;
  const cy=(y0+y1)/2,lz=(z0+z1)/2,lx=(heroM.__lb.min.x+heroM.__lb.max.x)/2-heroM.position.x; // x-пивот ОТНОСИТЕЛЬНО героя: world-lx таил смещение полосы => T=(-lx·cos) уносил клубок с дороги на обочину при боковых кувырках (телепорт в лес)
  const a=-x,ca=Math.cos(a),sa=Math.sin(a);
  // ЭМПИРИЧЕСКАЯ ОБОЛОЧКА (AABB-углы врут: meshes не заполняют коробку; const-radius AABB давал «горб» +0.55): точка = среднее облако мешей позной рамки (world при hy=0). Вращаем точки, берём макс(-y') = support фазы x. Собирается в тот же кадр, что метрика; позa tk-смешанная => envelope тоже
  // === ФИКСИРОВАННАЯ ОБОЛОЧКА СОБРАННОГО КЛУБКА (tk=1): измерить ОДИН РАЗ облако world-точек (hy=0, цепь чиста) => центр (cy,lz) + список угловых точек (y,cy)-x-(z,lz). Поза при tk=1 детерминирована (springs заморожены, club-абсолютка) => оболочка честная на всю club-фазу. tk-рампа смешивает resting-низ и club-огибающую плавно
  // === МЕХАНИКА КОЛЕСА: центр качения = метрический центр AABB позы (cy,lz); радиус = max‖p-c‖ по облаку. Позиция: heroM на (G+r) при tk=1, resting baseY при tk=0 (линейный микс). ОСТОРОЖНО: pivot-T гасит вращение центра — при r=const центр (G+r) НЕ двигается, вращается тело вокруг центра (как колесо на оси) — «ось далеко» уйдёт: ось = центр, который ровно в (G+r) на уровне середины клубка (не скелет-торс выше)
  // === ФИКСИРОВАННАЯ ОБОЛОЧКА СОБРАННОГО КЛУБКА (club-фаза детерминирована: springs заморожены, club-абсолютка) — меряется ОДИН РАЗ на входе tk>=1, используется на всей club-фазе. Живой AABB повёрнутой позы НЕ годится: expandByObject на повернутых мешах = AABB-вокруг-AABB, завышает низ на ±0.35 на 45° = «топорщится/тонет» по фазам. Фиксированная огибающая из НЕвращённых 8-углов +OUR OWN exact rotation = точная нижняя грань повёрнутого облака на любом угле (тот же источник углов, что у live-bbox)
  let sh=heroM.__shell;
  if(!sh&&tk>=1){
    // ОБЛАКО ВЕРШИН geometry (не 8 углов AABB): углы коробки пустотелы (меши не заполняют box), повёрнутые углы завышают низ на +0.15..0.2 на 45° = «висит». Вершины = реальная поверхность клубка
    const pts=[];const wp=new THREE.Vector3(),mm=new THREE.Matrix4();
    heroM.traverse(function(c){if(c.isMesh&&c!==heroBow){let q=c,vv=true;while(q){if(!q.visible){vv=false;break;}q=q.parent;}if(!vv||!c.geometry)return;
      mm.copy(rollRoot.matrixWorld).invert().premultiply(c.matrixWorld);const pa=c.geometry.attributes.position; // LOCAL-облако к rollRoot: world-точкам не хватало -S.x => центр качения u съезжал на -S.x·cos при боковых кувырках
      for(let i=0;i<pa.count;i++){wp.fromBufferAttribute(pa,i).applyMatrix4(mm);pts.push(wp.y,wp.z);}}});
    sh=heroM.__shell={pts:pts};
  }
  let low;
  if(sh){let lo=1e9;const cp=sh.pts;
    for(let i=0;i<cp.length;i+=2){const ry=(cp[i]-cy)*ca-(cp[i+1]-lz)*sa+cy;if(ry<lo)lo=ry;}
    low=lo;}
  else low=y0; // до сборки (фаза ввода tk<1): resting-микс ещё не club, club-огибающая к нему не относится; y0 = честный низ текущей позы
  heroM.position.y=baseY+((G-low)-baseY)*tk;
  // pivot: world_точка(p) = hy + SC·(T + R·p), p в rollRoot-локальных (= heroM-локальные до scale: world_box_при_нулевом_pivot = hy + SC·p => p = (world-координаты)/SC)
  const off={x:lx/SC,y:cy/SC,z:lz/SC};
  const Ry=off.y*ca-off.z*sa,Rz=off.y*sa+off.z*ca;
  rollRoot.position.set(off.x*(1-ca)*tk,(off.y-Ry)*tk,-Rz*tk); // T=(I-R)off => SC·(T+R·off)=SC·off — центр на месте; с углом — качение вокруг точки касания
  window.__dbg={p:p,tk:tk,x:x,hy:heroM.position.y,off:off,rp:[rollRoot.position.x,rollRoot.position.y,rollRoot.position.z],SC:SC,cy:cy,lz:lz};
  rollRoot.rotation.set(-x%(Math.PI*2),0,0); // угол на rollRoot (root==rollRoot — см. выше); %2PI: выход даёт x=2PI -> 0, иначе run-ветка гасила остаток одним кадром = «телепорт»
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
  if(!heroM.__tuck)springs(dt,105,11,ML); return;}
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
 if(heroBow){ // ЛУК ВСЕГДА В ЛЕВОЙ КИСТИ: idle-носка + фаза натяжения
 shotCd=Math.max(0,shotCd-dt);
 const wasDrawing=bowT>0;bowT=Math.max(0,bowT-dt);const b=wasDrawing?1-bowT/DRAW_T:0; // b 0->1 за DRAW_T, выпуск ровно на b=1
 if(wasDrawing&&bowT===0)releaseShot(); // выпуск в конце натяжения
 heroBow.visible=true;
 // ЛЕВАЯ (лучная): idle чуть согнута-вперёд (лук смотрит по бегом вперёд), при тяче выпрямляется в цель
 T(j.armL,'x',-1.5*b+alp.shoulder*(1-b)-.55*b*(0));T(j.armL,'z',-.18*b+(.12-Math.max(0,alp.shoulder)*.08)*(1-b));
 T(j.elbowL,'x',-.1*b+alp.elbow*(1-b)-.4*(1-b));
 heroBow.rotation.set(-(MJ.armL.rotation.x+MJ.elbowL.rotation.x),1.57,0);
 // арка/полочка: idle тетива натянута (b=0 -> nx=.14), при тяче полочка уходит к лицу
 for(const ss of bowStr){const sg=ss.userData.sg,nx=.14+.26*b,d=new THREE.Vector3(nx-(-.10),sg*.72,0);
  ss.scale.y=d.length()/ss.userData.L0;ss.quaternion.setFromUnitVectors(new THREE.Vector3(0,1,0),d.clone().normalize());
  ss.position.set((nx+(-.10))/2,sg*.36,0);}
 bowArr.visible=b>0;bowArr.position.set(.14-.5*(1-b),0,0); // стрела на тетиве появляется при натяжении
 // ПРАВАЯ: idle качается с бегом; при тяче локоть к корпусу, кисть идёт к уху; на релизе - резкий щелчок назад
 T(j.armR,'x',-1.5*b+arp.shoulder*(1-b));T(j.armR,'z',1.15*b+(-.12+Math.max(0,arp.shoulder)*.08)*(1-b));
 T(j.elbowR,'x',-1.35*b+arp.elbow*(1-b));
 if(b>0){ // idle-носка: правая свободна, слегка прикрывает лук у руки (не «пердит» вхолостую)
 }
}
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
// ---------- лук Link'а: появляется в вытянутой руке на выстрел ----------
function makeBow(){const g=new THREE.Group(); // локальные оси лука: y=высота дуги, x=нормаль плоскости (в цель), z=вдоль рукояти; в сцену ставится rotation.y=PI/2 (x->world -z)
 // рекурв-лук ломаной прямых сегментов (тора+струна читалась арбалетом). Силуэт в плоскости XY: концы загнуты к цели (-? x=нормаль) ... концы к +x, пузо от цели: выпуклость дуги ОТ цели, тетива вогнута к цели? НЕТ: тетива между концами, пузо смотрит В цель, тетива сзади.
 const wdM=M(0x5A3B22,.85); // тёмная вощёная древесина
 const UP=new THREE.Vector3(0,1,0);
 const P=[[.72,-.10],[.46,.04],[.20,.11],[0,.14]]; // кончик -> плечо -> пузо (y,x): пузо на +x (к цели: local x->world -z), концы загнуты на -x (рекурв, назад)
 const limb=(a,b)=>{const L=Math.hypot(b[0]-a[0],b[1]-a[1]);
  const m=new THREE.Mesh(new THREE.CylinderGeometry(.024,.024,L+.018,6),wdM);
  m.position.set((a[1]+b[1])/2,(a[0]+b[0])/2,0);
  m.quaternion.setFromUnitVectors(UP,new THREE.Vector3(b[1]-a[1],b[0]-a[0],0).normalize());g.add(m);};
 for(let k=0;k<P.length-1;k++){limb(P[k],P[k+1]);limb([-P[k][0],P[k][1]],[-P[k+1][0],P[k+1][1]]);}
 // тетива: полочка на рукояти (x=-.14+полка .02=-.12?) в покое прямые хорды кончик->полочка; тяча = полочка к лучнику (-? лучник за спиной: -x? x в цель => тяча на... пузо смотрит в цель (-x значит к цели). лучник сзади тетивы: тетива между концами (+x), пузо вперёд. полочка покоится у пуза? нет: тетива натянута между КОНЦАМИ (x=+.10), середина тетивы стоит на линии концы-концы x=+.10, а пузо x=-.14 выступает вперёд. полочка на тетиве в покое x=+.10; тяча: x растёт (к лучнику).
 const half=sg=>{const m=new THREE.Mesh(new THREE.CylinderGeometry(.008,.008,Math.hypot(.72,.24),4),M(0xE8E1CE,.6)); // кончик(±.72,-.10) -> полочка (0,+.14)
  m.position.set(.02,sg*.36,0);m.quaternion.setFromUnitVectors(UP,new THREE.Vector3(.24,sg*.72,0).normalize());
  m.userData={sg,L0:m.geometry.parameters.height};g.add(m);return m;};
 bowStr=[half(1),half(-1)];
 const grip=box(.05,.15,.05,wdM);grip.position.set(.14,0,0);g.add(grip); // рукоять на пузе
 const wrap=box(.045,.06,.05,M(0x2E5A24,.8));wrap.position.set(.14,.105,0);g.add(wrap); // зелёная обмотка-полочка
 const na=new THREE.Group();const sh=new THREE.Mesh(new THREE.CylinderGeometry(.014,.014,.7,5),M(0x6B4A2F,.9));sh.rotation.z=Math.PI/2;na.add(sh); // стрела вдоль x, летит -x
 const hd=new THREE.Mesh(new THREE.ConeGeometry(.03,.12,4),M(0xC4CBD2,.3,.8));hd.rotation.z=-Math.PI/2;hd.position.x=-.42;na.add(hd);
 const fm=new THREE.Mesh(new THREE.BoxGeometry(.14,.008,.1),M(0xC43A3A,.6));
 const f1=fm.clone();f1.position.x=.34;const f2=fm.clone();f2.position.x=.34;f2.rotation.x=1.57;na.add(f1);na.add(f2);
 na.position.set(-.05,0,0);na.visible=false;g.add(na);bowArr=na;
 return g;}
var heroBow=null,bowT=0,shotCd=0,bowStr=null,bowArr=null;
// лук цепляется к rig при готовности GLB (см. loader): heroBow=makeBow();rig.add(heroBow)
function tCoin(){ // sword pickup
 const g=new THREE.Group();
 const blade=box(.08,.7,.05,M(0xC4CBD2,.35,.75)); blade.position.y=.65;
 const guard=box(.22,.05,.05,M(0x6B3E2A,.85)); guard.position.y=.26;
 const grip=box(.06,.16,.06,M(0x6B3E2A,.85)); grip.position.y=.12;
 const pommel=new THREE.Mesh(new THREE.SphereGeometry(.05,8,6),M(0xFFD24A,.5,.2)); pommel.position.y=.04;
 g.add(blade,guard,grip,pommel); return g;}
// ---------- враги: Bokoblin (гоблин с дубиной), Magta (ящер-копьеносец в черепе), Phantom (красный фантом-капля) — тот же low-poly, что Link
const skinM2=M(0x9B6B4A,.8),bokuM=M(0x7B4A2F,.9),bokuDark=M(0x3E2415,.9),eyeM=M(0xFF3B30,.3),bokuHorn=M(0xD9CDB6,.8),magM=M(0x2E7A4A,.85),magDark=M(0x1A4A2A,.9),magicM=M(0x7A1225,.9),glowM=M(0xFF1A2E,.15),woodM=M(0x5B4632,.9);
function tMob(){ // Bokoblin: маленький горбун с рожками и дубиной
 const g=new THREE.Group();const H=1.15;
 const head=new THREE.Mesh(new THREE.BoxGeometry(.34,.26,.28),skinM2);head.position.y=H;head.rotation.x=.1;g.add(head);
 const snout=new THREE.Mesh(new THREE.ConeGeometry(.09,.2,4),skinM2);snout.rotation.x=-1.2;snout.position.set(0,H-.03,.18);g.add(snout);
 for(let i=0;i<2;i++){const h=new THREE.Mesh(new THREE.ConeGeometry(.05,.16,5),bokuHorn);h.position.set((i?.08:-.08),H+.14,-.06);h.rotation.set(-.45,i?.2:-.2,0);g.add(h);}
 const eL=new THREE.Mesh(new THREE.SphereGeometry(.04,6,4),eyeM);eL.position.set(.09,H+.02,.15);const eR=eL.clone();eR.position.x=-.09;g.add(eL,eR);
 const body=new THREE.Mesh(new THREE.BoxGeometry(.3,.4,.24),bokuM);body.position.y=H-.3;g.add(body);
 const loincloth=new THREE.Mesh(new THREE.BoxGeometry(.36,.14,.26),bokuDark);loincloth.position.y=H-.46;g.add(loincloth);
 const armL=new THREE.Mesh(new THREE.BoxGeometry(.09,.24,.09),skinM2);armL.position.set(-.21,H-.26,.04);const armR=armL.clone();armR.position.x=.21;g.add(armL,armR);
 const club=new THREE.Mesh(new THREE.CylinderGeometry(.06,.1,.52,6),woodM);club.position.set(.28,H-.2,.1);club.rotation.set(.45,0,-.25);
 const knot=new THREE.Mesh(new THREE.IcosahedronGeometry(.11,0),woodM);knot.position.set(.02,.3,0);club.add(knot);g.add(club);
 const legL=new THREE.Mesh(new THREE.BoxGeometry(.1,.3,.1),skinM2);legL.position.set(-.12,H-.58,0);const legR=legL.clone();legR.position.x=.12;g.add(legL,legR);
 const fL=new THREE.Mesh(new THREE.BoxGeometry(.1,.06,.12),bokuDark);fL.position.set(-.12,H-.72,.02);const fR=fL.clone();fR.position.x=.12;g.add(fL,fR);
 return g;}
function tBomb(){ // Magta: бронированный ящер с черепом-шлемом и копьем
 const g=new THREE.Group();const H=1.5;
 const skull=new THREE.Mesh(new THREE.IcosahedronGeometry(.24,0),M(0xE8E1CE,.9));skull.position.y=H;skull.scale.set(1,1.2,1);g.add(skull);
 const jaw=new THREE.Mesh(new THREE.BoxGeometry(.32,.06,.3),M(0xE8E1CE,.9));jaw.position.set(0,H-.24,.05);g.add(jaw);
 for(let i=0;i<4;i++){const t=new THREE.Mesh(new THREE.ConeGeometry(.03,.1,4),M(0xCFC7B0,.85));t.position.set((i%2?.08:-.08),H-.22,(i<2?.1:-.05));g.add(t);}
 const eL=new THREE.Mesh(new THREE.SphereGeometry(.045,6,4),eyeM);eL.position.set(.1,H+.03,.18);const eR=eL.clone();eR.position.x=-.1;g.add(eL,eR);
 const body=new THREE.Mesh(new THREE.BoxGeometry(.42,.52,.28),magM);body.position.y=H-.42;g.add(body);
 const shell=new THREE.Mesh(new THREE.IcosahedronGeometry(.31,0),magDark);shell.position.y=H-.3;shell.scale.set(.9,.7,.5);g.add(shell);
 const armL=new THREE.Mesh(new THREE.BoxGeometry(.1,.36,.1),magM);armL.position.set(-.25,H-.34,.04);const armR=armL.clone();armR.position.set(.25,H-.38,.08);g.add(armL,armR);
 const spear=new THREE.Mesh(new THREE.CylinderGeometry(.02,.025,.9,6),woodM);spear.position.set(.28,H-.2,.16);spear.rotation.x=-.5;
 const tip=new THREE.Mesh(new THREE.ConeGeometry(.05,.15,5),M(0xC4CBD2,.35,.75));tip.position.set(0,.48,0);spear.add(tip);g.add(spear);
 const tail=new THREE.Mesh(new THREE.ConeGeometry(.1,.5,5),magM);tail.rotation.x=1.4;tail.position.set(0,H-.5,-.14);g.add(tail);
 const legL=new THREE.Mesh(new THREE.BoxGeometry(.12,.4,.12),magDark);legL.position.set(-.14,H-.88,0);const legR=legL.clone();legR.position.x=.14;g.add(legL,legR);
 const fL=new THREE.Mesh(new THREE.BoxGeometry(.13,.07,.15),magDark);fL.position.set(-.14,H-1.08,.03);const fR=fL.clone();fR.position.x=.14;g.add(fL,fR);
 return g;}
function tBoss(){ // Phantom: красная капля-фантом с горящим глазом, быстрее других
 const g=new THREE.Group();const H=.85;
 const body=new THREE.Mesh(new THREE.ConeGeometry(.5,.9,6),magicM);body.position.y=H-.05;body.scale.set(.8,1,.7);g.add(body);
 const core=new THREE.Mesh(new THREE.IcosahedronGeometry(.34,0),glowM);core.position.y=H;g.add(core);
 for(let i=0;i<3;i++){const s=new THREE.Mesh(new THREE.ConeGeometry(.08,.36,4),magicM);const a=i/3*6.283+.4;
  s.position.set(Math.sin(a)*.4,H+.1,Math.cos(a)*.28);s.rotation.z=Math.sin(a)*.7;g.add(s);}
 const eye=new THREE.Mesh(new THREE.SphereGeometry(.1,8,6),M(0xFFEE88,.2));eye.position.set(0,H+.1,.32);g.add(eye);
 const pupil=new THREE.Mesh(new THREE.SphereGeometry(.045,6,5),M(0x110A05,.9));pupil.position.set(0,H+.1,.42);g.add(pupil);
 for(let i=0;i<3;i++){const sp=new THREE.Mesh(new THREE.ConeGeometry(.05,.18,5),magicM);
  sp.position.set((i-1)*.12,H+.4,-.06);sp.rotation.x=-.3;g.add(sp);}
 return g;}

function tCart(){ // povozka s tentom: zaprygivaemaya krysha, analog "poezda" Subway Surfers
 const g=new THREE.Group();const W=M(0x5B4632,.9);
 const bed=box(1.12,.14,3.0,W);bed.position.y=.66;g.add(bed);
 for(let i=0;i<2;i++){for(let j=0;j<2;j++){const ra=new THREE.Mesh(new THREE.CylinderGeometry(.42,.42,.06,10),W);ra.rotation.z=Math.PI/2;ra.position.set((i?1:-1)*.6,.42,(j?1:-1)*1.15); // 4 колеса
  for(let k=0;k<6;k++){const sp=new THREE.Mesh(new THREE.BoxGeometry(.05,.74,.04),W);sp.rotation.x=k*Math.PI/6;ra.add(sp);}
  const hub=new THREE.Mesh(new THREE.CylinderGeometry(.09,.09,.1,8),M(0x3A2A18,.9));hub.rotation.z=Math.PI/2;ra.add(hub);g.add(ra);}} // закрытие двойного цикла колёс
 const cv=box(1.06,.16,2.8,M(0x8A2E2E,.9));cv.position.y=1.0;g.add(cv);
 const cv2=box(1.12,.07,2.94,M(0x742323,.9));cv2.position.y=1.08;g.add(cv2);
 const rope=new THREE.Mesh(new THREE.CylinderGeometry(.018,.018,1.1,4),M(0xD8CBA8,.7));rope.rotation.z=Math.PI/2;rope.position.set(0,.92,1);g.add(rope);
 const rope2=rope.clone();rope2.position.z=-1;g.add(rope2);
 g.userData.len=3.6; // крыша 3.6м: вход за 2м до центра = край прыжковой дуги (hgt~1.35, дуга 2.85m)
 return g;}
function tHill(){ // pologyi zh-kholmik vershina .82: osnowanie-skirt zakryvaet styk s asfal'tom (ineache kupa "plavaet"), 3 klonka zernistosti dlja estestvennosti
 const g=new THREE.Group();const skirt=new THREE.Mesh(new THREE.CylinderGeometry(1.02,1.06,.34,14),M(0x6E8A3A,.95));
 skirt.position.y=.17;skirt.flatShading=true;g.add(skirt);
 const d=new THREE.Mesh(new THREE.SphereGeometry(1.3,14,7),M(0x8FB445,.95));
 d.scale.set(.64,.52,1.3);d.position.y=.12;d.flatShading=true;g.add(d);
 const d2=new THREE.Mesh(new THREE.SphereGeometry(.8,10,6),M(0xC0BE58,.95));
 d2.scale.set(.55,.5,.8);d2.position.set(.08,.5,-.1);d2.flatShading=true;g.add(d2);
 const d3=new THREE.Mesh(new THREE.SphereGeometry(.5,8,5),M(0xA9C24E,.95));
 d3.scale.set(.7,.42,.55);d3.position.set(-.14,.38,.55);d3.flatShading=true;g.add(d3);
 g.userData.len=3.2;
 return g;}
const TPL={low:tLow,bar:tBar,wall:tWall,mob:tMob,bomb:tBomb,boss:tBoss,cart:tCart,hill:tHill};
const PLAT={cart:1,hill:1};
// ---------- Фигурки врагов: hand-made GLB тем же китом, что и герой (enemies.glb); примитивные TPL = фолбэк ----------
let ENM=null,ENMROOT=null;
{const L=(typeof THREE.GLTFLoader==='function')&&new THREE.GLTFLoader();
 const ok=g=>{const roots={};g.scene.traverse(o=>{if(o.isMesh)o.frustumCulled=false;
   if(!o.isMesh)return;
   const nm=(o.name||'').replace(/_\d+$/,'');
   if((nm==='bokoblin'||nm==='magta'||nm==='phantom')&&!roots[nm])roots[nm]=o.parent||o;});
  // нормализация масштаба: как у героя - рост нормализуем по bbox группы-родителя
  g.scene.updateMatrixWorld(true);
  for(const k in roots){const rt=roots[k];const b=new THREE.Box3().setFromObject(rt);const H=b.max.y-b.min.y;
   if(H>0){rt.scale.setScalar(1.0/H);
    const b2=new THREE.Box3().setFromObject(rt);rt.position.y-=b2.min.y;}}
  g.scene.updateMatrixWorld(true);
  if(roots.bokoblin&&roots.magta&&roots.phantom){ENM=roots;ENMROOT=g.scene;}else ENM=null;};
 const bad=()=>{ENM=null;};
 const load=()=>{
  if(window.ENEMIES_GLB_B64){const bin=atob(window.ENEMIES_GLB_B64),u=new Uint8Array(bin.length);for(let i=0;i<bin.length;i++)u[i]=bin.charCodeAt(i);L.parse(u.buffer,'',ok,bad);}
  else L.load('../models/enemies.glb',ok,undefined,bad);};
 if(L)load();}
function enemyMesh(type){const key=type==='mob'?'bokoblin':type==='bomb'?'magta':type==='boss'?'phantom':null;
 if(!ENM||!ENM[key])return null;
 const grp=ENM[key];grp.updateWorldMatrix(true,true);
 // Инстанс: mesh'и группы, мировые трансформанты печём в локальную геометрию (gltf-цепочка занулена)
 const parMat=new THREE.Matrix4().copy(grp.matrixWorld).invert();
 const wrap=new THREE.Group();
 grp.traverse(o=>{if(!o.isMesh)return;
  const g2=o.geometry.clone();
  g2.applyMatrix4(new THREE.Matrix4().multiplyMatrices(parMat,o.matrixWorld));
  g2.computeVertexNormals();
  const mats=Array.isArray(o.material)?o.material:[o.material];
  const mm=mats.map(mt=>{const c=mt.clone();c.flatShading=true;c.needsUpdate=true;return c;}); // GLTFLoader уже привёл цвета к linear; повторный convert удваивал гамму (враги темнее мира)
  const cl=new THREE.Mesh(g2,mats.length>1?mm:mm[0]);
  cl.frustumCulled=false;wrap.add(cl);});
 // модель из glb уже лицом к +z (к камере) - wrap разворота не требует
 wrap.updateMatrixWorld(true);
 const bb=new THREE.Box3().setFromObject(wrap);
 wrap.userData.hgt=(bb.max.y-bb.min.y);
 wrap.userData.dip=bb.min.y;
 return wrap;}
const SHOOTABLE={low:1,mob:1,bomb:1,boss:1}; // cart/hill ne tsheli: po nim begut // валун (wall) стрелой не сбивается
// ---------- state ----------
const S={mode:'menu',t:0,dist:0,speed:26,coins:0,score:0,
 best:+(localStorage.getItem('nr_best')||0),bestC:+(localStorage.getItem('nr_bestc')||0),
 lane:1,x:0,y:0,vy:0,air:false,roll:0,anim:0,dead:0,shake:0,flash:0,ts:1,revive:0};
let nextSpawn=30; S.obs=[];S.coinsArr=[];S.arrows=[];S.arrowsMade=0;
function reset(){S.mode='run';S.t=0;S.dist=0;S.speed=18;S.coins=0;S.score=0;S.lane=1;S.x=0;S.y=0;S.vy=0;S.stepT=0;S.stepPh=null;
 S.air=false;S.roll=0;S.rollY=0;S.rollV=0;S.anim=0;S.dead=0;S.shake=0;S.flash=0;S.ts=1;
 for(const o of S.obs)scene.remove(o.m);for(const c of S.coinsArr)scene.remove(c.m);
 for(const a of S.arrows)scene.remove(a.m);
 if(arrowTrail)arrowTrail.visible=false; // иначе лента повиснет в воздухе после смерти (updArrows уже не догонит)
 bowT=0;shotCd=0; // незавершённая тяча не должна переезжать в новый забег (иначе первый тап проигнорирован до конца CD)
 S.obs=[];S.coinsArr=[];S.arrows=[];S.arrowsMade=0;nextSpawn=30;S.missed=0;deathPlayed=false;heartsP.length=0;}
function addObs(type,lane){const m=TPL[type]();m.position.set((lane-1)*LANE,0,SPAWN_Z);scene.add(m);
 const o={type,lane,z:SPAWN_Z,m};
 if(PLAT[type]){m.updateMatrixWorld(true);o.hgt=new THREE.Box3().setFromObject(m).max.y;o.len=m.userData.len;}
 const e=(type==='mob'||type==='bomb'||type==='boss')?enemyMesh(type):null;
 if(e){const want=type==='mob'?1.15:type==='bomb'?1.5:.95;
  const k=want/Math.max(.3,e.userData.hgt);
  e.scale.setScalar(k);
  m.traverse(c=>{if(c.isMesh)c.visible=false;}); // примитив-скелет не рисуем, но он носит позицию/полосу
  e.position.y=-e.userData.dip*k;  // встать на землю
  m.add(e); o.gl=e;
  // оживление: фаза и тип шага на обёртку (glAnim читает её из update)
  o.ph=Math.random()*6.283; o.walk=(type==='mob')?2.4:(type==='bomb')?1.7:0;}
 S.obs.push(o);}
function spawn(){
 const diff=Math.min(1,S.dist/3000), r=Math.random();
 const p=Math.random()<.45; // мечи не к каждому препятствию (частота снижена в ~1.5 раза)
 if(r<.12){ // бокoblin: идет НА героя в своей полосе (замедленный скролл - «живое» сближение)
  const l=(Math.random()*3)|0;addObs('mob',l);if(p&&Math.random()<.5)swordRun(l,3,SPAWN_Z-9,()=>.95);}
 else if(r<.2){ // Magta: бронированный, валун-череп - стреляй дважды
  const l=(Math.random()*3)|0;addObs('bomb',l);}
 else if(r<.27){ // Лик-нечисть: красное пятно, быстрее других
  const l=(Math.random()*3)|0;addObs('boss',l);}
 else if(r<.45){const l=(Math.random()*3)|0;addObs('low',l);
  if(p)swordRun(l,4,SPAWN_Z-3.3,i=>.95+Math.sin((i+.5)/4*Math.PI)*.8);} // арка над репьем - собирай в прыжке
 else if(r<.62){const l=(Math.random()*3)|0;addObs('bar',l);
  if(p)swordRun(l,3,SPAWN_Z-6.6,()=>.45);} // низкая линия за корнем - собирается в подкате
 else if(r<.73){const l=(Math.random()*3)|0; // povozka/holmik + linija mechey NA kryshe
  if(Math.random()<.5){addObs('cart',l);swordRun(l,3,SPAWN_Z+1.6,()=>1.3);}
  else{addObs('hill',l);swordRun(l,3,SPAWN_Z+1.4,()=>1.05);}}
 else if(r<.88){const bl=new Set();const n=diff>.4&&Math.random()<.5?2:1;   // валуны чаще
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
const stars=new THREE.InstancedMesh(starGeo,new THREE.MeshBasicMaterial({color:0xFFD879,transparent:true,fog:false,side:THREE.DoubleSide,depthWrite:false,blending:THREE.AdditiveBlending}),64);
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
  if(!p){V0.set(0,0,0);M4.compose(V1.set(0,-9999,0),Q0.identity(),V0);}
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
const NRK=56,tunRidge=new THREE.InstancedMesh(rockGeos[1],new THREE.MeshStandardMaterial({color:new THREE.Color(0x5d4d3e).convertSRGBToLinear(),roughness:.95,flatShading:true}),NRK); // ровно столько, сколько пишет цикл (28x2): лишние инстансы остались бы в identity-матрице = валун в (0,0,0) на тропе под героем
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
 {const k=Math.min(1,dt*2.8);const tx=lampX,ty=lampY,tz=lampZ,ti=on*4.5*envZ(lampZ);
  lampPL.position.set(lampPL.position.x+(tx-lampPL.position.x)*k,ty,lampPL.position.z+(tz-lampPL.position.z)*k);
  lampPL.intensity+=(ti-lampPL.intensity)*k;}
 // вторая лампа: следующий фонарь — чтобы тропы между ними не были пустыми
 lampPL2.__ti=0;
 for(let j=0;j<NL;j++){const b2=lanP[j],z=-150+(((b2.z+S.dist)%220)+220)%220;if(z<=lampZ-2&&z>lampZ-20){lampPL2.__tx=b2.x+bend(z)-Math.sign(b2.x)*.5;lampPL2.__tz=z;lampPL2.__ti=on*3.8*envZ(z);break;}}
 {const k=Math.min(1,dt*2.8);
  lampPL2.position.set(lampPL2.position.x+((lampPL2.__tx||lampPL2.position.x)-lampPL2.position.x)*k,2.2,lampPL2.position.z+((lampPL2.__tz!==undefined?lampPL2.__tz:lampPL2.position.z)-lampPL2.position.z)*k);
  lampPL2.intensity+=((lampPL2.__ti||0)-lampPL2.intensity)*k;}
 poles.instanceMatrix.needsUpdate=flame.instanceMatrix.needsUpdate=glow.instanceMatrix.needsUpdate=lampPool.instanceMatrix.needsUpdate=tunRidge.instanceMatrix.needsUpdate=true;
 // сечения пещеры: радиус и центр «дышат» вдоль длины, раструб у входов
 for(let i=0;i<TSEG;i++){const s=i/(TSEG-1)*SEG.len,zw=SEG.start+s,z=S.dist-zw; // вперёд = -z
  const e=Math.min(1,s/18,(SEG.len-s)/18); // стены неровные только в середине; у входов и выхода гладко
  const r=3.9+(.95*Math.sin(s*.11+tunSeed)+.6*Math.sin(s*.27+tunSeed*2))*e; // радиус с запасом до ближайших деревьев (|x|>=10.2): ни одна крона не проходит сквозь оболочку
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
function collide(dt){const dz=S.speed*dt;S.platTop=0;const h=S.roll>0?1.0:1.62, // хитбокс = измеренный клубок .90 + зазор
 feet=S.roll>0?S.rollY:S.y,top=feet+h; // под кувырком прогибаемся (как подкатом v4.34): bar закрывает щель, не ломая физику прыжка
 for(const o of S.obs){if(o.dead||!laneHit(o))continue;
  if(PLAT[o.type]){ // НЕПРЕРЫВНАЯ стенка: за кадр скролл проходит dz~0.3-0.7м; дискретная проверка «внутри окна» ловила приземляющегося в метре перед стенкой уже ПОСЛЕ прыжка через стенку = смерть вместо запрыга. Свип: момент входа передней стенки в героя и высота ног в этот момент
   const hL=o.len/2,wall=hL+.45; // полуlong + глубина героя .35 + запас
   const z1=o.z,z0=o.z-dz; // путь центра корпуса за кадр (o.z уже прокручен)
   if(z0>wall){continue;} // ещё далеко
   const inRoof=Math.abs(o.z)<hL; // крыша над героем СЕЙЧАС = опора
   if(inRoof){
    if(feet>=o.hgt-.05||S.platTop>0){S.platTop=Math.max(S.platTop||0,o.hgt);continue;}
    if(S.air){S.y=o.hgt;S.vy=0;S.air=false;S.platTop=Math.max(S.platTop||0,o.hgt);land=1;burst(S.x,o.hgt+.05,0,0x6B4E2E,4);continue;}
    return die();}
   // свип передней стенки: был за -wall, стал ближе -wall
   if(z0<-wall&&z1>=-wall){
    const t=(-wall-z0)/(z1-z0),feetAt=feet-S.vyPrev*dt*(1-t); //高度 ног в момент входа (vyPrev=vy ДО гравитации этого кадра; откатываем падение за кадр)
    if(feetAt>=o.hgt-.12||S.platTop>0){S.platTop=Math.max(S.platTop||0,o.hgt);if(S.vy<0){S.y=o.hgt;S.vy=0;S.air=false;land=1;burst(S.x,o.hgt+.05,0,0x6B4E2E,4);}continue;}
    if(S.air){S.y=o.hgt;S.vy=0;S.air=false;S.platTop=Math.max(S.platTop||0,o.hgt);land=1;burst(S.x,o.hgt+.05,0,0x6B4E2E,4);continue;} // авто-подъём у борта
    return die();}
   continue;}
  const near=Math.abs(o.z)<(o.type==='wall'?1.35:.95); // валун круглый R1.15, а не вагон 4.2 длиной
  if(!near)continue;
  if(o.type==='low'&&feet<.7)return die();
  if(o.type==='bar'&&top>1.05)return die();
  if(o.type==='wall')return die();
  if((o.type==='mob'||o.type==='bomb'||o.type==='boss')&&feet<(o.type==='boss'?1.9:1.5))return die();}
 for(const c of S.coinsArr){if(!c.taken&&Math.abs(c.z)<.9&&c.lane===S.lane&&Math.abs(S.y+.6-c.y)<.9){
  c.taken=true;scene.remove(c.m);S.coins++;sfx('pick');sparkle(S.x+bend(0),S.y+.7,0);burst(S.x+bend(0),S.y+.7,0,0xFFD879,8);S.coinsArr=S.coinsArr.filter(x=>x!==c);}}
 updPickFx(dt);}
// отладка: лог причин смерти в console (для боевого автотеста)
function die(){ // S.revive НЕ сбрасываем: счётчик ошибок реванша живёт до конца забега (3 ошибки = ПОЙМАЛИ)
 S.killer=S.obs.find(o=>!o.dead&&laneHit(o)&&o.z>-3&&o.z<3)||null; // «во что врезались» — для математ-реванша; окно шире collide-ного (wall R1.15, boss шире) so the killer is always captured
 if(MJ&&MJ.rig){heroM.rotation.set(0,0,0);if(rollRoot){rollRoot.rotation.set(0,0,0);}const rt=MJ.rig.parent;if(rt.__by!==undefined)rt.position.y=rt.__by;}S.mode='over';S.dead=.001;S.dz=(Math.random()<.5?-1:1)*(.35+Math.random()*.3);S.shake=1;S.flash=.4;S.ts=.32;sfx('bump');
 S.best=Math.max(S.best,Math.floor(S.score));localStorage.setItem('nr_best',S.best);
 S.bestC=Math.max(S.bestC,S.coins);localStorage.setItem('nr_bestc',S.bestC);
 burst(S.x+bend(0),S.y+.8,0,0xF5F5F5,30);showOver();}
// ---------- input ----------
function move(d){const l=Math.max(0,Math.min(2,S.lane+d));if(l!==S.lane&&S.mode==='run'){S.stepPh=S.anim;S.stepT=5.5/S.speed;S.stepD=Math.sign(l-S.lane);}S.lane=l;}
function jump(){if(!S.air&&S.roll<=0){S.vy=JUMP_V;S.air=true;S.coy=0;sfx('jump');}

}
function doRoll(){if(S.roll>0)return;S.roll=.62;S.rollAir=S.air;if(S.air){S.rollY=S.y;S.rollV=Math.min(S.vy,-2);S.vy=0;sfx('jump');}else{S.rollY=0;S.rollV=0;}}
// ---------- выстрел из лука: стрела мгновенно летит вперед по полосе, сбивает камень/врага ----------
function arrowGeo(){const g=new THREE.Group();
 const shaft=new THREE.Mesh(AR_CACHE.shaft,AR_CACHE.shaftM);shaft.rotation.x=Math.PI/2;g.add(shaft); // общая геометрия/материал: стрелы сыплются часто, new на выстрел = GC-фризы на телефоне
 const head=new THREE.Mesh(AR_CACHE.head,AR_CACHE.headM);head.rotation.x=-Math.PI/2;head.position.z=-.6;g.add(head);
 for(let i=0;i<2;i++){const f=new THREE.Mesh(AR_CACHE.fletch,AR_CACHE.fletchM);f.position.set(0,0,.42);f.rotation.z=i?1.57:0;g.add(f);}
 return g;}
const AR_CACHE={}; // лениво: THREE ещё не точно инициализирован в момент парса модуля... инициализируется в первом arrowGeo
{const L=(c,r,m)=>new THREE.MeshStandardMaterial({color:new THREE.Color(c).convertSRGBToLinear(),roughness:r,metalness:m});
 AR_CACHE.shaft=new THREE.CylinderGeometry(.045,.045,1.3,6);AR_CACHE.shaftM=new THREE.MeshBasicMaterial({color:0xFFF3D0,toneMapped:false,fog:false}); // толще и ярче: Basic без тумана читается на любой дистанции
 AR_CACHE.head=new THREE.ConeGeometry(.095,.3,4);AR_CACHE.headM=new THREE.MeshBasicMaterial({color:0x1A1D21,toneMapped:false,fog:false});
 AR_CACHE.fletch=new THREE.BoxGeometry(.3,.026,.4);AR_CACHE.fletchM=new THREE.MeshBasicMaterial({color:0xFF2E2E,toneMapped:false,fog:false});}
let arrowFlashKick=0,arrowFlash=null; // вспышка у выпуска: 0.12 с, растёт и гаснет - без неё выстрел не читается
{const g=new THREE.RingGeometry(.05,.14,12);
 arrowFlash=new THREE.Mesh(g,new THREE.MeshBasicMaterial({color:0xFFE89A,transparent:true,opacity:0,fog:false,side:THREE.DoubleSide,depthWrite:false,blending:THREE.AdditiveBlending}));
 arrowFlash.renderOrder=3;arrowFlash.frustumCulled=false;scene.add(arrowFlash);}
let arrowTrail=null; // хвост-лента за стрелой: ~4 px на дистанции поражения, без неё стрела теряется на текстуре тропы
{const g=new THREE.BufferGeometry();
 g.setAttribute('position',new THREE.BufferAttribute(new Float32Array(2*3),3));
 arrowTrail=new THREE.Line(g,new THREE.LineBasicMaterial({color:0xFFF3B0,transparent:true,opacity:.55,fog:false,depthWrite:false}));
 arrowTrail.frustumCulled=false;arrowTrail.visible=false;scene.add(arrowTrail);}
const DRAW_T=.5,SHOT_CD=.95; // натяжение .5 с, выстрел + общая блокировка .95 с: тап-спам не превращается в пулемёт
function shoot(){ // тап = НАЧАЛО натяжения; выпуск в конце фазы (releaseShot)
 if(S.mode!=='run'||bowT>0||shotCd>0)return;
 bowT=DRAW_T;shotCd=SHOT_CD;}
function releaseShot(){ // вызывается из driveHero в конце DRAW_T
 const z0=-5; const m=arrowGeo();m.position.set((S.lane-1)*LANE+bend(z0),S.y+.95,z0);
 scene.add(m);S.arrows.push({x:S.lane,z:z0,m,hit:0});S.arrowsMade++; // NB: полоса в .x; updArrows читает a.x
 twang();
 S.shake=Math.max(S.shake,.28); // kick камеры: выстрел чувствуется телом
 if(arrowFlash){const fp=new THREE.Vector3();m.getWorldPosition(fp);fp.y+=.06;arrowFlash.position.copy(fp);arrowFlash.userData.t=.001;}}
function updArrows(dt){
 for(let i=S.arrows.length-1;i>=0;i--){const a=S.arrows[i];
  if(a.hit){scene.remove(a.m);S.arrows.splice(i,1);if(!S.arrows.length&&arrowTrail)arrowTrail.visible=false;continue;} // поражение: стрела исчезает в тот же кадр (летела «вместе с героем», пока гасла .35c)
  a.z-=90*dt;if(a.z<-90){scene.remove(a.m);S.arrows.splice(i,1);if(!S.arrows.length&&arrowTrail)arrowTrail.visible=false;continue;}
  a.m.position.z=a.z;a.m.position.x=(a.x-1)*LANE+bend(a.z);
  if(arrowTrail){ // хвост за ЛЕТАЩЕЙ стрелой (эта ветка - до hit-continue, поражающие её не видят)
   const p=arrowTrail.geometry.attributes.position.array;
   p[0]=a.m.position.x;p[1]=a.m.position.y;p[2]=a.m.position.z;p[3]=a.m.position.x;p[4]=a.m.position.y;p[5]=a.m.position.z+1.5;
   arrowTrail.geometry.attributes.position.needsUpdate=true;arrowTrail.visible=true;}
  if(arrowFlash){const f=arrowFlash.userData.t||0;
   if(f>0){arrowFlash.userData.t=f+dt;
    const k=arrowFlash.userData.t/.12;
    if(k>=1){arrowFlash.material.opacity=0;arrowFlash.userData.t=0;}
    else{const p=new THREE.Vector3();a.m.getWorldPosition(p);arrowFlash.position.copy(p);arrowFlash.position.z-=.35;
     arrowFlash.quaternion.copy(camera.quaternion); // всегда лицом к камере: тонкое кольцо не ребрится в ноль
     arrowFlash.scale.setScalar(.6+k*1.9);arrowFlash.material.opacity=.9*(1-k);}}
   else arrowFlash.material.opacity=0;}
  for(const o of S.obs){if(o.dead||o.lane!==a.x)continue; // у стрелы полоса в .x
   const R=o.type==='wall'?1.35:1.0; // радиус цели: валун крупнее
   if(Math.abs(o.z-a.z)<R+.5){ // поражение
    if(!SHOOTABLE[o.type])continue;
    o.dead=true;killFx(o); // единое поражение для всех типов (у магты был скрытый hp=2 - «не убивается»)
    scene.remove(a.m);S.arrows.splice(i,1);if(!S.arrows.length&&arrowTrail)arrowTrail.visible=false;break;}}}
 // мертвые удаляются только здесь - в update их filter уже не увидит (фильтр идет раньше)
 S.obs=S.obs.filter(o=>o.dead?(scene.remove(o.m),false):true);}
function killFx(o){const p=new THREE.Vector3();o.m.getWorldPosition(p);
 const col=o.type==='boss'?0xFF1A2E:o.type==='mob'?0x7B4A2F:o.type==='bomb'?0x2E7A4A:0x8F8D89;
 burst(p.x,p.y+.7,p.z,col,18);hearts(p.x,p.y+.9,p.z);S.score+=60;S.shake=Math.max(S.shake,.18);sfx('bump');}
// сердечки-подтверждение попадания: аддитивные билборды, всплывают и гаснут
const heartCv=document.createElement('canvas');heartCv.width=heartCv.height=64;
{const g2=heartCv.getContext('2d');g2.fillStyle='#ff4f7b';const s=64;
 g2.beginPath();g2.moveTo(s/2,s*.88);g2.bezierCurveTo(s*.05,s*.55,s*.12,s*.12,s/2,s*.34);
 g2.bezierCurveTo(s*.88,s*.12,s*.95,s*.55,s/2,s*.88);g2.fill();}
const heartTex=new THREE.CanvasTexture(heartCv);
const heartsP=[];const NH=40;
const heartsMesh=new THREE.InstancedMesh(new THREE.PlaneGeometry(.3,.3),
 new THREE.MeshBasicMaterial({map:heartTex,transparent:true,depthWrite:false,blending:THREE.AdditiveBlending,fog:false}),NH);
heartsMesh.frustumCulled=false;heartsMesh.renderOrder=3;scene.add(heartsMesh);
function hearts(x,y,z){for(let i=0;i<7;i++)heartsP.push({x:x+(Math.random()-.5)*.5,y:y+Math.random()*.3,z:z+(Math.random()-.5)*.5,
 vy:1.8+Math.random()*1.6,sway:Math.random()*6.28,t:0,life:.6+Math.random()*.35});
 while(heartsP.length>NH)heartsP.shift();}
function updHearts(dt){for(let j=heartsP.length-1;j>=0;j--){const p=heartsP[j];p.t+=dt;if(p.t>=p.life){heartsP.splice(j,1);continue;}
 p.y+=p.vy*dt;p.vy-=1.4*dt;}
 for(let i=0;i<NH;i++){const p=heartsP[i];
  if(!p){V0.set(0,0,0);M4.compose(V1.set(0,-9999,0),Q0.identity(),V0);}
  else{const k=1-p.t/p.life;V1.set(p.x+Math.sin(p.sway+p.t*7)*.12,p.y,p.z);
   const s=(.7+.5*Math.sin(Math.min(1,p.t*6)*1.57))*k*1.4+.25;V0.set(s,s,1);M4.compose(V1,Q0.copy(camera.quaternion),V0);} // Q0 глобальный скиретч, .copy экономит clone на инстанс
  heartsMesh.setMatrixAt(i,M4);}
 heartsMesh.instanceMatrix.needsUpdate=true;}
function act(a,e){if(window.dlog&&a!=='T'&&a!=='O')dlog.swipe();
 if(S.mode==='menu'){reset();showRun();if(window.dlog)dlog.run();return;}
 if(S.mode==='over'){if(S.dead<=1.05)return; // limp-анимация: любые тапы молчат
 if(e&&e.timeStamp-lastTapTS<600)return; lastTapTS=e?e.timeStamp:0; // touchend+синтетический click = один жест (иначе фон-тап удваивает revive)
 if($('mathrow').style.display!=='none'){ // пока пример открыт: тап по фону = новый пример, кнопка/Enter = проверка
  if(!e||!e.target.closest('#mathrow')){S.revive++;newMath();if(S.revive>=3)showOver();}
  return;}
 if($('ogo').style.visibility!=='hidden'){reset(),showRun();}
 return;} // до respawn-задержки любые клавиши молчат
 if(S.mode!=='run')return;
 if(a==='L')move(-1);else if(a==='R')move(1);else if(a==='U')jump();else if(a==='D')doRoll();else if(a==='F')shoot();
 if(a==='U'||a==='F')window.__lastTap=a;}
let tsx=0,tsy=0,lastTapTS=0;
addEventListener('touchstart',e=>{tsx=e.touches[0].clientX;tsy=e.touches[0].clientY;},{passive:true});
addEventListener('touchend',e=>{const dx=e.changedTouches[0].clientX-tsx,dy=e.changedTouches[0].clientY-tsy;
 if(Math.abs(dx)<24&&Math.abs(dy)<24)return act(S.mode==='run'?'F':'T',e); // тап = выстрел из лука (over: e для кнопки/фону)
 if(Math.abs(dx)>Math.abs(dy))act(dx>0?'R':'L');else act(dy>0?'D':'U');},{passive:true});
addEventListener('click',e=>{if(S.mode==='over'&&S.dead>1.05)act('T',e);});
addEventListener('keydown',e=>act({ArrowLeft:'L',ArrowRight:'R',ArrowUp:'U',ArrowDown:'D',' ':'U'}[e.key]));
$('mathok').addEventListener('click',mathCheck);
$('mathn').addEventListener('keydown',e=>{e.stopPropagation();if(e.key==='Enter')mathCheck();});
// ---------- HUD ----------
const fmt=n=>(''+n).padStart(6,'0');
$('mbest').textContent='РЕКОРД '+fmt(S.best)+' · ⚔ '+S.bestC;
function renderMenuIdle(){scene.userData.iv=1;scene.traverse(o=>{if(o.isInstancedMesh)o.visible=false;}); // до первого rAF-кадра мир не собран: инстансы-валуны в identity стоят кучей под героем; прячем их до первого кадра loop()
 const m=new THREE.Matrix4().makeTranslation(0,-50,0);[tunRidge,poles,flame,glow,lampPool,stars,heartsMesh].forEach(im=>{for(let i=0;i<im.count;i++)im.setMatrixAt(i,m);im.instanceMatrix.needsUpdate=true;});} // паркуем под землёй всё, что пишет только run-ветка (dayFx/updHearts): иначе на меню и экране смерти эти инстансы стоят identity-кучей в (0,0,0) под героем (валуны, фонари, звёзды, сердца)
function showMenu(){$('menu').style.display='flex';$('over').style.display='none';$('hud').style.display='none';$('meta').style.display='none';
 let v=$('ver');if(!v){v=document.createElement('div');v.id='ver';v.style.cssText='position:fixed;bottom:6px;left:0;right:0;text-align:center;font-size:11px;opacity:.5';document.body.appendChild(v);}
 v.style.display='block';v.textContent='v'+VER+' · '+(mReady?'GLB':'FALLBACK');}
function showRun(){$('menu').style.display='none';$('over').style.display='none';$('hud').style.display='flex';$('meta').style.display='block';if($('ver'))$('ver').style.display='none';}
function showOver(){ // v4.45 «математический реванш»: тап по экрану смерти = простой пример (сложение/вычитание, однозначные и двузначные); верно = рестарт с +1 реваншем, неверно = результат закрыт
 $('over').style.display='flex';$('hud').style.display='none';$('over').style.pointerEvents='auto';
 $('ostats').textContent=fmt(Math.floor(S.score))+' · ⚔ '+S.coins+' · '+Math.floor(S.dist)+'m';
 $('obest').textContent='РЕКОРД '+fmt(S.best)+' · ⚔ '+S.bestC;
 if(S.revive>=3){$('oh1').textContent='ПОЙМАЛИ!';$('ocard').style.display='block';$('ogo').style.visibility='visible';$('ogo').textContent='НАЖМИ, ЧТОБЫ ПОВТОРИТЬ';
  $('mathq').style.display='none';$('mathrow').style.display='none';return;}
 newMath(); // пример показывает ТОЛЬКО showOver (единая точка): die->showOver->newMath = один mathrow на смерть; тап по фону = след. пример; верный ответ = reviveRun (продолжение), неверный = +1 ошибка
}
let mathAns=0;
function newMath(){let a=1+Math.floor(Math.random()*9),b=1+Math.floor(Math.random()*9); // уровень 6-летки: только однозначные, вычитание без отрицательного ответа
 if(Math.random()<.5){mathAns=a+b;$('mathq').textContent=a+' + '+b+' = ?';}
 else{if(b>a){const t=a;a=b;b=t;}mathAns=a-b;$('mathq').textContent=a+' − '+b+' = ?';}
 $('oh1').textContent='РЕВАНШ?';$('ocard').style.display='none';$('ogo').style.visibility='hidden';
 $('mathq').style.display='block';$('mathrow').style.display='flex';$('matherr').style.visibility='hidden';$('mathn').value='';$('mathn').focus();}
function mathCheck(){if(!$('mathrow')||$('mathrow').style.display==='none')return;
 if(S.mode!=='over'||$('mathn').value.trim()===''||+$('mathn').value!==mathAns){
  S.revive++;$('matherr').style.visibility='visible';if(S.revive>=3){$('mathrow').style.display='none';showOver();}return;}
 $('mathrow').style.display='none';$('mathq').style.display='none';reviveRun();}
function reviveRun(){ // верный ответ = бежать ДАЛЬШЕ с той же точки: «убийца» разрушается (killFx как от стрелы), полоса впереди расчищена на ~30м; мир, дистанция, счёт, монеты и полоса не трогаются
 const k=S.killer&&!S.killer.dead?S.killer:S.obs.find(o=>!o.dead&&laneHit(o)&&o.z>-6&&o.z<4); // шире fallback: герой стоит в шаге от убийцы, окно ±4m покрывает wall/boss
 if(k){k.dead=true;killFx(k);}
 S.obs.forEach(o=>{if(!o.dead&&o.lane===S.lane&&o.z>=2&&o.z<30){o.dead=true;scene.remove(o.m);}});
 S.mode='run';S.dead=0;S.shake=0;S.flash=0;S.ts=1;S.air=false;S.roll=0;S.rollY=0;S.rollV=0;S.y=0;S.vy=0;S.stepT=0;S.stepPh=null;S.speed=Math.max(16,S.speed*.6);
 if(heroM.__tuck||heroM.__piv||heroM.__shell){ // смерть в кувырке -> реванш: полный hard-reset roll-механики (иначе roll-остатки перетирают ран-позу = «улетевшие конечности» на первом кадре)
   heroM.__tuck=false;heroM.__piv=null;heroM.__shell=null;heroM.__rest=null;
   const _rt=(rollRoot||heroM);_rt.position.set(0,0,0);_rt.rotation.set(0,0,0);
   if(rollRoot){rollRoot.position.set(0,0,0);rollRoot.rotation.set(0,0,0);}
   heroM.rotation.set(0,0,0);heroM.scale.set(1,1,1);
   const j=MJ;const ML=[j&&j.rig,j&&j.head,j&&j.hat,j&&j.armL,j&&j.armR,j&&j.elbowL,j&&j.elbowR,j&&j.legL,j&&j.legR,j&&j.kneeL,j&&j.kneeR,j&&j.ankleL,j&&j.ankleR];
   for(const o of ML){if(!o)continue;o.rotation.set(0,0,0);o.__t&&(o.__t.x=0,o.__t.y=0,o.__t.z=0);}}
 if(heroBow)heroBow.visible=true;
 for(const a of S.arrows)scene.remove(a.m);if(arrowTrail)arrowTrail.visible=false;
 S.missed=0;deathPlayed=false;heartsP.length=0;
 $('over').style.display='none';$('over').style.pointerEvents='none';$('hud').style.display='flex';$('meta').style.display='block';}
showMenu();
// ---------- update ----------
const M4=new THREE.Matrix4(),Q0=new THREE.Quaternion(),QE=new THREE.Euler(),V1=new THREE.Vector3(),V0=new THREE.Vector3(1,1,1);
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
 const hT=SEG.m>.5?1:0; // в пещере нет деревьев/камней/кустов: гасим всю дорогу-растительность, не только стволы
 const hA=hT<.5?1:0; // открытая фаза: обычная сборка матриц
 for(let i=0;i<NTree;i++){const b=treePos[i],z=-260+(((b.z+S.dist*.85)%320)+320)%320;
  if(hT){V0.set(0,0,0);V1.set(0,-9999,z);}
  else{V1.set(b.x+bend(z),b.h/2,z);V0.set(b.w,b.h,b.w);}
  M4.compose(V1,Q0.identity(),V0);trunks.setMatrixAt(i,M4);
  if(!hT){V1.set(b.x+bend(z),b.h+b.canopyH/2,z);V0.set(b.canopyW,b.canopyH,b.canopyW);}
  M4.compose(V1,Q0.identity(),V0);canopies.setMatrixAt(i,M4);
  if(!hT){V1.set(b.x+bend(z)+b.canopyW*.35,.025,z);V0.set(b.canopyW*1.15,1,b.canopyW*1.15);}
  M4.compose(V1,Q0.identity(),V0);treeSh.setMatrixAt(i,M4);}
 treeSh.instanceMatrix.needsUpdate=true;
 trunks.instanceMatrix.needsUpdate=true;canopies.instanceMatrix.needsUpdate=true;V0.set(1,1,1);
 for(let i=0;i<NStone;i++){const v=stonePos[i],z=-260+(((v.z+S.dist*.85)%320)+320)%320; // камни у дороги
  if(hT){V0.set(0,0,0);V1.set(0,-9999,z);}else V1.set(v.x+bend(z),v.w*.5,z);
  if(!hT)V0.set(v.w,v.w*.7,v.w);
  Q0.setFromAxisAngle(YAX,v.ry);M4.compose(V1,Q0,V0);stoneMesh.setMatrixAt(i,M4);}
 stoneMesh.instanceMatrix.needsUpdate=true;Q0.identity();
 for(let i=0;i<NBush;i++){const b=bushPos[i],z=-200+(((b.z+S.dist)%220)+220)%220;
  const tilt=Math.sin(S.t*1.6+b.sw+b.x)*.13; // ветер по траве
  QE.set(tilt,b.ry,0,'YXZ');Q0.setFromEuler(QE);
  if(hT){V0.set(0,0,0);V1.set(0,-9999,z);}else V1.set(b.x+bend(z),b.h*.42,z);
  if(!hT)V0.set(.95,b.h*1.15,.95);
  M4.compose(V1,Q0,V0);bushes.setMatrixAt(i,M4);}
 bushes.instanceMatrix.needsUpdate=true;V0.set(1,1,1);Q0.identity();}
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
 S.vyPrev=S.vy;
 if(S.roll<=0){S.vy-=GRAV*dt;S.y+=S.vy*dt;
 const fl=S.platTop||0; // platTop заполнен коллизией ПРОШЛОГО кадра (collide идёт до физики): гравитация садится на крышку; за краем fl=0 - падаем на асфальт
 if(S.y<=fl){if(S.air){burst(S.x,fl+.1,0,0x6B4E2E,6);land=1;}S.y=fl;S.vy=0;S.air=false;S.coy=0;}}
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
  o.z+=vz;o.m.position.z=o.z-(o.len?o.len/2:0);o.m.position.x=(o.lane-1)*LANE+bend(o.z);
  if(o.type==='wall')o.m.userData.spin.rotation.x+=vz/o.m.userData.R*.08;} // валун катится К игроку: верх камня едет в +z; спин ×.08 - на 30+ м/с честное вращение = мясорубка
 for(const c of S.coinsArr){c.z+=dz;c.m.position.z=c.z;c.m.position.x=(c.lane-1)*LANE+bend(c.z);c.m.rotation.y+=dt*.9;} // медленное вращение - клинок можно разглядеть
 collide(dt); // ПОСЛЕ скролла: o.z = кадр текущего кадра; platTop читает гравитация следующего (1-кадровая задержка держит героя на крыше на ~10см дольше - не поймать на глаз)
 for(const o of S.obs){ // враги идут НА героя: свой ход поверх скролла; boss самый быстрый
  const crawl=o.type==='mob'?3.2:o.type==='boss'?6.5:o.type==='bomb'?1.4:0;
  if(!crawl)continue;
  o.z+=crawl*dt;o.m.position.z=o.z;o.m.position.x=(o.lane-1)*LANE+bend(o.z);
  if(o.type==='mob'||o.type==='boss'){o.m.position.y=Math.abs(Math.sin(S.t*9))*(o.type==='boss'?.18:.08);} // перевалка в ходьбе
  if(o.gl){ // шагающая анимация GLB-фигурок: наклон вперёд + покачивание с индивидуальной фазой
   const sp=o.walk||2.2, ph=(o.ph||0)+S.t*sp;
   o.gl.rotation.z=Math.sin(ph)*.07; o.gl.rotation.x=.06+Math.sin(ph*2+.8)*.03;
   o.gl.position.y=-o.gl.userData.dip*o.gl.scale.x+Math.abs(Math.sin(ph))*.025;
  }
 }
 updArrows(dt);updHearts(dt);
 S.obs=S.obs.filter(o=>o.z<DESPAWN_Z||(scene.remove(o.m),false));
 S.coinsArr=S.coinsArr.filter(c=>c.z<DESPAWN_Z&&!c.taken||(scene.remove(c.m),false));
 if(S.dist>=nextSpawn)spawn();
 }
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
  player.position.set(S.x,S.y+LIFT*(1-s),0); // LIFT-gate: в беге герой на +LIFT (подошва в нуле); скелетная поза кувырка сама ставит клубок на ноль - LIFT гасится вместе со входом в позу (s), иначе старт кувырка = прыжок на полкорпуса
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
 if(scene.userData.iv){scene.userData.iv=0;scene.traverse(o=>{if(o.isInstancedMesh)o.visible=true;});} // мир собран первым кадром - инстансы снова нужны
 update(dt*S.ts);updInst();if(mReady)driveHero(dt);else animRig(dt);animCam(dt);
 $('score').textContent=fmt(Math.floor(S.score));
 $('coins').textContent='⚔ '+S.coins;
 $('best').textContent=fmt(S.best);
 $('meta').textContent=Math.floor(S.dist)+' м \u00b7 '+Math.floor(S.speed)+' км/ч';
 renderer.render(scene,camera);
 requestAnimationFrame(loop);}
renderMenuIdle(); // старт до первого rAF-кадра
requestAnimationFrame(loop);