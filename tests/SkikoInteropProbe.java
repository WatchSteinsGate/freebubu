import org.jetbrains.skia.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
public class SkikoInteropProbe {
 public static void main(String[] args){
  if(!glfwInit())throw new AssertionError("GLFW init");glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);long win=glfwCreateWindow(64,64,"interop test",0,0);if(win==0)throw new AssertionError("GL window");glfwMakeContextCurrent(win);org.lwjgl.opengl.GL.createCapabilities();
  int texture=glGenTextures();glBindTexture(GL_TEXTURE_2D,texture);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,32,32,0,GL_RGBA,GL_UNSIGNED_BYTE,0L);
  try(DirectContext context=DirectContext.Companion.makeGL();BackendTexture backend=BackendTexture.Companion.makeGL(32,32,false,texture,GL_TEXTURE_2D,GL_RGBA8)){
   try(Surface surface=GaneshInteropKt.makeFromBackendTexture(Surface.Companion,context,backend,SurfaceOrigin.BOTTOM_LEFT,SurfaceColorFormat.RGBA_8888,null,0,null)){
    if(surface==null)throw new AssertionError("Native texture surface");surface.getCanvas().clear(0xff13579b);context.flushAndSubmit(surface,true);
    try(Image image=Image.Companion.borrowTextureFrom(context,backend,SurfaceOrigin.BOTTOM_LEFT,ColorType.RGBA_8888,ColorAlphaType.PREMUL,null)){if(image==null||image.getWidth()!=32)throw new AssertionError("Borrowed image");}
    java.nio.ByteBuffer pixels=org.lwjgl.BufferUtils.createByteBuffer(32*32*4);glGetTexImage(GL_TEXTURE_2D,0,GL_RGBA,GL_UNSIGNED_BYTE,pixels);if((pixels.get(0)&255)!=0x13||(pixels.get(1)&255)!=0x57||(pixels.get(2)&255)!=0x9b)throw new AssertionError("Rendered pixel mismatch");
   }
  }finally{glDeleteTextures(texture);glfwDestroyWindow(win);glfwTerminate();}
  System.out.println("PASS: native GL surface and borrowed image interop; rendered texture pixels verified");
 }
}
