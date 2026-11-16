import com.sun.jdi.*;
public class InspectKeys {
 public static void main(String[] args)throws Exception{
  var c=Bootstrap.virtualMachineManager().attachingConnectors().stream().filter(x->x.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();var a=c.defaultArguments();a.get("hostname").setValue("127.0.0.1");a.get("port").setValue("5005");var vm=c.attach(a);
  try{for(var name:new String[]{"thatonecatgif.loader.Loader","zip.nebula.loader.Key","zip.nebula.loader.Loader"})for(var type:vm.classesByName(name)){
   var f=type.fieldByName("key");Value v=f==null?null:type.getValue(f);System.out.println(name+" loader="+type.classLoader().referenceType().name()+" key="+(v==null?"absent":v instanceof StringReference s?"present length="+s.value().length():"non-string"));
  }}finally{vm.dispose();}
 }
}
