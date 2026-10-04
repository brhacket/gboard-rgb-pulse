import com.android.apksig.ApkSigner;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;

/** Small, reproducible APK signer used by build.sh (APK Signature Scheme v2/v3). */
public final class Sign {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("usage: Sign key.pk8 cert.pem input.apk output.apk");
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PrivateKey key = factory.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(new File(args[0]).toPath())));
        CertificateFactory certificates = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certificates.generateCertificate(new File(args[1]).toURI().toURL().openStream());
        ApkSigner signer = new ApkSigner.Builder(Collections.singletonList(cert), key)
                .setInputApk(new File(args[2]))
                .setOutputApk(new File(args[3]))
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .setV4SigningEnabled(false)
                .build();
        signer.sign();
    }
}
