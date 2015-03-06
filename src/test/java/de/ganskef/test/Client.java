package de.ganskef.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.util.EntityUtils;

public class Client {

    public File get(String url, IProxy proxy) throws Exception {
        HttpHost testProxy = new HttpHost("localhost", proxy.getProxyPort());
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(
                testProxy);
        return get(url, routePlanner, "proxy.out");
    }

    public File get(String url) throws Exception {
        return get(url, (DefaultRoutePlanner) null, "direct.out");
    }

    private File get(String url, DefaultRoutePlanner routePlanner, String target)
            throws Exception {
        URI uri = new URI(url);
        HttpClientBuilder clientBuilder = HttpClients.custom();
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            SSLContext sslcontext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain,
                                String authType) throws CertificateException {
                            return true;
                        }
                    }).build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslcontext);
            clientBuilder.setSSLSocketFactory(sslsf);
        }
        if (routePlanner != null) {
            clientBuilder.setRoutePlanner(routePlanner);
        }
        CloseableHttpClient httpclient = clientBuilder.build();

        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = httpclient.execute(httpGet);
        InputStream input = null;
        OutputStream output = null;
        try {
            HttpEntity entity = response.getEntity();
            input = entity.getContent();
            output = new FileOutputStream(target);
            IOUtils.copy(input, output);
            EntityUtils.consume(entity);
        } finally {
            response.close();
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
        return new File(target);
    }

    public static void main(String[] args) throws Exception {
        new Client().get("https://localhost:8083");
        new Client().get("https://www.google.com/humans.txt");
    }

}
