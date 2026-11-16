import java.lang.instrument.*;
public class DebugAttachProbe {
 public static void premain(String arg,Instrumentation inst){System.out.println("[DebugAttachProbe] PID "+ProcessHandle.current().pid());}
 public static void agentmain(String arg,Instrumentation inst)throws Exception{
  for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("okhttp3.CertificatePinner")){if(!inst.isModifiableClass(c))throw new AssertionError("Cannot retransform pinner");inst.retransformClasses(c);break;}
  System.out.println("[DebugAttachProbe] PASS: dynamically attached Java agent; instrumentation available");
 }
 public static void main(String[] args)throws Exception{if(args[0].equals("--sleep")){System.out.println("[AttachControl] PID "+ProcessHandle.current().pid());Thread.sleep(120000);return;}com.sun.tools.attach.VirtualMachine vm=com.sun.tools.attach.VirtualMachine.attach(args[0]);try{vm.loadAgent(args[1],"");System.out.println("PASS: dynamic Java agent attachment");}finally{vm.detach();}}
}
