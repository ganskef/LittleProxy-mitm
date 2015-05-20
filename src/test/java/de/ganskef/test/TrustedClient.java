package de.ganskef.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;

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

        final X509TrustManager addedTm = defaultTrustManager(ks);
        final X509TrustManager javaTm = defaultTrustManager(null);
        X509TrustManager customTm = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                List<X509Certificate> issuers = new ArrayList<X509Certificate>();
                issuers.addAll(Arrays.asList(addedTm.getAcceptedIssuers()));
                issuers.addAll(Arrays.asList(javaTm.getAcceptedIssuers()));
                return issuers.toArray(new X509Certificate[issuers.size()]);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
                try {
                    addedTm.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    javaTm.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
                try {
                    javaTm.checkClientTrusted(chain, authType);
                } catch (CertificateException e) {
                    addedTm.checkClientTrusted(chain, authType);
                }
            }
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { customTm }, null);
        return context;
    }

    private X509TrustManager defaultTrustManager(KeyStore trustStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        String tma = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tma);
        tmf.init(trustStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        for (TrustManager each : trustManagers) {
            if (each instanceof X509TrustManager) {
                return (X509TrustManager) each;
            }
        }
        throw new IllegalStateException("Missed X509TrustManager in "
                + Arrays.toString(trustManagers));
    }

    public static void main(String[] args) throws Exception {
        File trusted = new TrustedClient().get("https://localhost:8083");
        System.out.println(FileUtils.readFileToString(trusted));
        File online = new TrustedClient()
                .get("https://www.google.com/humans.txt");
        System.out.println(FileUtils.readFileToString(online));
    }

}
