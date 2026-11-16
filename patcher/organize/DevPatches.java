import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class DevPatches {
 public static void main(String[] args)throws Exception{
  ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(c,0);int count=0;
  for(MethodNode m:c.methods)if(m.name.equals("check$okhttp")&&m.desc.equals("(Ljava/lang/String;Lkotlin/jvm/functions/Function0;)V")){
   m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();m.visibleLocalVariableAnnotations=null;m.invisibleLocalVariableAnnotations=null;m.instructions.add(new InsnNode(Opcodes.RETURN));m.maxStack=0;m.maxLocals=3;count++;
  }
  if(count!=1)throw new IllegalStateException("Expected one OkHttp pin-verification method, found "+count);
  ClassWriter cw=new ClassWriter(0);c.accept(cw);Files.write(Path.of(args[1]),cw.toByteArray());
 }
}
