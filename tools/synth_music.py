# Forest-runner calm track: ~230 s, 10 секций по 8 тактов, пентатоника, мягкиеpluck+pad. Mono 22050.
import numpy as np
SR=22050
def env(n,a,r,peak=1.0):
    t=np.arange(n)/SR; e=np.minimum(t/max(a,1e-4),1)*np.exp(-t/r)*peak; return e
def tone(f,dur,a=.01,r=.6,peak=.5,harm=((1,1),(2,.35),(3,.12))):
    n=int(dur*SR); t=np.arange(n)/SR; y=np.zeros(n)
    for m,amp in harm: y=y+amp*np.sin(2*np.pi*f*m*t)
    return y*env(n,a,r,peak)
def place(buf,x,at):
    i=int(at*SR); j=min(len(buf),i+len(x)); buf[i:j]+=x[:j-i]
# notes: A minor pentatonic-ish, calm
N={'A3':220,'C4':261.63,'D4':293.66,'E4':329.63,'G4':392,'A4':440,'C5':523.25,'D5':587.33,'E5':659.25,'G5':784,'E3':164.81,'G3':196,'F3':174.61,'C3':130.81,'G2':98,'F2':87.31,'C4b':261.63}
CHORDS=[('Am',[110,130.81,164.81]),('F',[87.31,130.81,174.61]),('C',[130.81,164.81,196]),('G',[98,123.47,146.83])]
BAR=4*60/84  # 84 bpm, 4/4
TOT=int(235*SR); buf=np.zeros(TOT+int(SR))
def arp(t0,chord,pat=[0,1,2,1],oct_=1):
    for k,p in enumerate(pat):
        f=chord[p%3]*2*oct_
        place(buf,tone(f,.8,a=.005,r=.5,peak=.16),t0+k*BAR/4)
def melody(t0,seq):  # seq: (beats, noteName, peak)
    b=t0
    for d,dn,pk in seq:
        place(buf,tone(N[dn],min(d*BAR*.95,2.2),a=.02,r=1.3,peak=pk,harm=((1,1),(2,.25),(4,.06))),b)
        b+=d*BAR/4
def pad(t0,notes,dur,peak=.08):
    for f in notes:
        place(buf,tone(f/2,dur,a=dur*.3,r=dur*.5,peak=peak,harm=((1,1),(2,.2))),t0)
# структура: 10 секций по 8 тактов; секция = два 4-тактовых мотива (или None - только пад)
PROG=['Am','F','C','G']*20
tsec=1.0
def section(idx,motifs,with_arp=True,padpk=.09,basspk=.22,arp_oct=1):
    global tsec
    for bi in range(8):
        chord=[c for c in CHORDS if c[0]==PROG[idx*8+bi]][0][1]
        t0=tsec+bi*BAR
        pad(t0,chord,BAR*1.05,padpk)
        place(buf,tone(chord[0],1.2,a=.01,r=.9,peak=basspk),t0)
        if with_arp: arp(t0,chord,oct_=arp_oct)
        if motifs: melody(t0,motifs[(bi//4)%len(motifs)][bi%4])
    tsec+=8*BAR
M1=[[(4,'E5',.3),(2,'D5',.25),(2,'C5',.25)],
    [(4,'D5',.3),(4,'C5',.25)],
    [(2,'A4',.25),(2,'C5',.25),(4,'E5',.3)],
    [(4,'D5',.3),(2,'C5',.25),(2,'A4',.25)]]
M2=[[(4,'G5',.28),(4,'E5',.3)],
    [(2,'D5',.25),(2,'E5',.25),(4,'D5',.3)],
    [(2,'C5',.25),(2,'D5',.25),(4,'E5',.3)],
    [(4,'D5',.25),(4,'C5',.25)]]
M3=[[(2,'E5',.25),(2,'G5',.25),(4,'A4',.3)],
    [(4,'G5',.25),(4,'E5',.28)],
    [(2,'D5',.25),(2,'E5',.25),(4,'D5',.25)],
    [(4,'C5',.25),(2,'A4',.25),(2,'C5',.25)]]
M4=[[(8,'E4',.22)],                    # разрежённая нижняя октава
    [(6,'C4',.2),(2,'D4',.18)],
    [(8,'E4',.22)],
    [(4,'G3',.2),(4,'C4',.2)]]
tsec=1.0
section(0,[M1,M2])                       # экспозиция
section(1,[M2,M3])
section(2,None,padpk=.11,basspk=.16)     # B: тёплый пад без лида
section(3,[M3,M1])
section(4,[M4,M4],with_arp=False)        # C: разрежённая, только бас-щипки
section(5,[M1,M3],arp_oct=2)             # арпеджио на октаву выше
section(6,None)                          # выдох: пад + бас
section(7,[M2,M1])
section(8,[M4,M3])
section(9,[M1,M3])                       # реприза
end=int(tsec*SR)
tail=int(.9*SR); buf[end:end+tail]*=np.linspace(1,0,tail)
buf=buf[:end+tail]
head=int(.5*SR); buf[:head]*=np.linspace(0,1,head)
buf=np.tanh(buf*1.4)*.82
peak=np.max(np.abs(buf)); buf=buf/peak*.7   # общий уровень ниже прежнего (было .89)
w=(buf*32767).astype('<i2')
import wave
wf=wave.open('/Users/serkonyc/.hermes/cache/scratch/music-v3.wav','wb')
wf.setnchannels(1); wf.setsampwidth(2); wf.setframerate(SR); wf.writeframes(w.tobytes()); wf.close()
print('duration_s',len(buf)/SR,'peak',float(peak))
