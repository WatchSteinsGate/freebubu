import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.lang.reflect.Field;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import kotlin.time.Clock;
import zip.nebula.*;
import zip.nebula.client.IIIIlIIlllllIIIlllIIIlIIlllIllIlIlIllllIllllIIIlIlIIlIlllllIIIIl;

/** Build-time template: all members are merged into the original cape manager. */
public class CosmeticsPatch implements ClientModInitializer {
 private static UUID cpOwner;
 private static String cpName,cpToken,cpAuthority,cpRaw,cpHash;
 private static Path cpDirectory;
 private static HttpServer cpServer;
 private static ExecutorService cpExecutor;
 private static Gson cpJson;
 private static List<Identifier> cpTextures;
 private static List<Integer> cpDelays;
 private static long cpStart,cpLastPoll;
 private static Object cpNickRow,cpPreviousNick;
 private static String cpNickOwner;
 private static boolean cpRegistered;
 public void onInitializeClient(){cpInitialize();}
 public static synchronized void cpInitialize(){
  if(cpRegistered)return;cpRegistered=true;
  cpJson=new GsonBuilder().setPrettyPrinting().create();cpTextures=new ArrayList<>();cpDelays=new ArrayList<>();cpToken=UUID.randomUUID().toString();
  cpDirectory=FabricLoader.getInstance().getGameDir().resolve("Nebula/cosmetics");cpRaw="";cpHash="";
  Minecraft mc=Minecraft.getInstance();cpOwner=mc.getUser().getProfileId();cpName=mc.getUser().getName();
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{cpTick(client);}catch(Exception e){System.err.println("[Nebula] Cosmetics could not be applied: "+e);}});
  ClientLifecycleEvents.CLIENT_STOPPING.register(client->{synchronized(CosmeticsPatch.class){if(cpServer!=null)cpServer.stop(0);if(cpExecutor!=null)cpExecutor.shutdownNow();}});
  try{
   cpServer=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),Integer.getInteger("nebula.cosmetics.port",6969)),8);
   cpAuthority="127.0.0.1:"+cpServer.getAddress().getPort();
   cpExecutor=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"cosmetics-editor");t.setDaemon(true);return t;});cpServer.setExecutor(cpExecutor);cpServer.createContext("/",CosmeticsPatch::cpHandle);cpServer.start();
   System.out.println("[Nebula] Cosmetics editor: http://"+cpAuthority+"/");
  }catch(IOException e){throw new IllegalStateException("Cosmetics editor could not start",e);}
 }
 private static Path cpPath(){return cpDirectory.resolve(cpOwner+".json");}
 private static synchronized void cpTick(Minecraft mc)throws Exception{
  UUID id=mc.getUser().getProfileId();String name=mc.getUser().getName();
  if(!Objects.equals(id,cpOwner)||!Objects.equals(name,cpName)){cpRestoreNick();cpClearCape(mc);cpRaw="";cpOwner=id;cpName=name;}
  if(System.nanoTime()-cpLastPoll<500_000_000L)return;cpLastPoll=System.nanoTime();
  cpMigrate();Path file=cpPath();if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))return;
  byte[] bytes;try(InputStream in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){bytes=in.readNBytes(67_108_865);}if(bytes.length>67_108_864)throw new IOException("Cosmetics state exceeds 64 MiB");
  String raw=new String(bytes,StandardCharsets.UTF_8);JsonObject state=JsonParser.parseString(raw).getAsJsonObject();
  if(!cpOwner.toString().equals(state.get("uuid").getAsString())||state.get("schema").getAsInt()!=1)throw new IOException("Cosmetics state owner/schema mismatch");
  if(!raw.equals(cpRaw)){state=cpValidate(state);cpApplyCape(mc,state);cpApplyNick(state,true);cpRaw=raw;System.out.println("[Nebula] Applied cosmetics revision "+state.get("revision"));}else if(state.has("nickname"))cpApplyNick(state,false);
 }
 private static void cpMigrate()throws IOException{
  if(Files.exists(cpPath(),LinkOption.NOFOLLOW_LINKS))return;
  Path root=FabricLoader.getInstance().getGameDir().resolve("Nebula");
  for(String old:new String[]{"thatonecatgif/FreeCosmeticsLOL","local-cosmetics"}){Path file=root.resolve(old).resolve(cpOwner+".json");if(Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)){Files.createDirectories(cpDirectory);Files.copy(file,cpPath());return;}}
 }
 private static Map<String,Object> cpNickMap()throws Exception{
  Field f=IIIIlIIlllllIIIlllIIIlIIlllIllIlIlIllllIllllIIIlIlIIlIlllllIIIIl.class.getDeclaredField("IIIllIlllIIIlllIIllllIIIIIIlllllIIllIIlIllIlIlIlIllIIlIIIllIllIl");f.setAccessible(true);return (Map<String,Object>)f.get(null);
 }
 private static void cpRestoreNick()throws Exception{
  if(cpNickOwner==null)return;Map<String,Object> map=cpNickMap();if(map.get(cpNickOwner)==cpNickRow){if(cpPreviousNick==null)map.remove(cpNickOwner);else map.put(cpNickOwner,cpPreviousNick);}cpNickOwner=null;cpNickRow=null;cpPreviousNick=null;
 }
 private static void cpApplyNick(JsonObject state,boolean force)throws Exception{
  if(!state.has("nickname")){cpRestoreNick();return;}
  if(cpNickOwner!=null&&!cpNickOwner.equals(cpName))cpRestoreNick();Map<String,Object> map=cpNickMap();Object row=map.get(cpName);if(!force&&cpNickRow!=null&&row==cpNickRow)return;if(row!=cpNickRow)cpPreviousNick=row;
  List<Nick> frames=new ArrayList<>();for(JsonElement e:state.getAsJsonObject("nickname").getAsJsonArray("frames")){JsonObject f=e.getAsJsonObject();frames.add(new Nick(f.get("nick").getAsString(),f.get("prefix").getAsString(),f.get("delay").getAsInt()));}
  kotlin.time.Instant now=Clock.System.INSTANCE.now();CosmeticDTO dto=new CosmeticDTO("device-cosmetics","device","device",cpOwner,cpName,new CosmeticDataDTO.NickData(frames),CosmeticType.NICK,CosmeticVisibility.PUBLIC,now,now,now);
  IIIIlIIlllllIIIlllIIIlIIlllIllIlIlIllllIllllIIIlIlIIlIlllllIIIIl.llIIllIllIIIlIlIllIIlllIlIlIllllllIlIIIllIIllllllIIllllllIIlIIlI.IlIIIllIIIIIIIIIllIllllllIllllIlIIlIIllIlllIllIIIlIllIlIlIlIlIIl(dto);cpNickOwner=cpName;cpNickRow=map.get(cpName);
 }
 private static void cpClearCape(Minecraft mc){for(Identifier id:cpTextures)mc.getTextureManager().release(id);cpTextures.clear();cpDelays.clear();cpHash="";}
 private static void cpApplyCape(Minecraft mc,JsonObject state)throws Exception{
  if(!state.has("cape")){cpClearCape(mc);return;}
  JsonObject cape=state.getAsJsonObject("cape");String hash=cape.get("sha256").getAsString();if(hash.equals(cpHash))return;
  List<Identifier> textures=new ArrayList<>();List<Integer> delays=new ArrayList<>();
  try{int i=0;for(JsonElement e:cape.getAsJsonArray("frames")){JsonObject f=e.getAsJsonObject();byte[] bytes=Base64.getDecoder().decode(f.get("png").getAsString());NativeImage image=NativeImage.read(bytes);Identifier id=Identifier.fromNamespaceAndPath("nebula","cosmetics/"+cpOwner+"/"+hash+"/"+i++);DynamicTexture texture=new DynamicTexture(()->"Cape",image);try{mc.getTextureManager().register(id,texture);}catch(Exception ex){texture.close();throw ex;}textures.add(id);delays.add(f.get("delay").getAsInt());}}
  catch(Exception e){for(Identifier id:textures)mc.getTextureManager().release(id);throw e;}
  cpClearCape(mc);cpTextures.addAll(textures);cpDelays.addAll(delays);cpHash=hash;cpStart=System.nanoTime();System.out.println("[Nebula] Registered cape frames="+textures.size());
 }
 public static synchronized Identifier cpCape(UUID owner){
  if(!Objects.equals(owner,cpOwner)||cpTextures==null||cpTextures.isEmpty())return null;
  long total=0;for(int delay:cpDelays)total+=delay;long elapsed=(System.nanoTime()-cpStart)/1_000_000L%total;for(int i=0;i<cpDelays.size();i++){elapsed-=cpDelays.get(i);if(elapsed<0)return cpTextures.get(i);}return cpTextures.getFirst();
 }
 private static void cpCheck(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
 private static int cpDelay(JsonObject frame,int min){int delay=frame.get("delay").getAsInt();cpCheck(delay>=min&&delay<=10000,"Invalid frame delay");return delay;}
 private static synchronized JsonObject cpValidate(JsonObject input)throws Exception{
  JsonObject state=input.deepCopy();state.addProperty("schema",1);state.addProperty("uuid",cpOwner.toString());state.addProperty("revision",UUID.randomUUID().toString());
  if(state.has("nickname")){JsonArray frames=state.getAsJsonObject("nickname").getAsJsonArray("frames");cpCheck(frames.size()>0&&frames.size()<=32,"Use 1–32 nickname frames");for(JsonElement e:frames){JsonObject f=e.getAsJsonObject();cpCheck(f.get("nick").getAsString().length()<=512&&f.get("prefix").getAsString().length()<=512,"Nickname or prefix is too long");cpDelay(f,50);}}
  if(state.has("cape")){JsonObject cape=state.getAsJsonObject("cape");JsonArray frames=cape.getAsJsonArray("frames");cpCheck(frames.size()>0&&frames.size()<=128,"Use 1–128 cape frames");MessageDigest hash=MessageDigest.getInstance("SHA-256");long pixels=0;int width=0;
   for(JsonElement e:frames){JsonObject f=e.getAsJsonObject();int delay=cpDelay(f,20);byte[] bytes=Base64.getDecoder().decode(f.get("png").getAsString());cpCheck(bytes.length<=16_777_216,"PNG frame exceeds 16 MiB");
    try(ImageInputStream stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){Iterator<ImageReader> readers=ImageIO.getImageReaders(stream);cpCheck(readers.hasNext(),"Invalid PNG");ImageReader reader=readers.next();try{cpCheck(reader.getFormatName().equalsIgnoreCase("png"),"Frames must be PNG");reader.setInput(stream);int w=reader.getWidth(0),h=reader.getHeight(0);cpCheck(w==h*2&&w>=64&&w<=4096,"Cape atlas must be 2:1, width 64–4096");cpCheck(width==0||width==w,"All cape frames must have matching dimensions");width=w;pixels+=(long)w*h;cpCheck(pixels<=134_217_728L,"Cape animation exceeds its decoded pixel budget");reader.read(0);}finally{reader.dispose();}}
    hash.update(bytes);hash.update((byte)(delay>>8));hash.update((byte)delay);
   }cape.addProperty("sha256",HexFormat.of().formatHex(hash.digest()));
  }return state;
 }
 private static synchronized JsonObject cpRead()throws Exception{
  cpMigrate();JsonObject out=new JsonObject();out.addProperty("name",cpName);JsonObject state=new JsonObject();
  if(Files.exists(cpPath(),LinkOption.NOFOLLOW_LINKS)){cpCheck(Files.isRegularFile(cpPath(),LinkOption.NOFOLLOW_LINKS),"State file must be regular");cpCheck(Files.size(cpPath())<=67_108_864,"State exceeds 64 MiB");state=JsonParser.parseString(Files.readString(cpPath())).getAsJsonObject();cpCheck(cpOwner.toString().equals(state.get("uuid").getAsString()),"State owner mismatch");}
  out.add("state",state);return out;
 }
 private static synchronized JsonObject cpWrite(JsonObject input)throws Exception{
  JsonObject state=cpValidate(input);byte[] bytes=cpJson.toJson(state).getBytes(StandardCharsets.UTF_8);cpCheck(bytes.length<=67_108_864,"State exceeds 64 MiB");Files.createDirectories(cpDirectory);cpCheck(!Files.isSymbolicLink(cpPath()),"Symlinked state file");Path tmp=Files.createTempFile(cpDirectory,".cosmetics-",".tmp");
  try{Files.write(tmp,bytes);try(FileChannel c=FileChannel.open(tmp,StandardOpenOption.WRITE)){c.force(true);}Files.move(tmp,cpPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(tmp);}return cpRead();
 }
 private static void cpReply(HttpExchange exchange,int code,String text,String type)throws IOException{byte[] bytes=text.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);}
 private static void cpHandle(HttpExchange exchange)throws IOException{
  try{
   Headers h=exchange.getRequestHeaders(),out=exchange.getResponseHeaders();out.set("Cache-Control","no-store");out.set("X-Content-Type-Options","nosniff");out.set("Content-Security-Policy","default-src 'self'; script-src 'nonce-"+cpToken+"'; style-src 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
   if(!cpAuthority.equalsIgnoreCase(h.getFirst("Host"))){cpReply(exchange,403,"Invalid host","text/plain");return;}
   String origin=h.getFirst("Origin");if(origin!=null&&!origin.equals("http://"+cpAuthority)){cpReply(exchange,403,"Invalid origin","text/plain");return;}
   String path=exchange.getRequestURI().getPath(),method=exchange.getRequestMethod();
   if(path.equals("/")&&method.equals("GET")){
    try(InputStream in=CosmeticsPatch.class.getResourceAsStream("/thatonecatgif/FreeCosmeticsLOL/editor.html")){if(in==null)throw new IOException("Editor resource missing");cpReply(exchange,200,new String(in.readAllBytes(),StandardCharsets.UTF_8).replace("__TOKEN__",cpToken),"text/html");}return;
   }
   if(!path.equals("/api/state")){cpReply(exchange,404,"Not found","text/plain");return;}
   if(!cpToken.equals(h.getFirst("X-Cosmetics-Token"))){cpReply(exchange,403,"Reload the editor page","text/plain");return;}
   if(method.equals("GET")){cpReply(exchange,200,cpJson.toJson(cpRead()),"application/json");return;}
   if(method.equals("POST")){byte[] bytes=exchange.getRequestBody().readNBytes(67_108_865);cpCheck(bytes.length<=67_108_864,"State exceeds 64 MiB");JsonObject state=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();cpReply(exchange,200,cpJson.toJson(cpWrite(state)),"application/json");return;}
   cpReply(exchange,405,"Method not allowed","text/plain");
  }catch(Exception e){cpReply(exchange,400,e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(),"text/plain");}finally{exchange.close();}
 }
}
