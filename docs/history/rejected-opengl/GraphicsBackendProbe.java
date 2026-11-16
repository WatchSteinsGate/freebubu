import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
public class GraphicsBackendProbe {
 public static void premain(String args,Instrumentation inst){Thread t=new Thread(()->{try{
  for(int tries=0;tries<150;tries++){
   Class<?> mc=null;for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("net.minecraft.client.Minecraft")){mc=c;break;}
   if(mc!=null){Object instance=mc.getMethod("getInstance").invoke(null);if(instance!=null){Object options=mc.getField("options").get(instance);if(options!=null){Field f=options.getClass().getDeclaredField("preferredGraphicsBackendFromStartup");f.setAccessible(true);Object selected=f.get(options);if(selected!=null){Object option=options.getClass().getMethod("preferredGraphicsBackend").invoke(options);Object saved=option.getClass().getMethod("get").invoke(option);Class<?> rs=Class.forName("com.mojang.blaze3d.systems.RenderSystem",false,mc.getClassLoader());Field d=rs.getDeclaredField("DEVICE");d.setAccessible(true);Object device=d.get(null);if(device==null){Thread.sleep(1000);continue;}Field b=device.getClass().getDeclaredField("backend");b.setAccessible(true);Object backend=b.get(device);if(!backend.getClass().getName().startsWith("com.mojang.blaze3d.opengl."))throw new AssertionError("Actual backend="+backend.getClass().getName());if(!saved.toString().equals("VULKAN"))throw new AssertionError("Saved preference changed: "+saved);System.out.println("[GraphicsBackendProbe] PASS: forced Vulkan launch selects OpenGL; saved Vulkan preference preserved");return;}}}}
   Thread.sleep(1000);
  }throw new AssertionError("Graphics startup timed out");
 }catch(Throwable e){System.err.println("[GraphicsBackendProbe] FAIL: "+e);e.printStackTrace();}},"graphics-backend-validation");t.setDaemon(true);t.start();}
}
