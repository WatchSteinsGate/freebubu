import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;
public class NativeDebugProbe {
 static Linker link=Linker.nativeLinker();
 static MethodHandle fn(long address,MemoryLayout result,MemoryLayout... args){return link.downcallHandle(MemorySegment.ofAddress(address),FunctionDescriptor.of(result,args));}
 public static void main(String[] args)throws Throwable{
  try(Arena a=Arena.ofConfined()){
   SymbolLookup dll=SymbolLookup.libraryLookup(args[0],a);long base=dll.find("cw_tbl").orElseThrow().address()-0x2055600;
   if((int)fn(base+0x209ea9c,JAVA_INT).invokeExact()!=0)throw new AssertionError("IsDebuggerPresent");
   MemorySegment flag=a.allocate(JAVA_INT);flag.set(JAVA_INT,0,1);
   int ok=(int)fn(base+0x209eafc,JAVA_INT,ADDRESS,ADDRESS).invokeExact(MemorySegment.ofAddress(-1),flag);
   if(ok!=1||flag.get(JAVA_INT,0)!=0)throw new AssertionError("Remote debugger flag");
   SymbolLookup kernel=SymbolLookup.libraryLookup("kernel32.dll",a);
   MethodHandle mod=link.downcallHandle(kernel.find("GetModuleHandleA").orElseThrow(),FunctionDescriptor.of(ADDRESS,ADDRESS));
   MemorySegment nt=(MemorySegment)mod.invokeExact(a.allocateFrom("ntdll.dll"));
   MethodHandle get=fn(base+0x2d22200,ADDRESS,ADDRESS,ADDRESS);
   MemorySegment query=(MemorySegment)get.invokeExact(nt,a.allocateFrom("NtQueryInformationProcess"));
   if(query.address()!=base+0x2d22000)throw new AssertionError("Query lookup routing");
   MethodHandle q=link.downcallHandle(query,FunctionDescriptor.of(JAVA_INT,ADDRESS,JAVA_INT,ADDRESS,JAVA_INT,ADDRESS));
   MemorySegment out=a.allocate(48),len=a.allocate(JAVA_INT),process=MemorySegment.ofAddress(-1);
   for(int type:new int[]{7,30,31}){
    out.fill((byte)0x7f);int status=(int)q.invokeExact(process,type,out,48,len);
    int expected=type==30?0xc0000353:0;
    if(status!=expected||out.get(JAVA_INT,0)!=(type==31?1:0))throw new AssertionError("Debug query "+type);
   }
   int status=(int)q.invokeExact(process,0,out,48,len);if(status!=0)throw new AssertionError("Normal process query delegation: "+status);
   status=(int)fn(base+0x2d22400,JAVA_INT,ADDRESS,JAVA_INT,ADDRESS,JAVA_INT).invokeExact(MemorySegment.ofAddress(-2),17,MemorySegment.NULL,0);
   if(status!=0)throw new AssertionError("Thread hide no-op");
   MemorySegment normal=(MemorySegment)get.invokeExact(nt,a.allocateFrom("NtSetInformationThread"));
   if(normal.address()!=SymbolLookup.libraryLookup("ntdll.dll",a).find("NtSetInformationThread").orElseThrow().address())throw new AssertionError("Other symbol lookup changed");
   for(long rva:new long[]{0x29ca,0x2a08}){MemorySegment bytes=MemorySegment.ofAddress(base+rva).reinterpret(5);if(bytes.get(JAVA_BYTE,0)!=(byte)0x31||bytes.get(JAVA_BYTE,1)!=(byte)0xc0)throw new AssertionError("Startup DACL call still present");}
   System.out.println("PASS: native debugger flags neutralized; debug query routing and thread-hide no-op work; startup DACL restrictions removed; normal process queries, security APIs and symbol lookup preserved");
  }
 }
}
