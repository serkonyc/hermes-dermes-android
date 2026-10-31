# Forest-runner calm loop: ~78 s, sections A/A/B/A, pentatonic, soft plucks+pad. Mono 22050.
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
TOT=int(78*SR); buf=np.zeros(TOT+int(SR))
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
# structure: A | A' | B | A'' with 1 s lead-in; each melody = 4 bars (one per chord)
PROG=['Am','F','C','G']*4
tsec=1.0
def section(idx,bar_mels,with_arp=True,padpk=.09,basspk=.22):
    global tsec
    for bi in range(4):
        chord=[c for c in CHORDS if c[0]==PROG[idx*4+bi]][0][1]
        t0=tsec+bi*BAR
        pad(t0,chord,BAR*1.05,padpk)
        place(buf,tone(chord[0],1.2,a=.01,r=.9,peak=basspk),t0)  # bass pluck
        if with_arp: arp(t0,chord)
        if bar_mels: melody(t0,bar_mels[bi])
    tsec+=4*BAR
tsec=1.0
section(0,[[(4,'E5',.3),(2,'D5',.25),(2,'C5',.25)],
           [(4,'D5',.3),(4,'C5',.25)],
           [(2,'A4',.25),(2,'C5',.25),(4,'E5',.3)],
           [(4,'D5',.3),(2,'C5',.25),(2,'A4',.25)]])
section(1,[[(4,'G5',.28),(4,'E5',.3)],
           [(2,'D5',.25),(2,'E5',.25),(4,'D5',.3)],
           [(2,'C5',.25),(2,'D5',.25),(4,'E5',.3)],
           [(4,'D5',.25),(4,'C5',.25)]])
section(2,None,padpk=.11,basspk=.16)  # B: warm pad + sustained bass, no lead - контраст
section(3,[[(2,'E5',.25),(2,'G5',.25),(4,'A4',.3)],
           [(4,'G5',.25),(4,'E5',.28)],
           [(2,'D5',.25),(2,'E5',.25),(4,'D5',.25)],
           [(4,'C5',.25),(2,'A4',.25),(2,'C5',.25)]])
end=int(tsec*SR)  # samples
tail=int(.9*SR); buf[end:end+tail]*=np.linspace(1,0,tail)
buf=buf[:end+tail]
# loop-safe: pad sustain at start begins from silence too
head=int(.5*SR); buf[:head]*=np.linspace(0,1,head)
# gentle master: soft clip + level
buf=np.tanh(buf*1.4)*.82
peak=np.max(np.abs(buf)); buf=buf/peak*.89
w=(buf*32767).astype('<i2')
import struct,wave
wf=wave.open('/Users/serkonyc/.hermes/cache/scratch/music-v2.wav','wb')
wf.setnchannels(1); wf.setsampwidth(2); wf.setframerate(SR); wf.writeframes(w.tobytes()); wf.close()
print('duration_s',len(buf)/SR,'peak',float(peak))
