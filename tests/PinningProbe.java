import okhttp3.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.nio.file.*;
public class PinningProbe {
 public static void main(String[] args)throws Exception {
  KeyStore ks=KeyStore.getInstance(KeyStore.getDefaultType());ks.load(null,null);try(var in=Files.newInputStream(Path.of(args[0]))){ks.setCertificateEntry("test-ca",CertificateFactory.getInstance("X.509").generateCertificate(in));}
  TrustManagerFactory factory=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());factory.init(ks);X509TrustManager tm=(X509TrustManager)factory.getTrustManagers()[0];SSLContext ssl=SSLContext.getInstance("TLS");ssl.init(null,new TrustManager[]{tm},null);
  CertificatePinner pins=new CertificatePinner.Builder().add("localhost","sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=").build();
  OkHttpClient trusted=new OkHttpClient.Builder().sslSocketFactory(ssl.getSocketFactory(),tm).certificatePinner(pins).build();
  try(Response response=trusted.newCall(new Request.Builder().url("https://localhost:9443/").build()).execute()){if(!response.isSuccessful())throw new AssertionError("HTTP "+response.code());}
  try(Response r=new OkHttpClient().newCall(new Request.Builder().url("https://localhost:9443/").build()).execute()){throw new AssertionError("Untrusted CA accepted");}catch(SSLHandshakeException expected){}
  try(Response r=trusted.newCall(new Request.Builder().url("https://127.0.0.1:9443/").build()).execute()){throw new AssertionError("Wrong hostname accepted");}catch(SSLPeerUnverifiedException expected){}
  System.out.println("PASS: trusted certificate succeeds despite mismatched pin; untrusted certificates and hostname mismatches remain rejected");System.exit(0);
 }
}
