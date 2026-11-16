package nebula.bridge;
import java.io.*;
import java.lang.invoke.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Build template; methods become members of the original obfuscated resolver. */
public final class BootstrapData {
 private static final Map<String,Long> VALUES=load();
 private static String key(String owner,String name,long a,long b){return owner+'\0'+name+'\0'+a+'\0'+b;}
 private static String text(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>1000000)throw new IOException("length");return new String(in.readNBytes(n),StandardCharsets.UTF_8);}
 private static Map<String,Long> load(){
  try(var in=new DataInputStream(BootstrapData.class.getResourceAsStream("/assets/nebula/bootstrap.bin"))){
   if(in.readInt()!=1312965425)throw new IOException("magic");int count=in.readInt();if(count<0||count>200000)throw new IOException("count");
   var out=new HashMap<String,Long>();for(int i=0;i<count;i++){String k=key(text(in),text(in),in.readLong(),in.readLong());long value=in.readLong();Long prior=out.put(k,value);if(prior!=null&&prior!=value)throw new IOException("conflict");}return Map.copyOf(out);
  }catch(IOException e){throw new ExceptionInInitializerError(e);}
 }
 public static CallSite bootstrap(MethodHandles.Lookup lookup,String name,MethodType type,long a,long b){
  String owner=lookup.lookupClass().getName().replace('.','/');Long value=VALUES.get(key(owner,name,a,b));
  if(value==null)throw new BootstrapMethodError("No captured constant for "+owner+"; refusing to guess");
  if(!type.equals(MethodType.methodType(long.class)))throw new BootstrapMethodError("Unexpected captured constant type");
  return new ConstantCallSite(MethodHandles.constant(long.class,value));
 }
}
