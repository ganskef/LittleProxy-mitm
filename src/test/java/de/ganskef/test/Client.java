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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class Client {

    public File get(String url, IProxy proxy) throws Exception {
        HttpHost proxyHost = new HttpHost("localhost", proxy.getProxyPort());
        return get(url, proxyHost, "proxy.out");
    }

    public File get(String url) throws Exception {
        return get(url, (HttpHost) null, "direct.out");
    }

    private File get(String url, HttpHost proxyHost, String target)
            throws Exception {
        File result = new File(target);
        if (result.exists() && !result.delete()) {
            throw new IllegalStateException("Coudn't be deleted " + result);
        }
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
        CloseableHttpClient httpclient = clientBuilder.build();

        HttpGet request = new HttpGet(url);
        if (proxyHost != null) {
            RequestConfig config = RequestConfig.custom().setProxy(proxyHost)
                    .build();
            request.setConfig(config);
        }
        CloseableHttpResponse response = httpclient.execute(request);
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
        return result;
    }

    public static void main(String[] args) throws Exception {
        new Client().get("https://localhost:8083");
        new Client().get("https://www.google.com/humans.txt");
    }

}
