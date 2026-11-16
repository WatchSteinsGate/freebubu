import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
public class ProbeAgent {
 public static void premain(String arg,Instrumentation inst){Thread thread=new Thread(()->{
  for(int tries=0;tries<120;tries++){
   try{
    Class<?> profile=null;for(Class<?> c:inst.getAllLoadedClasses()){
     String n=c.getName();if(n.equals("thatonecatgif.LocalStatsMod")||n.equals("thatonecatgif.FreeCosmeticsLOL.Editor")||n.startsWith("thatonecatgif.mixins."))throw new AssertionError("Removed helper loaded: "+n);
     if(n.equals("zip.nebula.client.lIIlIIllIlIIllllllIlllIlllIIllIlllIlIIlIIIllIllIIIllIlIIlllIIIlI"))profile=c;
    }
    if(profile!=null){Field owner=profile.getDeclaredField("spOwner");owner.setAccessible(true);if(owner.get(null)!=null){Object snapshot=profile.getMethod("spSnapshot").invoke(null);Class<?> continuation=Class.forName("kotlin.coroutines.Continuation",false,profile.getClassLoader());Method fetch=profile.getDeclaredMethod("lIllllllIllllIlIIIIIIlIIllIIIIllllIIlIIlIIIlIllIIIllIIllIIIIIIll",String.class,continuation);fetch.setAccessible(true);Object instance=profile.getField("lIIlIIIIlIlllIlIlIlIIIIllIIIllIIlllIIIIIIlIlIllIlIlIIlIIlIlIlIll").get(null);Object result=fetch.invoke(instance,"probe",null);if(!result.getClass().getName().equals("zip.nebula.Statistics"))throw new AssertionError("Wrong fetch result");for(String getter:new String[]{"getTotalProfit","getTotalExpGained","getTotalTimeMacroed","getTotalMacroStarts","getTotalFailsafes"}){Object a=result.getClass().getMethod(getter).invoke(result),b=snapshot.getClass().getMethod(getter).invoke(snapshot);if(!a.equals(b))throw new AssertionError("Mismatched "+getter);}System.out.println("[PatchProbe] PASS: original stats fetch returns persisted statistics; removed helpers/mixins are absent");return;}}
    Thread.sleep(1000);
   }catch(Throwable e){System.err.println("[PatchProbe] FAIL: "+e);e.printStackTrace();return;}
  }System.err.println("[PatchProbe] FAIL: stats initialization timed out");
 },"patch-validation");thread.setDaemon(true);thread.start();}
}
