package de.ganskef.test;

import io.netty.handler.ssl.SslContext;

import java.io.File;

import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.BouncyCastleSslEngineSource;
import org.littleshoot.proxy.mitm.SubjectAlternativeNameHolder;

public class SecureServer extends Server {

    public SecureServer(int port) {
        super(port);
    }

    public Server start() throws Exception {
        BouncyCastleSslEngineSource es = new BouncyCastleSslEngineSource(
                new Authority(), true, true);
        SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();
        // san.addDomainName("localhost");
        // san.addDomainName("*.local");
        // san.addIpAddress("127.0.0.1");
        es.initializeServerCertificates("localhost", san);
        File certChainFile = new File("littleproxy-mitm-localhost-cert.pem");
        File keyFile = new File("littleproxy-mitm-localhost-key.pem");
        SslContext sslCtx = SslContext.newServerContext(certChainFile, keyFile);

        // SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
        // SslContext sslCtx = SslContext.newServerContext(SslProvider.JDK,
        // ssc.certificate(), ssc.privateKey());
        return super.start(sslCtx);
    }

    @Override
    public String getBaseUrl() {
        return ("https://localhost:" + getPort());
    }

    public static void main(String[] args) throws Exception {
        new SecureServer(8083).start();
        waitUntilInterupted();
    }

}
