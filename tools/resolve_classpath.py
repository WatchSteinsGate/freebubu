#!/usr/bin/env python3
"""Fetch the pinned build dependencies, or reuse an explicitly supplied classpath."""
from pathlib import Path
import concurrent.futures,hashlib,json,os,subprocess,sys,zipfile
root=Path(__file__).resolve().parents[1]
if os.environ.get('NEBULA_CLASSPATH'):
 print(os.environ['NEBULA_CLASSPATH']);raise SystemExit(0)
cache=root/'.cache';lock=json.loads((root/'metadata/dependencies.lock.json').read_text())
def digest(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def fetch(item):
 p=cache/item['path'];p.parent.mkdir(parents=True,exist_ok=True)
 if p.exists() and digest(p)==item['sha256']:return
 tmp=p.with_suffix(p.suffix+'.download')
 if 'url' in item:subprocess.run(['curl','-fsSL','--retry','2','--max-time','180',item['url'],'-o',str(tmp)],check=True)
 else:
  with zipfile.ZipFile(cache/item['archive']) as z:tmp.write_bytes(z.read(item['entry']))
 if digest(tmp)!=item['sha256']:raise RuntimeError('Checksum mismatch: '+item['path'])
 tmp.replace(p)
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:list(pool.map(fetch,lock['mods']))
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:list(pool.map(fetch,lock['classpath']))
print(os.pathsep.join(str(cache/i['path']) for i in lock['classpath']))
