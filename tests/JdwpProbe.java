import com.sun.jdi.*;
public class JdwpProbe {
 public static void main(String[] args)throws Exception{
  var connector=Bootstrap.virtualMachineManager().attachingConnectors().stream().filter(c->c.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();var options=connector.defaultArguments();options.get("hostname").setValue("127.0.0.1");options.get("port").setValue("5005");options.get("timeout").setValue("10000");VirtualMachine vm=connector.attach(options);try{if(vm.allThreads().isEmpty())throw new AssertionError("No threads");System.out.println("PASS: JDWP attached; VM threads and version accessible: "+vm.version());}finally{vm.dispose();}
 }
}
