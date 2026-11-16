import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** Build-time patch for the supplied Windows Skia library, which has no Vulkan JNI. */
public final class GraphicsCompatibility implements Opcodes {
 public static final String TARGET="zip/nebula/client/IlllIlIIllllIIIIllIIlllIIlIIlIlIlIllIlIIIIlIlIIlIllIllIIIllIIIII";
 static final String CONFIG="net/minecraft/client/main/GameConfig",GAME=CONFIG+"$GameData",API="net/minecraft/client/PreferredGraphicsApi";
 static void field(InsnList out,String name,String descriptor){out.add(new VarInsnNode(ALOAD,0));out.add(new FieldInsnNode(GETFIELD,CONFIG,"game","L"+GAME+";"));out.add(new FieldInsnNode(GETFIELD,GAME,name,descriptor));}
 public static void main(String[] args)throws Exception{
  ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(c,0);
  if(!c.name.equals(TARGET))throw new IllegalStateException("Wrong startup class: "+c.name);int changes=0;
  for(MethodNode m:c.methods)if(m.name.equals("IlIIlIlIlllIlIIlIIIIIIllllIIlllIllllllIIlIIIlIIIIlIllIlIlIlIIlII")&&m.desc.equals("(L"+CONFIG+";)Lnet/minecraft/client/Minecraft;")){
   if((m.access&ACC_STATIC)==0)throw new IllegalStateException("Startup factory must be static");
   InsnList p=new InsnList();LabelNode ready=new LabelNode();
   field(p,"forcedGraphicsApi","L"+API+";");p.add(new FieldInsnNode(GETSTATIC,API,"OPENGL","L"+API+";"));p.add(new JumpInsnNode(IF_ACMPEQ,ready));
   p.add(new TypeInsnNode(NEW,CONFIG));p.add(new InsnNode(DUP));
   for(String[] f:new String[][]{{"user","L"+CONFIG+"$UserData;"},{"display","Lcom/mojang/blaze3d/platform/DisplayData;"},{"location","L"+CONFIG+"$FolderData;"}}){p.add(new VarInsnNode(ALOAD,0));p.add(new FieldInsnNode(GETFIELD,CONFIG,f[0],f[1]));}
   p.add(new TypeInsnNode(NEW,GAME));p.add(new InsnNode(DUP));
   for(String[] f:new String[][]{{"demo","Z"},{"launchVersion","Ljava/lang/String;"},{"versionType","Ljava/lang/String;"},{"disableMultiplayer","Z"},{"disableChat","Z"},{"captureTracyImages","Z"},{"vulkanValidation","Z"},{"renderDebugLabels","Z"}})field(p,f[0],f[1]);
   p.add(new FieldInsnNode(GETSTATIC,API,"OPENGL","L"+API+";"));field(p,"offlineDeveloperMode","Z");
   p.add(new MethodInsnNode(INVOKESPECIAL,GAME,"<init>","(ZLjava/lang/String;Ljava/lang/String;ZZZZZL"+API+";Z)V",false));
   p.add(new VarInsnNode(ALOAD,0));p.add(new FieldInsnNode(GETFIELD,CONFIG,"quickPlay","L"+CONFIG+"$QuickPlayData;"));
   p.add(new MethodInsnNode(INVOKESPECIAL,CONFIG,"<init>","(L"+CONFIG+"$UserData;Lcom/mojang/blaze3d/platform/DisplayData;L"+CONFIG+"$FolderData;L"+GAME+";L"+CONFIG+"$QuickPlayData;)V",false));
   p.add(new VarInsnNode(ASTORE,0));p.add(new FieldInsnNode(GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;"));p.add(new LdcInsnNode("[Nebula] Using OpenGL: the packaged Windows Skia library has no Vulkan bindings."));p.add(new MethodInsnNode(INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false));p.add(ready);
   m.instructions.insert(p);changes++;
  }
  if(changes!=1)throw new IllegalStateException("Expected one existing startup factory, found "+changes);
  ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(cw);Files.write(Path.of(args[1]),cw.toByteArray());System.out.println("Patched existing startup factory: OpenGL selected for Windows Skia");
 }
}
