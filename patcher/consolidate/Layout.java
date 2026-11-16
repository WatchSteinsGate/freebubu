import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
/** Build tool: relocate resources while preserving both native-facing Java class identities. */
public class Layout {
 static String rename(String s){
  if(s.equals("/assets/nebula/\u0001"))return "/thatonecatgif/data/\u0001";
  s=s.replace("assets/nebula/world-offset.accesswidener","META-INF/world-offset.accesswidener");
  for(String file:List.of("bootstrap.bin","foraging-trees.dat","metal-pipe.wav","scream.wav","kuudra-lego.wav"))s=s.replace("assets/nebula/"+file,"thatonecatgif/data/"+file);
  return s.replace("natives/windows-x64/","thatonecatgif/native/windows-x64/");
 }
 public static void main(String[] args)throws Exception{
  try(JarFile input=new JarFile(args[0]);JarOutputStream out=new JarOutputStream(Files.newOutputStream(Path.of(args[1])))){
   for(JarEntry entry:Collections.list(input.entries())){
    String old=entry.getName();if(old.startsWith("zip/nebula/loader/Loader$$TypeSwitch"))continue;
    String name=rename(old);byte[] bytes=input.getInputStream(entry).readAllBytes();
    if(old.endsWith(".class")){
     String raw=new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1);
     if(!rename(raw).equals(raw)){
      ClassNode c=new ClassNode();new ClassReader(bytes).accept(new ClassRemapper(c,new Remapper(Opcodes.ASM9){
       public String map(String n){return rename(n);}
       public Object mapValue(Object v){if(v instanceof String s)return rename(s);return super.mapValue(v);}
      }),0);
      ClassWriter w=new ClassWriter(0);c.accept(w);bytes=w.toByteArray();
     }
    }else if(old.endsWith(".json")||old.endsWith(".properties"))bytes=rename(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    out.putNextEntry(new JarEntry(name));out.write(bytes);out.closeEntry();
   }
  }
 }
}
