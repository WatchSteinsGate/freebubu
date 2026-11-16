; Win64 leaf functions; no stack allocation or nonvolatile-register changes.
; query @ code+0; getproc @ code+0x200; thread @ code+0x400.

cmp edx,7
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
ret

mov rax,rdx
shr rax,16
jz fallback
cmp byte ptr [rdx+0],78
jne fallback
cmp byte ptr [rdx+1],116
jne fallback
cmp byte ptr [rdx+2],81
jne fallback
cmp byte ptr [rdx+3],117
jne fallback
cmp byte ptr [rdx+4],101
jne fallback
cmp byte ptr [rdx+5],114
jne fallback
cmp byte ptr [rdx+6],121
jne fallback
cmp byte ptr [rdx+7],73
jne fallback
cmp byte ptr [rdx+8],110
jne fallback
cmp byte ptr [rdx+9],102
jne fallback
cmp byte ptr [rdx+10],111
jne fallback
cmp byte ptr [rdx+11],114
jne fallback
cmp byte ptr [rdx+12],109
jne fallback
cmp byte ptr [rdx+13],97
jne fallback
cmp byte ptr [rdx+14],116
jne fallback
cmp byte ptr [rdx+15],105
jne fallback
cmp byte ptr [rdx+16],111
jne fallback
cmp byte ptr [rdx+17],110
jne fallback
cmp byte ptr [rdx+18],80
jne fallback
cmp byte ptr [rdx+19],114
jne fallback
cmp byte ptr [rdx+20],111
jne fallback
cmp byte ptr [rdx+21],99
jne fallback
cmp byte ptr [rdx+22],101
jne fallback
cmp byte ptr [rdx+23],115
jne fallback
cmp byte ptr [rdx+24],115
jne fallback
cmp byte ptr [rdx+25],0
jne fallback
lea rax,[rip+0]
ret
fallback: jmp qword ptr [rip+0]

cmp edx,17
jne fallback
xor eax,eax
ret
fallback: jmp qword ptr [rip+0]
