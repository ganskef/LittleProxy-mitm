package de.ganskef.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.littleshoot.proxy.mitm.MergeTrustManager;

/**
 * Client which uses a custom trust store as well as the default one.
 */
public class TrustedClient extends Client implements IClient {

    protected SSLContext initSslContext() throws GeneralSecurityException,
            IOException {
        FileInputStream is = new FileInputStream(new File(
                "littleproxy-mitm.p12"));
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, "Be Your Own Lantern".toCharArray());
        is.close();

        X509TrustManager customTm = new MergeTrustManager(ks);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { customTm }, null);
        return context;
    }

    public static void main(String[] args) throws Exception {
        File trusted = new TrustedClient().get("https://localhost:8083");
        System.out.println(FileUtils.readFileToString(trusted));
        File online = new TrustedClient()
                .get("https://www.google.com/humans.txt");
        System.out.println(FileUtils.readFileToString(online));
    }

}
