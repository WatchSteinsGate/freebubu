import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import zip.nebula.Statistics;
import zip.nebula.client.IIIIIlIllIIIllIIlIllIlllIIIIIlIlIIlIIlllIlIlIlIlIIllIllIIllIIlII;
import zip.nebula.client.lIIlIlIIIlllIlIlIIllllIllIIIIIIlIlIlIIllIlIIIlIIIIIlllIllIlIlIIl;

/** Build-time template: merged into the original profile API class, never shipped as a class. */
public class StatsPatch implements ClientModInitializer {
 private static Properties spData;
 private static Path spFile;
 private static String spOwner,spActive;
 private static long spLast,spCheckpoint;
 private static ZoneId spZone;
 private static boolean spDirty,spRegistered;
 private static RuntimeException spError;
 public void onInitializeClient(){spInitialize();}
 public static synchronized void spInitialize(){
  if(spRegistered)return;spRegistered=true;
  ClientTickEvents.END_CLIENT_TICK.register(mc->{try{spTick(mc);}catch(Exception e){spError=new IllegalStateException("Statistics could not be updated",e);System.err.println("[Nebula] Statistics: "+e);}});
  ClientLifecycleEvents.CLIENT_STOPPING.register(mc->{synchronized(StatsPatch.class){try{if(spData!=null){spObserve(null,System.currentTimeMillis());spFlush();}}catch(Exception e){System.err.println("[Nebula] Could not save statistics: "+e);}}});
 }
 private static synchronized void spTick(Minecraft mc)throws Exception{
  String owner=mc.getUser().getProfileId().toString();
  if(!owner.equals(spOwner)){
   if(spData!=null){spObserve(null,System.currentTimeMillis());spFlush();}
   Path file=mc.gameDirectory.toPath().resolve("Nebula/stats").resolve(owner+".properties");
   Path old=mc.gameDirectory.toPath().resolve("nebula-local-statistics").resolve(owner+".properties");
   if(!Files.exists(file)&&Files.isRegularFile(old,LinkOption.NOFOLLOW_LINKS)){Files.createDirectories(file.getParent());Files.copy(old,file);}
   spOpen(file,owner);
  }
  lIIlIlIIIlllIlIlIIllllIllIIIIIIlIlIlIIllIlIIIlIIIIIlllIllIlIlIIl macro=IIIIIlIllIIIllIIlIllIlllIIIIIlIlIIlIIlllIlIlIlIlIIllIllIIllIIlII.IIlllIllIllIlIllllIlIllllIlIlIIlllIlllIllIIIllIIIIllIIlIlIlIllll.llIIIIlIllIllIlIllIllIIllllIlIIllllllllIIIIIlllIIIIlIIlllllllIIl();
  spObserve(macro==null?null:macro.IIllIlIlIIIIIllIlIIlllllIlllIlIIlIIIIlIIIIlIllIIllIlIlIlIIllIIII(),System.currentTimeMillis());
  if(System.nanoTime()>=spCheckpoint){spFlush();spCheckpoint=System.nanoTime()+10_000_000_000L;}
 }
 public static synchronized void spOpen(Path file,String owner)throws Exception{
  Properties data=new Properties();
  if(Files.exists(file,LinkOption.NOFOLLOW_LINKS)){
   if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Statistics must be a regular file");
   try(InputStream in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){data.load(in);}
   if(!"1".equals(data.getProperty("schema")))throw new IOException("Unsupported statistics schema");
  }else{data.setProperty("schema","1");data.setProperty("zone",ZoneId.systemDefault().getId());}
  spZone=ZoneId.of(data.getProperty("zone"));spData=data;spFile=file;spOwner=owner;spActive=null;spLast=0;spError=null;spDirty=false;
  // An interrupted session must not count time while the client was stopped.
  if(data.remove("active.macro")!=null){data.remove("active.last");spDirty=true;}
 }
 private static String spKey(String name){return Base64.getUrlEncoder().withoutPadding().encodeToString(name.getBytes(StandardCharsets.UTF_8));}
 private static String spName(String key){return new String(Base64.getUrlDecoder().decode(key),StandardCharsets.UTF_8);}
 private static void spAdd(String key,long delta){spData.setProperty(key,Long.toString(Math.addExact(Long.parseLong(spData.getProperty(key,"0")),delta)));spDirty=true;}
 public static synchronized void spObserve(String macro,long now){
  if(spData==null)throw new IllegalStateException("Statistics are still loading");
  if(spActive!=null){
   if(now<spLast)throw new IllegalArgumentException("Time moved backwards");
   long start=spLast;
   while(start<now){LocalDate day=Instant.ofEpochMilli(start).atZone(spZone).toLocalDate();long end=Math.min(now,day.plusDays(1).atStartOfDay(spZone).toInstant().toEpochMilli());
    if(end<=start)throw new IllegalStateException("Invalid day boundary");spAdd("time."+spKey(spActive),end-start);spAdd("daily."+day+"."+spKey(spActive),end-start);start=end;}
  }
  if(!Objects.equals(spActive,macro)&&macro!=null)spAdd("starts."+spKey(macro),1);
  spActive=macro;spLast=now;
  if(macro==null){spData.remove("active.macro");spData.remove("active.last");}else{spData.setProperty("active.macro",spKey(macro));spData.setProperty("active.last",Long.toString(now));}
 }
 public static synchronized void spGain(String kind,String macro,double amount){
  if(macro==null||macro.isBlank()||!Double.isFinite(amount)||amount<=0)return;
  if(!kind.equals("profit")&&!kind.equals("xp"))throw new IllegalArgumentException("Unknown statistic");
  try{if(spData==null)spTick(Minecraft.getInstance());String key=kind+"."+spKey(macro);spData.setProperty(key,new BigDecimal(spData.getProperty(key,"0")).add(BigDecimal.valueOf(amount)).toPlainString());spDirty=true;}
  catch(Exception e){spError=new IllegalStateException("Could not record "+kind,e);}
 }
 public static void spProfit(lIIlIlIIIlllIlIlIIllllIllIIIIIIlIlIlIIllIlIIIlIIIIIlllIllIlIlIIl macro,double amount){if(macro!=null)spGain("profit",macro.IIllIlIlIIIIIllIlIIlllllIlllIlIIlIIIIlIIIIlIllIIllIlIlIlIIllIIII(),amount);}
 public static void spXp(double amount){lIIlIlIIIlllIlIlIIllllIllIIIIIIlIlIlIIllIlIIIlIIIIIlllIllIlIlIIl macro=IIIIIlIllIIIllIIlIllIlllIIIIIlIlIIlIIlllIlIlIlIlIIllIllIIllIIlII.IIlllIllIllIlIllllIlIllllIlIlIIlllIlllIllIIIllIIIIllIIlIlIlIllll.llIIIIlIllIllIlIllIllIIllllIlIIllllllllIIIIIlllIIIIlIIlllllllIIl();if(macro!=null)spGain("xp",macro.IIllIlIlIIIIIllIlIIlllllIlllIlIIlIIIIlIIIIlIllIIllIlIlIlIIllIIII(),amount);}
 public static synchronized void spFailsafe(String name){
  if(name==null||name.isBlank())return;
  // This DTO is also constructed while parsing history; count only the live sender.
  for(StackTraceElement frame:Thread.currentThread().getStackTrace())if(frame.isNativeMethod()&&frame.getClassName().equals("zip.nebula.client.lIlllIIIllIIllllIlIllIlIIIIlIlllIlIlIlIIIIIIlIIlIllllIlllIIllllI")&&frame.getMethodName().equals("lIlIIIIIlIIIlIIIlIIllIIIIlIlllIllIllIIIIIIIlIIIllllIIIIIllIlllIl")){
   try{if(spData==null)spTick(Minecraft.getInstance());spAdd("failsafes."+spKey(name),1);}catch(Exception e){spError=new IllegalStateException("Could not record failsafe",e);}return;}
 }
 public static synchronized void spFlush()throws Exception{
  if(!spDirty||spData==null)return;Files.createDirectories(spFile.getParent());
  Path temp=Files.createTempFile(spFile.getParent(),".statistics-",".tmp");
  try{try(OutputStream out=Files.newOutputStream(temp)){spData.store(out,"Nebula statistics");}try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}Files.move(temp,spFile,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);spDirty=false;}finally{Files.deleteIfExists(temp);}
 }
 public static synchronized Statistics spSnapshot(){
  if(spError!=null)throw spError;
  if(spData==null){try{spTick(Minecraft.getInstance());}catch(Exception e){throw new IllegalStateException("Statistics could not be loaded",e);}}
  Map<String,Long> time=new TreeMap<>();Map<String,Integer> starts=new TreeMap<>(),fails=new TreeMap<>();Map<String,Double> xp=new TreeMap<>(),profit=new TreeMap<>();Map<kotlinx.datetime.LocalDate,Map<String,Long>> daily=new TreeMap<>();
  for(String key:spData.stringPropertyNames()){
   String value=spData.getProperty(key);
   if(key.startsWith("time."))time.put(spName(key.substring(5)),Long.parseLong(value));
   else if(key.startsWith("starts."))starts.put(spName(key.substring(7)),Integer.parseInt(value));
   else if(key.startsWith("failsafes."))fails.put(spName(key.substring(9)),Integer.parseInt(value));
   else if(key.startsWith("xp."))xp.put(spName(key.substring(3)),Double.parseDouble(value));
   else if(key.startsWith("profit."))profit.put(spName(key.substring(7)),Double.parseDouble(value));
   else if(key.startsWith("daily.")){int split=key.indexOf('.',6);kotlinx.datetime.LocalDate day=new kotlinx.datetime.LocalDate(LocalDate.parse(key.substring(6,split)));daily.computeIfAbsent(day,d->new TreeMap<>()).put(spName(key.substring(split+1)),Long.parseLong(value));}
  }
  return new Statistics(time,time.values().stream().mapToLong(Long::longValue).sum(),starts,starts.values().stream().mapToInt(Integer::intValue).sum(),fails,fails.values().stream().mapToInt(Integer::intValue).sum(),xp,xp.values().stream().mapToDouble(Double::doubleValue).sum(),profit,profit.values().stream().mapToDouble(Double::doubleValue).sum(),daily);
 }
}
