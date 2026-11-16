import java.lang.instrument.*;
import java.lang.reflect.*;
import java.util.*;
public class AnimationProbe {
 public static void premain(String arg,Instrumentation inst){Thread t=new Thread(()->{try{
  Class<?> cape=null;
  for(int tries=0;tries<120&&cape==null;tries++){for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("zip.nebula.client.IlIlIlIllIIlIlIlIlIlIllIlIIIIIIIIlllIllIIlIIIIlIIllIlIIIllIIllIl")){cape=c;break;}if(cape==null)Thread.sleep(1000);}
  if(cape==null)throw new AssertionError("Cape manager not loaded");
  Field reads=cape.getDeclaredField("cpDiskReads"),textures=cape.getDeclaredField("cpTextures"),owner=cape.getDeclaredField("cpOwner");for(Field f:List.of(reads,textures,owner))f.setAccessible(true);
  for(int i=0;i<120;i++){synchronized(cape){if(reads.getLong(null)>0&&textures.get(null)!=null&&((List<?>)textures.get(null)).size()>1)break;}Thread.sleep(1000);}
  long before;List<?> frames;Object id;synchronized(cape){before=reads.getLong(null);frames=new ArrayList<>((List<?>)textures.get(null));id=owner.get(null);}if(frames.size()<2)throw new AssertionError("Animated cape missing");
  Method get=cape.getDeclaredMethod("cpCape",java.util.UUID.class);get.setAccessible(true);Set<Object> observed=new HashSet<>();
  for(int i=0;i<100;i++){observed.add(get.invoke(null,id));Thread.sleep(100);}
  synchronized(cape){if(reads.getLong(null)!=before)throw new AssertionError("Unchanged state reread");if(!frames.equals(textures.get(null)))throw new AssertionError("Textures reallocated");}
  if(observed.size()<2)throw new AssertionError("Animation static");
  System.out.println("[AnimationProbe] PASS: "+observed.size()+" animated textures observed; zero state rereads or texture replacement over 10 seconds");
 }catch(Throwable e){System.err.println("[AnimationProbe] FAIL: "+e);e.printStackTrace();}},"animation-validation");t.setDaemon(true);t.start();}
}
