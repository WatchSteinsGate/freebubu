import json,pathlib,urllib.request,hashlib,concurrent.futures
root=pathlib.Path('/workspace/minecraft/assets');d=json.loads(next((root/'indexes').glob('*.json')).read_text());unique={x['hash'] for x in d['objects'].values()}
def fetch(h):
 p=root/'objects'/h[:2]/h;p.parent.mkdir(parents=True,exist_ok=True)
 if p.exists() and hashlib.sha1(p.read_bytes()).hexdigest()==h:return
 with urllib.request.urlopen('https://resources.download.minecraft.net/'+h[:2]+'/'+h,timeout=60) as r:data=r.read()
 assert hashlib.sha1(data).hexdigest()==h
 p.write_bytes(data)
with concurrent.futures.ThreadPoolExecutor(max_workers=20) as pool:
 for n,_ in enumerate(pool.map(fetch,unique),1):
  if n%500==0:print(n,'/',len(unique),flush=True)
print('All assets verified',flush=True)
