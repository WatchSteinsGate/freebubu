#!/usr/bin/env python3
"""Inspect the resolver snapshot without changing or deriving guessed constants."""
import collections,io,json,pathlib,struct,sys,zipfile
jar=pathlib.Path(sys.argv[1]);dest=pathlib.Path(sys.argv[2])
with zipfile.ZipFile(jar) as z:
 name=next((n for n in ['thatonecatgif/data/bootstrap.bin','assets/nebula/bootstrap.bin','thatonecatgif/assets/bootstrap.bin'] if n in z.namelist()),None)
 if name is None:raise SystemExit('No recognized bootstrap table resource')
 data=z.read(name)
s=io.BytesIO(data)
def integer():return struct.unpack('>i',s.read(4))[0]
def text():return s.read(integer()).decode()
magic,count=integer(),integer();rows=[]
for _ in range(count):rows.append((text(),text(),*struct.unpack('>qqq',s.read(24))))
mask=(1<<64)-1
def unxor(x,shift):
 y=x
 for _ in range(64//shift+1):y=x^(y>>shift)
 return y&mask
def splitmix_inverse(x):return unxor(unxor(unxor(x,31)*pow(0x94d049bb133111eb,-1,1<<64)&mask,27)*pow(0xbf58476d1ce4e5b9,-1,1<<64)&mask,30)
def murmur_inverse(x):return unxor(unxor(unxor(x,33)*pow(0xc4ceb9fe1a85ec53,-1,1<<64)&mask,33)*pow(0xff51afd7ed558ccd,-1,1<<64)&mask,33)
groups=collections.defaultdict(set);largest=collections.Counter(o for o,*_ in rows).most_common(1)[0][0];subset=[r for r in rows if r[0]==largest];tests={}
for name,inv in [('identity',lambda x:x),('splitmix64',splitmix_inverse),('murmur64',murmur_inverse)]:
 for op in ['xor','subtract']:
  vals=[]
  for o,n,a,b,c in subset:
   x=inv((a^c)&mask);vals.append((x^b) if op=='xor' else (x-b)&mask)
  tests[name+' inverse, '+op+' B']={'records':len(vals),'distinct_inferred_seeds':len(set(vals)),'constant_seed_match':len(set(vals))==1}
for owner,name,a,b,result in rows:groups[(b&mask)>>32].add(owner)
report={'records':count,'bytes':len(data),'owners':len(set(r[0] for r in rows)),'class_ids':len(groups),'class_ids_map_to_single_owner':all(len(x)==1 for x in groups.values()),'unique_A':len(set(r[2] for r in rows)),'unique_B':len(set(r[3] for r in rows)),'distinct_results':len(set(r[4] for r in rows)),'candidate_checks':tests,'reconstructed':False,'note':'B has a class ID in its high 32 bits and a site index in its low 32 bits. Candidate invertible mixers did not recover a fixed per-class seed. Recorded data remains required; no guessed resolver was installed.'}
dest.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
