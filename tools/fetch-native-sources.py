#!/usr/bin/env python3
"""Fetch SHA-pinned public Skiko/Skia sources for the full native build."""
import hashlib,json,pathlib,subprocess,zipfile
root=pathlib.Path(__file__).resolve().parents[1];cache=root/'.cache/vulkan-crash';cache.mkdir(parents=True,exist_ok=True)
manifest=json.loads((root/'metadata/native-build-inputs.json').read_text())
for item in manifest['archives'][:2]:
 p=cache/item['filename']
 if not p.exists() or hashlib.file_digest(p.open('rb'),'sha256').hexdigest()!=item['sha256']:
  tmp=p.with_suffix(p.suffix+'.download')
  subprocess.run(['curl','-fsSL','--retry','2',item['url'],'-o',str(tmp)],check=True)
  if hashlib.file_digest(tmp.open('rb'),'sha256').hexdigest()!=item['sha256']:raise SystemExit('Download checksum mismatch: '+item['filename'])
  tmp.replace(p)
 dest=cache/('source' if item['filename']=='skiko-source.zip' else 'skia-windows');dest.mkdir(exist_ok=True)
 with zipfile.ZipFile(p) as z:
  for name in z.namelist():
   target=(dest/name).resolve()
   if not target.is_relative_to(dest.resolve()):raise SystemExit('Archive path leaves extraction root')
  z.extractall(dest)
 print(dest)
