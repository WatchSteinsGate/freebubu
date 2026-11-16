#!/usr/bin/env python3
"""Verify LFS inputs and extract the published native DLL for a packaging rebuild."""
import hashlib,json,pathlib,sys,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
manifest=json.loads((root/'metadata/builds.json').read_text())
for item in manifest['artifacts']:
 p=root/'artifacts'/item['filename']
 if not p.exists() or p.stat().st_size!=item['size']:
  raise SystemExit(f'Missing LFS binary: {p}. Run git lfs pull first.')
 if hashlib.file_digest(p.open('rb'),'sha256').hexdigest()!=item['sha256']:
  raise SystemExit(f'Input checksum mismatch: {p}')
cache=root/'.cache/native-inputs';cache.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(root/'artifacts/Nebula-Windows-direct.jar') as jar:
 dll=jar.read('thatonecatgif/native/windows-x64/skiko-windows-x64.dll')
 expected=jar.read('thatonecatgif/native/windows-x64/skiko-windows-x64.dll.sha256').decode().strip()
 if hashlib.sha256(dll).hexdigest()!=expected:raise SystemExit('Published Skiko checksum mismatch')
 (cache/'skiko-windows-x64.dll').write_bytes(dll)
print(json.dumps({'NEBULA_BASE_JAR':str(root/'artifacts/Nebula-Windows-original.jar'),'NEBULA_LINUX_REFERENCE':str(root/'artifacts/Nebula-Linux-reference.jar'),'NEBULA_SKIKO_DLL':str(cache/'skiko-windows-x64.dll')},indent=2))
