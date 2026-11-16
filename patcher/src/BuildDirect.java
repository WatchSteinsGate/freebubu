import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import com.google.gson.*;

public class BuildDirect implements Opcodes {
 static final String STATS="zip/nebula/client/lIIlIIllIlIIllllllIlllIlllIIllIlllIlIIlIIIllIllIIIllIlIIlllIIIlI";
 static final String CAPE="zip/nebula/client/IlIlIlIllIIlIlIlIlIlIllIlIIIIIIIIlllIllIIlIIIIlIIllIlIIIllIIllIl";
 static final String MACRO="zip/nebula/client/lIIlIlIIIlllIlIlIIllllIllIIIIIIlIlIlIIllIlIIIlIIIIIlllIllIlIlIIl";
 static ClassNode node(byte[] data){ClassNode c=new ClassNode();new ClassReader(data).accept(c,0);return c;}
 static byte[] write(ClassNode c){ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){protected String getCommonSuperClass(String a,String b){try{return super.getCommonSuperClass(a,b);}catch(Exception e){throw new IllegalStateException("Cannot resolve hierarchy: "+a+" and "+b,e);}}};c.accept(w);return w.toByteArray();}
 static void merge(ClassNode target,Path template,String name)throws Exception{
  ClassNode remapped=new ClassNode();new ClassReader(Files.readAllBytes(template)).accept(new ClassRemapper(remapped,new SimpleRemapper(name,target.name)),0);
  for(FieldNode field:remapped.fields){if(target.fields.stream().anyMatch(f->f.name.equals(field.name)))throw new IllegalStateException("Field collision "+field.name);target.fields.add(field);}
  for(MethodNode m:remapped.methods){if(m.name.equals("<init>"))continue;if(m.name.equals("<clinit>"))throw new IllegalStateException("Move initialization into explicit init");if(target.methods.stream().anyMatch(x->x.name.equals(m.name)&&x.desc.equals(m.desc)))throw new IllegalStateException("Method collision "+m.name);target.methods.add(m);}
  target.interfaces.add("net/fabricmc/api/ClientModInitializer");
  for(MethodNode m:target.methods)if(m.name.equals("<init>")&&m.desc.equals("()V"))m.access=(m.access&~(ACC_PRIVATE|ACC_PROTECTED))|ACC_PUBLIC;
 }
 static boolean removed(String n){return n.startsWith("thatonecatgif/FreeCosmeticsLOL/")&&!n.endsWith("editor.html")||n.startsWith("thatonecatgif/mixins/")||n.startsWith("thatonecatgif/warnings/")||n.startsWith("thatonecatgif/LocalStatsMod")||n.equals("thatonecatgif/LocalStatisticsStore.class")||n.equals("thatonecatgif/StatsPath.class")||n.equals("thatonecatgif/GainAccumulator.class")||n.startsWith("thatonecatgif/SerialWriteQueue")||n.equals("thatonecatgif/WarningFilter.class")||n.equals("thatonecatgif/CosmeticsMigration.class")||n.equals("thatonecatgif/mixins/statistics.json")||n.equals("thatonecatgif/mixins/warnings.json");}
 static void require(int actual,int expected,String what){if(actual!=expected)throw new IllegalStateException(what+" matches="+actual+", expected="+expected);}
 public static void main(String[] args)throws Exception{
  if(args.length!=3)throw new IllegalArgumentException("Usage: BuildDirect <base.jar> <project-root> <output.jar>"); Path root=Path.of(args[1]);Map<String,byte[]> entries=new LinkedHashMap<>();try(JarFile jar=new JarFile(args[0])){for(JarEntry e:Collections.list(jar.entries()))entries.put(e.getName(),jar.getInputStream(e).readAllBytes());}
  Map<String,Integer> audit=new LinkedHashMap<>();
  ClassNode stats=node(entries.get(STATS+".class"));merge(stats,root.resolve("build/classes/StatsPatch.class"),"StatsPatch");int changes=0;
  for(MethodNode m:stats.methods)if(m.name.equals("lIllllllIllllIlIIIIIIlIIllIIIIllllIIlIIlIIIlIllIIIllIIllIIIIIIll")&&m.desc.equals("(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;")){
   m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();m.instructions.add(new MethodInsnNode(INVOKESTATIC,STATS,"spSnapshot","()Lzip/nebula/Statistics;",false));m.instructions.add(new InsnNode(ARETURN));changes++;
  }require(changes,1,"Stats fetch replacement");audit.put("statsFetch",changes);entries.put(stats.name+".class",write(stats));
  String profit="zip/nebula/client/IIIIIIlllIIIlIIllIIllllIIlIIllIlIIIlllIlIIIllllIIlIIIlllIlIlIllI";ClassNode pc=node(entries.get(profit+".class"));changes=0;
  for(MethodNode m:pc.methods)if(m.name.equals("lIIIllIllIlIllIIlllllIIIlIIIIlIIIIIIlIlllIIllIllIIllIllIlIllIllI"))for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode call&&call.name.equals("llllIlIIIIIlIllIIlIlIllIIIlllIlIlIlIlIIIIIIIIIIlllllIlIllllIIIll")&&call.desc.equals("(D)V")){
   InsnList add=new InsnList();add.add(new VarInsnNode(ALOAD,7));add.add(new VarInsnNode(DLOAD,4));add.add(new MethodInsnNode(INVOKESTATIC,STATS,"spProfit","(L"+MACRO+";D)V",false));m.instructions.insert(i,add);changes++;
  }require(changes,1,"Profit hook");audit.put("profit",changes);entries.put(pc.name+".class",write(pc));
  String xp="zip/nebula/client/llIlIIIlIIlIIllIIlIIIIlllIllIIIIIllIllIIIIIlIIIIIIIIIllIIIIlIIII";ClassNode xc=node(entries.get(xp+".class"));changes=0;
  for(MethodNode m:xc.methods)if(m.name.equals("llIlIlIIIlIIIlIlIIllllIIIIlIIlIlIlIlIllllIIlIIllllIIIIllIIlIlIll"))for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof FieldInsnNode f&&f.getOpcode()==PUTSTATIC&&f.owner.equals(xp)&&f.name.equals("IlllllIllllIIIIllllllllIIllIIlIIlIlIllIllIlllllIIIIlllIlllIlIllI")){
   InsnList add=new InsnList();add.add(new VarInsnNode(DLOAD,4));add.add(new MethodInsnNode(INVOKESTATIC,STATS,"spXp","(D)V",false));m.instructions.insert(i,add);changes++;
  }require(changes,1,"XP hook");audit.put("xp",changes);entries.put(xc.name+".class",write(xc));
  String fails="zip/nebula/lIlllIlIIllIIllIIIIlIIlIlllllIIIlllllIlllllIIlIIIIllllIllIIIlllI";ClassNode fc=node(entries.get(fails+".class"));changes=0;
  for(MethodNode m:fc.methods)if(m.name.equals("<init>")&&m.desc.equals("(Ljava/lang/String;Lkotlin/time/Instant;Ljava/lang/String;)V"))for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==RETURN){InsnList add=new InsnList();add.add(new VarInsnNode(ALOAD,1));add.add(new MethodInsnNode(INVOKESTATIC,STATS,"spFailsafe","(Ljava/lang/String;)V",false));m.instructions.insertBefore(i,add);changes++;}
  require(changes,1,"Failsafe hook");audit.put("failsafe",changes);entries.put(fc.name+".class",write(fc));
  ClassNode cape=node(entries.get(CAPE+".class"));merge(cape,root.resolve("build/classes/CosmeticsPatch.class"),"CosmeticsPatch");changes=0;
  for(MethodNode m:cape.methods)if(m.name.equals("IIIIIIIllIlIlllllllIllIlIllIlIIIIIlIIlIllllIIIIIllIlIIIlllIIlIll")&&m.desc.equals("(Ljava/util/UUID;)Lnet/minecraft/resources/Identifier;")){
   InsnList add=new InsnList();LabelNode fallback=new LabelNode();add.add(new VarInsnNode(ALOAD,1));add.add(new MethodInsnNode(INVOKESTATIC,CAPE,"cpCape","(Ljava/util/UUID;)Lnet/minecraft/resources/Identifier;",false));add.add(new InsnNode(DUP));add.add(new JumpInsnNode(IFNULL,fallback));add.add(new InsnNode(ARETURN));add.add(fallback);add.add(new InsnNode(POP));m.instructions.insert(add);changes++;
  }require(changes,1,"Cape lookup");audit.put("capeLookup",changes);entries.put(cape.name+".class",write(cape));
  JsonObject mod=JsonParser.parseString(new String(entries.get("fabric.mod.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();JsonArray mixins=mod.getAsJsonArray("mixins"),kept=new JsonArray();for(JsonElement e:mixins)if(e.getAsString().equals("nebula.mixins.json"))kept.add(e);mod.add("mixins",kept);
  JsonArray clients=new JsonArray();clients.add(STATS.replace('/','.'));clients.add(CAPE.replace('/','.'));mod.getAsJsonObject("entrypoints").add("client",clients);mod.addProperty("description","Nebula for Windows with statistics and personal cosmetics.");entries.put("fabric.mod.json",new GsonBuilder().setPrettyPrinting().create().toJson(mod).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  entries.put("thatonecatgif/FreeCosmeticsLOL/editor.html",Files.readAllBytes(root.resolve("editor/editor.html")));
  int deleted=0;for(String n:new ArrayList<>(entries.keySet()))if(removed(n)){entries.remove(n);deleted++;}audit.put("removedAddonEntries",deleted);
  // Fail the build if original or retained helper bytecode still names a removed class.
  for(var e:entries.entrySet())if(e.getKey().endsWith(".class")){
   ClassReader r=new ClassReader(e.getValue());r.accept(new ClassVisitor(ASM9){public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){return new MethodVisitor(ASM9){public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){if(removed(owner+".class"))throw new IllegalStateException("Dangling removed class reference "+e.getKey()+" -> "+owner);}public void visitTypeInsn(int op,String type){if(removed(type+".class"))throw new IllegalStateException("Dangling removed type "+type);}};}},ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  }
  Path output=Path.of(args[2]);try(JarOutputStream jar=new JarOutputStream(Files.newOutputStream(output))){for(var e:entries.entrySet()){JarEntry item=new JarEntry(e.getKey());jar.putNextEntry(item);jar.write(e.getValue());jar.closeEntry();}}
  Files.writeString(root.resolve("build/patch-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(audit));System.out.println(audit);System.out.println(output);
 }
}
