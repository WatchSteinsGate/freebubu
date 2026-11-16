#!/usr/bin/env python3
"""Generate the reviewed, position-independent Windows debug patch plan."""
import sys,pathlib,struct,json,hashlib,re
sys.path.insert(0,str(pathlib.Path('.cache/native-tools').resolve()))
import pefile
from keystone import Ks,KS_ARCH_X86,KS_MODE_64
src=pathlib.Path('.cache/native-analysis/client.dll');d=src.read_bytes();p=pefile.PE(data=d);base=p.OPTIONAL_HEADER.ImageBase
code_rva=p.OPTIONAL_HEADER.SizeOfImage;data_rva=code_rva+0x1000
query='''cmp edx,7
je q_port
cmp edx,30
je q_object
cmp edx,31
je q_flags
fallback: jmp qword ptr [rip+0]
q_port: cmp r9d,8
jb fallback
test r8,r8
jz fallback
mov qword ptr [r8],0
mov rax,qword ptr [rsp+40]
test rax,rax
jz port_done
mov dword ptr [rax],8
port_done: xor eax,eax
ret
q_object: cmp r9d,8
jb fallback
test r8,r8
jz fallback
mov qword ptr [r8],0
mov rax,qword ptr [rsp+40]
test rax,rax
jz object_done
mov dword ptr [rax],8
object_done: mov eax,0xc0000353
ret
q_flags: cmp r9d,4
jb fallback
test r8,r8
jz fallback
mov dword ptr [r8],1
mov rax,qword ptr [rsp+40]
test rax,rax
jz flags_done
mov dword ptr [rax],4
flags_done: xor eax,eax
ret'''
getproc='''mov rax,rdx
shr rax,16
jz fallback
'''
name=b'NtQueryInformationProcess\0'
for off,value in enumerate(name):getproc+=f'cmp byte ptr [rdx+{off}],{value}\njne fallback\n'
getproc+='lea rax,[rip+0]\nret\nfallback: jmp qword ptr [rip+0]'
thread='cmp edx,17\njne fallback\nxor eax,eax\nret\nfallback: jmp qword ptr [rip+0]'
ks=Ks(KS_ARCH_X86,KS_MODE_64)
def asm(text,off,links):

 try:b=bytearray(ks.asm(text,addr=code_rva+off)[0])
 except Exception as e:print('ASM ERROR offset',off,'count',getattr(e,'stat_count',None));print(text);raise
 for pattern,target in links:
  pos=b.index(pattern);struct.pack_into('<i',b,pos+len(pattern)-4,target-(code_rva+off+pos+len(pattern)))
 return b
code=bytearray(b'\xcc'*0x500)
for off,text,links in [(0,query,[(b'\xff\x25\0\0\0\0',data_rva+0x520)]),(0x200,getproc,[(b'\x48\x8d\x05\0\0\0\0',code_rva),(b'\xff\x25\0\0\0\0',0x2d170a0)]),(0x400,thread,[(b'\xff\x25\0\0\0\0',0x2d17b70)])]:
 b=asm(text,off,links);assert len(b)<0x200;code[off:off+len(b)]=b
imports=p.OPTIONAL_HEADER.DATA_DIRECTORY[1];old=p.get_data(imports.VirtualAddress,20*len(p.DIRECTORY_ENTRY_IMPORT))
data=bytearray(0x600);data[:len(old)]=old
struct.pack_into('<IIIII',data,len(old),data_rva+0x500,0,0,data_rva+0x550,data_rva+0x520)
for off in [0x500,0x520]:struct.pack_into('<QQ',data,off,data_rva+0x560,0)
data[0x550:0x550+10]=b'ntdll.dll\0';data[0x560:0x560+28]=b'\0\0NtQueryInformationProcess\0'
patches=[]
def patch(rva,new,why):
 off=p.get_offset_from_rva(rva);patches.append({'rva':rva,'offset':off,'before':d[off:off+len(new)].hex(),'after':new.hex(),'reason':why})
patch(0x209ea9c,bytes.fromhex('31c0c39090909090'),'IsDebuggerPresent returns FALSE')
patch(0x209eafc,bytes.fromhex('31c08902ffc0c390'),'CheckRemoteDebuggerPresent writes FALSE and returns success')
patch(0x23859c8,b'\xe9'+struct.pack('<i',code_rva+0x400-(0x23859c8+5))+b'\x90','ThreadHideFromDebugger is a no-op; other thread information calls use the original API')
for rva in (0x29ca,0x2a08):patch(rva,bytes.fromhex('31c0909090'),'Disable JNI startup process-DACL restrictions (kernel-object DACL calls)')
for m in re.finditer(rb'\xff[\x15\x25]',d[:0x2645170]):
 off=m.start();rva=p.get_rva_from_offset(off);target=rva+6+struct.unpack_from('<i',d,off+2)[0]
 if target==0x2d170a0:
  op=b'\xe8' if d[off+1]==0x15 else b'\xe9'
  patch(rva,op+struct.pack('<i',code_rva+0x200-(rva+5))+b'\x90','Scope NtQueryInformationProcess debug classes; all other symbol lookups are unchanged')
plan={'input_sha256':hashlib.sha256(d).hexdigest(),'code_rva':code_rva,'data_rva':data_rva,'code_hex':code.hex(),'imports_hex':data.hex(),'import_directory_size':len(old)+40,'patches':patches}
pathlib.Path('patcher/native/debug-patch.json').write_text(json.dumps(plan,indent=2)+'\n')
pathlib.Path('patcher/native/debug-stubs.asm').write_text('; Win64 leaf functions; no stack allocation or nonvolatile-register changes.\n; query @ code+0; getproc @ code+0x200; thread @ code+0x400.\n\n'+query+'\n\n'+getproc+'\n\n'+thread+'\n')
print('Generated scoped debug patch:',len(patches),'guarded sites; new code RVA',hex(code_rva))
