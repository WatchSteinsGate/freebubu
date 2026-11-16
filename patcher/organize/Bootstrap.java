package thatonecatgif;
import java.io.*;
import java.security.*;
import java.util.*;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
public final class Bootstrap implements PreLaunchEntrypoint {
 private static ClassLoader payloadLoader;
 public void onPreLaunch(){
  if(!System.getProperty("os.name").startsWith("Windows"))throw new IllegalStateException("Nebula requires Windows x86-64");
  try{
   if(!Set.of("amd64","x86_64").contains(System.getProperty("os.arch")))throw new IllegalStateException("Nebula requires x86-64");
   System.setProperty("log4j2.ignoreTCL",System.getProperty("log4j2.ignoreTCL","true"));
   var payload=RuntimeFiles.resolve("nebula.windowsPayload");
   String expected="599252d7a411b1d469398e3d5c32819f5a2c38fde607075095e6f86e2e34daf4";
   if(!expected.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(payload)))))throw new SecurityException("Windows payload checksum mismatch");
   System.load(RuntimeFiles.resolve("nebula.windowsBridge").toString());WorkerPolicy.installIfEnabled();
   payloadLoader=new ClassLoader(ClassLoader.getPlatformClassLoader()){
    protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
     if(name.equals("NativeLoader")||name.equals("zip.nebula.loader.Loader"))synchronized(getClassLoadingLock(name)){
      Class<?> c=findLoadedClass(name);if(c==null)try(InputStream in=Bootstrap.class.getResourceAsStream("/"+name.replace('.','/')+".class")){if(in==null)throw new ClassNotFoundException(name);byte[] bytes=in.readAllBytes();c=defineClass(name,bytes,0,bytes.length);}catch(IOException e){throw new ClassNotFoundException(name,e);}if(resolve)resolveClass(c);return c;
     }return super.loadClass(name,resolve);
    }
   };
   Class.forName("NativeLoader",true,payloadLoader).getMethod("load",String.class).invoke(null,payload.toString());
   Class<?> original=Class.forName("zip.nebula.client.IlllIIIIIIlllIIIllIlIIllllIllllllIlllIllIlllIIllIIIIlllllIlIllIl",false,getClass().getClassLoader());NativeBridge.install(original,payload.toString());
   Class<?> client=Class.forName("zip.nebula.client.NebulaClient",true,getClass().getClassLoader());client.getMethod("earlyLoad").invoke(client.getField("INSTANCE").get(null));
  }catch(Throwable e){throw new IllegalStateException("Windows bootstrap failed",e);}
 }
}
