import com.sun.jdi.*;
public class GraphicsBackendInspect {
 static Value field(ObjectReference o,String name){return o.getValue(o.referenceType().fieldByName(name));}
 static String name(Value v){return ((StringReference)field((ObjectReference)v,"name")).value();}
 public static void main(String[] args)throws Exception{
  var connector=Bootstrap.virtualMachineManager().attachingConnectors().stream().filter(c->c.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();var options=connector.defaultArguments();options.get("hostname").setValue("127.0.0.1");options.get("port").setValue("5005");options.get("timeout").setValue("10000");VirtualMachine vm=connector.attach(options);
  try{
   ReferenceType rs=vm.classesByName("com.mojang.blaze3d.systems.RenderSystem").getFirst();ObjectReference device=(ObjectReference)rs.getValue(rs.fieldByName("DEVICE"));ObjectReference backend=(ObjectReference)field(device,"backend");String type=backend.referenceType().name();if(!type.startsWith("com.mojang.blaze3d.opengl."))throw new AssertionError(type);
   ReferenceType mc=vm.classesByName("net.minecraft.client.Minecraft").getFirst();ObjectReference instance=(ObjectReference)mc.getValue(mc.fieldByName("instance"));ObjectReference settings=(ObjectReference)field(instance,"options");ObjectReference option=(ObjectReference)field(settings,"preferredGraphicsBackend");String saved=name(field(option,"value"));if(!saved.equals("VULKAN"))throw new AssertionError("Saved preference changed: "+saved);
   System.out.println("PASS: explicit Vulkan launch uses "+type+"; saved graphics preference remains "+saved);
  }finally{vm.dispose();}
 }
}
