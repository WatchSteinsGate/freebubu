package zip.nebula.loader;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
/** Template merged with extraction and bridge methods into the original loader class. */
public class Loader implements PreLaunchEntrypoint {
 private static ClassLoader payloadLoader;
 public static native void load();
 public static native void installBridge(Class<?> original,String path);
 public static native void refresh(long seconds);
 public static void loadPayload(String path){System.load(path);}
 public void onPreLaunch(){
  if(!System.getProperty("os.name").startsWith("Windows")||!Set.of("amd64","x86_64").contains(System.getProperty("os.arch")))throw new IllegalStateException("Nebula requires Windows x86-64");
  try{
   System.setProperty("log4j2.ignoreTCL",System.getProperty("log4j2.ignoreTCL","true"));
   var payload=nebula.bridge.RuntimeFiles.resolve("nebula.windowsPayload");
   String expected="5a0fb2750d9d78f07201f43c7962954ea66a15b90e56937c6a8f0e4d6f8a51c4";
   if(!expected.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(payload)))))throw new SecurityException("Windows payload checksum mismatch");
   System.load(nebula.bridge.RuntimeFiles.resolve("nebula.windowsBridge").toString());nebula.bridge.WorkerPolicy.installIfEnabled();
   payloadLoader=new URLClassLoader(new java.net.URL[]{Loader.class.getProtectionDomain().getCodeSource().getLocation()},FabricLoader.class.getClassLoader());
   Class.forName("zip.nebula.loader.Loader",true,payloadLoader).getMethod("loadPayload",String.class).invoke(null,payload.toString());
   Class<?> original=Class.forName("zip.nebula.client.IlllIIIIIIlllIIIllIlIIllllIllllllIlllIllIlllIIllIIIIlllllIlIllIl",false,getClass().getClassLoader());installBridge(original,payload.toString());
   Class<?> client=Class.forName("zip.nebula.client.NebulaClient",true,getClass().getClassLoader());client.getMethod("earlyLoad").invoke(client.getField("INSTANCE").get(null));
  }catch(Throwable e){throw new IllegalStateException("Windows bootstrap failed",e);}
 }
}
