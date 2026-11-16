import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.net.*;
import javax.net.ssl.*;
import okhttp3.*;
public class WindowsTrustProbe {
 public static void main(String[] args)throws Exception{
  X509Certificate expected;try(var in=Files.newInputStream(Path.of(args[0]))){expected=(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(in);}
  KeyStore roots=KeyStore.getInstance("Windows-ROOT");roots.load(null,null);
  if(args.length>1&&args[1].equals("--delete")){
   int removed=0;for(String alias:java.util.Collections.list(roots.aliases()))if(expected.equals(roots.getCertificate(alias))){roots.deleteEntry(alias);removed++;}
   roots.load(null,null);for(String alias:java.util.Collections.list(roots.aliases()))if(expected.equals(roots.getCertificate(alias)))throw new AssertionError("Temporary CA still present");
   System.out.println("Removed temporary test CA entries: "+removed);return;
  }
  boolean found=false;
  for(var aliases=roots.aliases();aliases.hasMoreElements();)if(expected.equals(roots.getCertificate(aliases.nextElement())))found=true;
  if(!found)throw new AssertionError("Test CA absent from Windows ROOT store");
  TrustManagerFactory f=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());f.init(roots);X509TrustManager tm=(X509TrustManager)f.getTrustManagers()[0];SSLContext tls=SSLContext.getInstance("TLS");tls.init(null,new TrustManager[]{tm},null);
  OkHttpClient client=new OkHttpClient.Builder().sslSocketFactory(tls.getSocketFactory(),tm).proxy(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",8080))).followRedirects(false).build();
  try(Response r=client.newCall(new Request.Builder().url("https://api.nebulaclient.net/").build()).execute()){
   System.out.println("PASS: test CA present in Windows ROOT; Windows-store-backed certificate validation and hostname verification accepted the interception certificate; HTTP "+r.code());
  }System.exit(0);
 }
}
