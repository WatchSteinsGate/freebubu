#!/usr/bin/env python3
"""Relocate this Windows build, including Java constants and JNI export names."""
import hashlib,io,json,pathlib,struct,sys,zipfile
from patch_native_debug import patch as patch_debug_native
U2=struct.Struct('>H');U4=struct.Struct('>I')
# Native payload encodes its zip.nebula ABI; retain until native source is available.
RENAMES=[(b'Java_thatonecatgif',b'Java_nebula_bridge')]
RESOURCES=[(b'thatonecatgif/FreeCosmeticsLOL/editor.html',b'assets/nebula/editor/index.html'),(b'thatonecatgif/world-offset.accesswidener',b'assets/nebula/world-offset.accesswidener'),(b'thatonecatgif/assets/native/',b'natives/windows-x64/'),(b'thatonecatgif/assets/',b'assets/nebula/'),(b'thatonecatgif/runtime/',b'natives/windows-x64/')]
def replace(data):
 # Keep historical on-disk migration locations readable.
 if data==b"thatonecatgif/FreeCosmeticsLOL":return data
 for old,new in RESOURCES+RENAMES+[(b'thatonecatgif/',b'nebula/bridge/'),(b'thatonecatgif.',b'nebula.bridge.')]:data=data.replace(old,new)
 if data==b'thatonecatgif':data=b'nebula/bridge'
 return data
def classfile(data):
 # Preserve every constant-pool index and instruction; update only UTF-8 constants.
 out=bytearray(data[:10]);i=10;k=1;count=U2.unpack_from(data,8)[0]
 widths={3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
 while k<count:
  tag=data[i];i+=1;out.append(tag)
  if tag==1:
   n=U2.unpack_from(data,i)[0];i+=2;s=replace(data[i:i+n]);out+=U2.pack(len(s))+s;i+=n
  else:
   n=widths[tag];out+=data[i:i+n];i+=n
   if tag in (5,6):k+=1
  k+=1
 return bytes(out)+data[i:]
def native(data):
 d=bytearray(data)
 for old,new in RENAMES+[(b'thatonecatgif/',b'nebula/bridge/'),(b'thatonecatgif.',b'nebula.bridge.')]:
  assert len(old)==len(new)
  d=d.replace(old,new).replace(old.decode().encode('utf-16le'),new.decode().encode('utf-16le'))
 # PE GetProcAddress binary-searches its sorted name table. Re-sort after JNI renames.
 pe=struct.unpack_from('<I',d,0x3c)[0];opt=pe+24;magic=struct.unpack_from('<H',d,opt)[0]
 export=struct.unpack_from('<I',d,opt+(112 if magic==0x20b else 96))[0]
 sections=pe+24+struct.unpack_from('<H',d,pe+20)[0];ns=struct.unpack_from('<H',d,pe+6)[0]
 def raw(rva):
  for s in range(ns):
   vsize,va,size,ptr=struct.unpack_from('<IIII',d,sections+40*s+8)
   if va<=rva<va+max(vsize,size):return ptr+rva-va
  raise ValueError(f'Unmapped PE RVA {rva:x}')
 if export:
  e=raw(export);nn=struct.unpack_from('<I',d,e+24)[0];np,op=map(raw,struct.unpack_from('<II',d,e+32))
  names=[]
  for x in range(nn):
   rva=struct.unpack_from('<I',d,np+4*x)[0];p=raw(rva);end=d.index(0,p)
   names.append((bytes(d[p:end]),rva,struct.unpack_from('<H',d,op+2*x)[0]))
  for x,(_,rva,ordinal) in enumerate(sorted(names)):
   struct.pack_into('<I',d,np+4*x,rva);struct.pack_into('<H',d,op+2*x,ordinal)
 return bytes(d)
def main():
 source,dest,compiled=map(pathlib.Path,sys.argv[1:]);entries={}
 with zipfile.ZipFile(source) as z:
  original={n:z.read(n) for n in z.namelist() if not n.endswith('/')}
 loader=zipfile.ZipFile(io.BytesIO(original.pop('thatonecatgif/runtime/loader.jar')))
 original.update({n:loader.read(n) for n in loader.namelist() if n.endswith('.class')})
 for n in ('Bootstrap.class','Bootstrap$1.class','WorkerPolicy.class'):original['thatonecatgif/'+n]=(compiled/'thatonecatgif'/n).read_bytes()
 original['org/jetbrains/skiko/LibraryLoader.class']=(compiled/'LibraryLoader.class').read_bytes()
 original['okhttp3/CertificatePinner.class']=(compiled/'CertificatePinner.class').read_bytes()
 payload=original['thatonecatgif/runtime/client.dll'];patched=patch_debug_native(payload,pathlib.Path(__file__).resolve().parents[1]);original['thatonecatgif/runtime/client.dll']=patched
 old_hash=hashlib.sha256(payload).hexdigest().encode();new_hash=hashlib.sha256(patched).hexdigest().encode()
 for n in ('Bootstrap.class','Bootstrap$1.class'):original['thatonecatgif/'+n]=original['thatonecatgif/'+n].replace(old_hash,new_hash)
 original.pop('thatonecatgif/assets/native/checksums.properties') # merged below, same checksum filename
 for name,data in original.items():
  if name.startswith(('lib/ppc64-AIX','lib/ppc64-OS400')):continue
  newname=replace(name.encode()).decode()
  if name in ('skiko-windows-x64.dll','skiko-windows-x64.dll.sha256','icudtl.dat'):newname='natives/windows-x64/'+name
  if name.endswith('.class'):data=classfile(data)
  elif name.endswith('.dll') and name.startswith('thatonecatgif/'):data=native(data)
  elif name.endswith('bootstrap.bin'):data=replace(data)
  elif name.endswith(('.json','.accesswidener','.properties','.MF')):data=replace(data)
  if newname in entries:raise ValueError('Resource collision: '+newname)
  entries[newname]=data
 fabric=json.loads(entries['fabric.mod.json']);meta=json.loads(entries['nebulaclient.json']);meta['version']=fabric['version'];entries['nebulaclient.json']=(json.dumps(meta,indent=2)+'\n').encode()
 native_root='natives/windows-x64/'
 hashes={n[len(native_root):]:hashlib.sha256(v).hexdigest() for n,v in entries.items() if n.startswith(native_root) and n.endswith(('.dll','.zip'))}
 entries[native_root+'checksums.properties']=''.join(f'{n}={v}\n' for n,v in sorted(hashes.items())).encode()
 # RuntimeFiles no longer needs/ships an entry for the flattened loader JAR.
 with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for n,v in entries.items():z.writestr(n,v)
 print(json.dumps({'jar':str(dest),'entries':len(entries),'version':fabric['version'],'native_debug_patch_sites':9,'okhttp_pin_verification_disabled':True,'original_resolver_reconstructed':False,'native_client_sha256':new_hash.decode(),'sha256':hashlib.file_digest(open(dest,'rb'),'sha256').hexdigest()}))
if __name__=='__main__':main()
