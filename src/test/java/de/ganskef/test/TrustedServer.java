package de.ganskef.test;

import io.netty.handler.ssl.SslContext;

import java.io.File;

import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.BouncyCastleSslEngineSource;
import org.littleshoot.proxy.mitm.SubjectAlternativeNameHolder;

public class TrustedServer extends Server {

    public TrustedServer(int port) {
        super(port);
    }

    public Server start() throws Exception {
        BouncyCastleSslEngineSource es = new BouncyCastleSslEngineSource(
                new Authority(), true, true);
        SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();
        // san.addDomainName("localhost");
        es.initializeServerCertificates("localhost", san);
        File certChainFile = new File("littleproxy-mitm-localhost-cert.pem");
        File keyFile = new File("littleproxy-mitm-localhost-key.pem");
        SslContext sslCtx = SslContext.newServerContext(certChainFile, keyFile);
        return super.start(sslCtx);
    }

    @Override
    public String getBaseUrl() {
        if (getPort() == 443) {
            return ("https://localhost");
        } else {
            return ("https://localhost:" + getPort());
        }
    }

    public static void main(String[] args) throws Exception {
        new TrustedServer(8083).start();
        waitUntilInterupted();
    }

}
