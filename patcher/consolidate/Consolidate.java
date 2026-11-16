import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;
import com.google.gson.*;
/** Build-time relocation of helper members into classes already present in the mod. */
public class Consolidate implements Opcodes {
 static final String L="zip/nebula/loader/Loader",R="zip/nebula/client/IlllIIIIIIlllIIIllIlIIllllIllllllIlllIllIlllIIllIIIIlllllIlIllIl",N="zip/nebula/client/NebulaClient",D="dev/obliqa/L36xKgeb";
 static final Map<String,String> targets=new LinkedHashMap<>(),prefix=new HashMap<>();
 static void add(String helper,String target,String p){targets.put("nebula/bridge/"+helper,target);prefix.put("nebula/bridge/"+helper,p);}
 static String member(String owner,String name){if(!targets.containsKey(owner)||name.startsWith("<"))return name;if(owner.endsWith("NativeBridge"))return "installBridge";if(owner.endsWith("NativePassthrough"))return "refresh";if(owner.endsWith("LocalStartupAsset")&&name.equals("read"))return name;return prefix.get(owner)+name;}
 static final Remapper mapper=new Remapper(ASM9){
  public String map(String name){return targets.getOrDefault(name,name);}
  public String mapMethodName(String owner,String name,String desc){return member(owner,name);}
  public String mapFieldName(String owner,String name,String desc){return member(owner,name);}
  public Object mapValue(Object value){if(value instanceof String s){for(var e:targets.entrySet())if(s.equals(e.getKey().replace('/','.')))return e.getValue().replace('/','.');}return super.mapValue(value);}
 };
 static ClassNode node(byte[] data,boolean remap){ClassNode c=new ClassNode();new ClassReader(data).accept(remap?new ClassRemapper(c,mapper):c,0);return c;}
 static byte[] write(ClassNode c){ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 public static void main(String[] args)throws Exception{
  add("RuntimeFiles",L,"rf_");add("WorkerPolicy",L,"wp_");add("NativeBridge",L,"");add("NativePassthrough",L,"");
  add("BootstrapData",R,"bd_");add("TimeProxy",R,"tp_");add("ValueBootstrap",R,"vb_");add("LocalProfile",R,"lp_");
  add("Startup",N,"ns_");add("NativeCore",D,"nc_");add("Assets",L,"as_");add("LocalStartupAsset",L,"");add("GuiScale",L,"gs_");
  Map<String,byte[]> entries=new LinkedHashMap<>();try(JarFile j=new JarFile(args[0])){for(var e:Collections.list(j.entries()))entries.put(e.getName(),j.getInputStream(e).readAllBytes());}
  entries.put(L+".class",Files.readAllBytes(Path.of(args[2],L+".class")));
  entries.put("nebula/bridge/BootstrapData.class",Files.readAllBytes(Path.of(args[2],"nebula/bridge/BootstrapData.class")));
  // Matching Linux Java facades already call Skiko's real JNI methods; replace the Windows adapters.
  try(JarFile linux=new JarFile(args[3])){for(String c:List.of("org/jetbrains/skia/GaneshInteropKt","org/jetbrains/skia/Image$Companion","org/jetbrains/skia/BackendTexture$Companion","org/jetbrains/skia/impl/Managed"))entries.put(c+".class",linux.getInputStream(linux.getJarEntry(c+".class")).readAllBytes());}
  Map<String,ClassNode> classes=new LinkedHashMap<>();
  for(var e:entries.entrySet())if(e.getKey().endsWith(".class")&&!e.getKey().startsWith("nebula/bridge/")&&!e.getKey().equals("NativeLoader.class")){String raw=new String(e.getValue(),java.nio.charset.StandardCharsets.ISO_8859_1);String name=e.getKey().substring(0,e.getKey().length()-6);if(targets.containsValue(name)||raw.contains("nebula/bridge/")||raw.contains("nebula.bridge."))classes.put(name,node(e.getValue(),true));}
  for(var e:targets.entrySet()){
   // These native declarations already live in the loader template.
   if(e.getKey().endsWith("NativeBridge")||e.getKey().endsWith("NativePassthrough"))continue;
   ClassNode source=node(entries.get(e.getKey()+".class"),true),target=classes.get(e.getValue());
   for(FieldNode f:source.fields){f.access=(f.access&~(ACC_PRIVATE|ACC_PROTECTED))|ACC_PUBLIC;target.fields.add(f);}
   for(MethodNode m:source.methods){
    if(m.name.equals("<init>"))continue;
    m.access=(m.access&~(ACC_PRIVATE|ACC_PROTECTED))|ACC_PUBLIC;
    if(m.name.equals("<clinit>")){
     // Run pure helper initialization before the original class initialization can call it.
     m.name=prefix.get(e.getKey())+"init";m.access=ACC_PUBLIC|ACC_STATIC;
     MethodNode cl=target.methods.stream().filter(x->x.name.equals("<clinit>")).findFirst().orElse(null);
     if(cl==null){cl=new MethodNode(ACC_STATIC,"<clinit>","()V",null,null);cl.instructions.add(new InsnNode(RETURN));target.methods.add(cl);}
     cl.instructions.insert(new MethodInsnNode(INVOKESTATIC,target.name,m.name,"()V",false));
     // Fields initialized by moved methods cannot retain JVM final-write restrictions.
     for(FieldNode f:source.fields)f.access&=~ACC_FINAL;
    }
    if(target.methods.stream().anyMatch(x->x.name.equals(m.name)&&x.desc.equals(m.desc)))throw new IllegalStateException("Duplicate "+target.name+"."+m.name);
    target.methods.add(m);
   }
  }
  // The reflected reader was merged with its original method name, read(String).
  int removed=0;for(String k:new ArrayList<>(entries.keySet()))if(k.startsWith("nebula/bridge/")||k.equals("NativeLoader.class")){entries.remove(k);removed++;}
  for(var e:classes.entrySet())entries.put(e.getKey()+".class",write(e.getValue()));
  JsonObject fabric=JsonParser.parseString(new String(entries.get("fabric.mod.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();JsonArray pre=new JsonArray();pre.add(L.replace('/','.'));fabric.getAsJsonObject("entrypoints").add("preLaunch",pre);entries.put("fabric.mod.json",new GsonBuilder().setPrettyPrinting().create().toJson(fabric).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  for(var e:entries.entrySet())if(e.getKey().endsWith(".class")){
   byte[] data=e.getValue();for(String h:targets.keySet())if(new String(data,java.nio.charset.StandardCharsets.ISO_8859_1).contains(h))throw new IllegalStateException("Dangling helper reference "+e.getKey()+" -> "+h);
  }
  try(JarOutputStream out=new JarOutputStream(Files.newOutputStream(Path.of(args[1])))){for(var e:entries.entrySet()){out.putNextEntry(new JarEntry(e.getKey()));out.write(e.getValue());out.closeEntry();}}
  System.out.println("Consolidated "+removed+" helper class files into existing classes; rewrote "+classes.size()+" existing classes");
 }
}
