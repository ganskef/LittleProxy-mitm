package de.ganskef.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Client {

    private static final int PROXY_PORT_UNDEFINED = -1;

    public File get(String url, IProxy proxy) throws Exception {
        return get(new URI(url), proxy.getProxyPort());
    }

    public File get(String url) throws Exception {
        URI uri = new URI(url);
        return get(uri, PROXY_PORT_UNDEFINED);
    }

    public File get(URI uri, int proxyPort) throws Exception {
        URLConnection con = createConnection(uri.toURL(), proxyPort);
        final boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
        if (ssl) {
            return callHttpsGet(con);
        } else {
            return callHttpGet(con);
        }
    }

    private File callHttpGet(URLConnection con) throws IOException {
        con.connect();
        return read(con);
    }

    private File callHttpsGet(URLConnection con)
            throws GeneralSecurityException, IOException {
        FileInputStream is = new FileInputStream(new File(
                "littleproxy-mitm.p12"));
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, "Be Your Own Lantern".toCharArray());
        is.close();

        String tma = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tma);
        tmf.init(ks);
        X509TrustManager defaultTrustManager = (X509TrustManager) tmf
                .getTrustManagers()[0];

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { defaultTrustManager }, null);
        SSLSocketFactory sslSocketFactory = context.getSocketFactory();

        ((HttpsURLConnection) con).setSSLSocketFactory(sslSocketFactory);
        con.connect();
        return read(con);
    }

    private URLConnection createConnection(URL url, int proxyPort)
            throws MalformedURLException, IOException {
        URLConnection result;
        if (proxyPort != PROXY_PORT_UNDEFINED) {
            SocketAddress sa = new InetSocketAddress("localhost", proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
            result = url.openConnection(proxy);
        } else {
            result = url.openConnection();
        }
        return result;
    }

    private File read(URLConnection con) throws IOException {
        String name = Client.class.getSimpleName();
        File result = File.createTempFile(name, ".out");
        result.deleteOnExit();
        OutputStream os = null;
        InputStream is = null;
        try {
            is = con.getInputStream();
            os = new FileOutputStream(result);
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        File result = new Client().get("https://localhost:8083");
        System.out.println(FileUtils.readFileToString(result));
    }

}
