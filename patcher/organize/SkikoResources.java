import java.nio.file.*;
import org.objectweb.asm.*;
public class SkikoResources {
 public static void main(String[] args)throws Exception {
  ClassReader cr=new ClassReader(Files.readAllBytes(Path.of(args[0])));ClassWriter cw=new ClassWriter(0);
  cr.accept(new ClassVisitor(Opcodes.ASM9,cw){
   public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return new MethodVisitor(Opcodes.ASM9,super.visitMethod(a,n,d,s,e)){
    public void visitMethodInsn(int o,String owner,String name,String desc,boolean itf){
     if(owner.equals("java/lang/Class")&&(name.equals("getResourceAsStream")||name.equals("getResource"))){super.visitMethodInsn(Opcodes.INVOKESTATIC,"org/jetbrains/skiko/LibraryLoader","nebulaResource","(Ljava/lang/String;)Ljava/lang/String;",false);}
     super.visitMethodInsn(o,owner,name,desc,itf);
    }
   };}
   public void visitEnd(){
    MethodVisitor m=super.visitMethod(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC,"nebulaResource","(Ljava/lang/String;)Ljava/lang/String;",null,null);m.visitCode();
    // This loader only reads Skiko and ICU packaged resources.
    m.visitLdcInsn("/natives/windows-x64/");m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.ICONST_1);m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","substring","(I)Ljava/lang/String;",false);m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/String","concat","(Ljava/lang/String;)Ljava/lang/String;",false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(3,1);m.visitEnd();super.visitEnd();
   }
  },0);Files.write(Path.of(args[1]),cw.toByteArray());
 }
}
