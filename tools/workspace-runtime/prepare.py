import json,pathlib,subprocess,hashlib,concurrent.futures,shutil,zipfile
R=pathlib.Path('/workspace/minecraft');v=json.load(open('/workspace/tools/minecraft-26.2.json'));f=json.load(open(R/'fabric-profile.json'))
def fetch(item):
 url,path,sha=item;path.parent.mkdir(parents=True,exist_ok=True)
 if path.exists() and (not sha or hashlib.sha1(path.read_bytes()).hexdigest()==sha):return
 subprocess.run(['curl','-fsSL','--retry','2','--max-time','120',url,'-o',str(path)],check=True,stderr=subprocess.DEVNULL)
 if sha:assert hashlib.sha1(path.read_bytes()).hexdigest()==sha,str(path)
def allowed(lib):
 ok=not bool(lib.get('rules'))
 for r in lib.get('rules',[]):
  if r.get('os',{}).get('name','windows')=='windows':ok=r['action']=='allow'
 return ok
items=[];cp=[]
for lib in v['libraries']:
 if not allowed(lib):continue
 a=lib['downloads'].get('artifact')
 if a:
  path=R/'libraries'/a['path'];items.append((a['url'],path,a['sha1']));cp.append(path)
for lib in f['libraries']:
 group,name,ver=lib['name'].split(':');rel=f"{group.replace('.','/')}/{name}/{ver}/{name}-{ver}.jar";path=R/'libraries'/rel;items.append((lib['url']+rel,path,None));cp.append(path)
a=v['downloads']['client'];path=R/'versions/26.2/client.jar';items.append((a['url'],path,a['sha1']));cp.append(path)
a=v['assetIndex'];items.append((a['url'],R/'assets/indexes'/f"{a['id']}.json",a['sha1']))
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:list(pool.map(fetch,items))
(R/'classpath.json').write_text(json.dumps([str(p) for p in cp]));print('Verified/downloaded client and',len(items)-2,'libraries',flush=True)
mods=R/'mods';mods.mkdir(exist_ok=True)
for project in ['fabric-api','fabric-language-kotlin']:
 dest=R/(project+'-versions.json');subprocess.run(['curl','-fsSL','--get','--data-urlencode','game_versions=["26.2"]','https://api.modrinth.com/v2/project/'+project+'/version','-o',str(dest)],check=True)
 d=json.loads(dest.read_text());a=next(x for x in d[0]['files'] if x.get('primary'));p=mods/a['filename'];fetch((a['url'],p,a['hashes']['sha1']));print('Installed',p.name,flush=True)
shutil.copy2('/workspace/nebula-work/Nebula-Windows-direct.jar',mods/'Nebula-Windows-direct.jar')
print('Fabric instance prepared',flush=True)
