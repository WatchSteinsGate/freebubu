#!/usr/bin/env python3
"""Update existing native files and integrity metadata after helper consolidation."""
import hashlib,json,pathlib,struct,sys,zipfile
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'.cache/native-tools'))
import pefile
source,dest=map(pathlib.Path,sys.argv[1:3]);skiko=pathlib.Path(sys.argv[3])
with zipfile.ZipFile(source) as z:entries={n:z.read(n) for n in z.namelist() if not n.endswith('/')}
key='natives/windows-x64/bridge.dll';d=bytearray(entries[key]);p=pefile.PE(data=d)
old=b'nebula/bridge/NativePassthrough';new=b'zip/nebula/loader/Loader';assert d.count(old)==1;d=d.replace(old,new+b'\0'*(len(old)-len(new)))
section=next(s for s in p.sections if s.Name.startswith(b'.edata'));cursor=section.Misc_VirtualSize
exp=p.DIRECTORY_ENTRY_EXPORT.struct
names=[]
for e in p.DIRECTORY_ENTRY_EXPORT.symbols:
 if not e.name:continue
 name=e.name
 if name==b'Java_nebula_bridge_FramebufferInterop_borrowNative':continue
 name={b'Java_nebula_bridge_NativeBridge_install':b'Java_zip_nebula_loader_Loader_installBridge',b'Java_nebula_bridge_WorkerPolicy_install':b'Java_zip_nebula_loader_Loader_wp_1install'}.get(name,name)
 if name!=e.name:
  assert cursor+len(name)+1<section.SizeOfRawData
  off=section.PointerToRawData+cursor;d[off:off+len(name)+1]=name+b'\0';rva=section.VirtualAddress+cursor;cursor+=len(name)+1
 else:rva=p.get_rva_from_offset(d.index(name+b'\0',section.PointerToRawData))
 names.append((name,rva,e.ordinal-exp.Base))
np=p.get_offset_from_rva(exp.AddressOfNames);op=p.get_offset_from_rva(exp.AddressOfNameOrdinals)
for i,(_,rva,ordinal) in enumerate(sorted(names)):
 struct.pack_into('<I',d,np+i*4,rva);struct.pack_into('<H',d,op+i*2,ordinal)
struct.pack_into('<I',d,exp.get_field_absolute_offset('NumberOfNames'),len(names))
struct.pack_into('<I',d,section.get_file_offset()+8,cursor)
entries[key]=bytes(d);entries['natives/windows-x64/skiko-windows-x64.dll']=skiko.read_bytes()
root='natives/windows-x64/'
hashes={n[len(root):]:hashlib.sha256(v).hexdigest() for n,v in entries.items() if n.startswith(root) and n.endswith(('.dll','.zip'))}
entries[root+'checksums.properties']=''.join(f'{n}={v}\n' for n,v in sorted(hashes.items())).encode()
entries[root+'skiko-windows-x64.dll.sha256']=hashes['skiko-windows-x64.dll'].encode()
with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for n,v in entries.items():z.writestr(n,v)
print(json.dumps({'jar':str(dest),'sha256':hashlib.file_digest(open(dest,'rb'),'sha256').hexdigest(),'native_hashes':hashes},indent=2))
