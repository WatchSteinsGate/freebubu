#!/usr/bin/env python3
"""Verify relocated native resources without changing their Java ABI or DLL code."""
import hashlib,json,pathlib,shutil,sys,zipfile
source,dest=map(pathlib.Path,sys.argv[1:]);root='thatonecatgif/native/windows-x64/'
with zipfile.ZipFile(source) as z:
 # Preserve both identities: native consumers also look them up after startup.
 for cls in ['zip/nebula/loader/Loader.class','zip/nebula/loader/Key.class']:assert cls in z.namelist(),cls
 hashes={n[len(root):]:hashlib.file_digest(z.open(n),'sha256').hexdigest() for n in z.namelist() if n.startswith(root) and n.endswith(('.dll','.zip'))}
 assert hashes['client.dll'].encode() in z.read('zip/nebula/loader/Loader.class')
 recorded=dict(line.split('=',1) for line in z.read(root+'checksums.properties').decode().splitlines() if line)
 assert recorded==hashes
 assert z.read(root+'skiko-windows-x64.dll.sha256').strip()==hashes['skiko-windows-x64.dll'].encode()
shutil.copyfile(source,dest)
print(json.dumps({'sha256':hashlib.file_digest(dest.open('rb'),'sha256').hexdigest(),'native_class_names_preserved':True,'native_hashes':hashes},indent=2))
