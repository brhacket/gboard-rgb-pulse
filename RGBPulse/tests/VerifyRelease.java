import java.io.File;
import java.security.MessageDigest;
import com.android.apksig.ApkVerifier;
public class VerifyRelease {
 public static void main(String[] args)throws Exception{
  ApkVerifier.Result r=new ApkVerifier.Builder(new File(args[0])).setMinCheckedPlatformVersion(33).build().verify();
  if(!r.isVerified())throw new AssertionError(r.getErrors());
  StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(r.getSignerCertificates().get(0).getEncoded()))hash.append(String.format("%02x",b));
  if(!hash.toString().equals("592b78f82378ea10a909a1d960a47bd1590ac84f738ad7314ecded596445dff1"))throw new AssertionError(hash);
  System.out.println("PASS: release APK signature verified for API 33+; original signing certificate SHA-256="+hash);
 }
}
