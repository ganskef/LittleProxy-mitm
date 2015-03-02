package de.ganskef.test;

import io.netty.handler.ssl.SslContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.BouncyCastleSslEngineSource;

public class SecureServer extends Server {

    public SecureServer(int port) {
        super(port);
    }

    public Server start() throws Exception {
        // if (!new File("littleproxy-mitm-localhost-cert.pem").isFile()
        // && !new File("littleproxy-mitm-localhost-key.pem").isFile()) {
        BouncyCastleSslEngineSource es = new BouncyCastleSslEngineSource(
                new Authority(), true, true);
        Collection<List<?>> subjectAlternativeNames = new ArrayList<List<?>>();
        subjectAlternativeNames.add(Arrays
                .asList(new Object[] { 2, "127.0.0.1" }));
        es.initializeServerCertificates("localhost", subjectAlternativeNames);
        // }
        File certChainFile = new File("littleproxy-mitm-localhost-cert.pem");
        File keyFile = new File("littleproxy-mitm-localhost-key.pem");
        SslContext sslCtx = SslContext.newServerContext(certChainFile, keyFile);

        // SelfSignedCertificate ssc = new SelfSignedCertificate();
        // SslContext sslCtx = SslContext.newServerContext(SslProvider.JDK,
        // ssc.certificate(), ssc.privateKey());
        return super.start(sslCtx);
    }

    @Override
    public String getBaseUrl() {
        if (getPort() == 443) {
            return ("https://127.0.0.1");
        } else {
            return ("https://127.0.0.1:" + getPort());
        }
    }

    public static void main(String[] args) throws Exception {
        new SecureServer(8083).start();
        waitUntilInterupted();
    }

}
