import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
public class ClassLookupTrace {
 public static void main(String[] args)throws Exception{
  var c=Bootstrap.virtualMachineManager().attachingConnectors().stream().filter(x->x.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();var a=c.defaultArguments();a.get("hostname").setValue("127.0.0.1");a.get("port").setValue("5005");a.get("timeout").setValue("10000");var vm=c.attach(a);
  try{
   for(String name:new String[]{"java.lang.ClassNotFoundException","java.lang.NoClassDefFoundError"})for(var type:vm.classesByName(name)){var r=vm.eventRequestManager().createExceptionRequest(type,true,true);r.setSuspendPolicy(EventRequest.SUSPEND_NONE);r.enable();}
   long until=System.currentTimeMillis()+25000;
   while(System.currentTimeMillis()<until){var set=vm.eventQueue().remove(1000);if(set==null)continue;for(var e:set)if(e instanceof ExceptionEvent x){var f=x.exception().referenceType().fieldByName("detailMessage");var v=x.exception().getValue(f);if(v instanceof StringReference s&&(s.value().contains("zip.nebula.loader")||s.value().contains("zip/nebula/loader")))System.out.println(x.exception().referenceType().name()+": "+s.value());}set.resume();}
  }finally{vm.dispose();}
 }
}
