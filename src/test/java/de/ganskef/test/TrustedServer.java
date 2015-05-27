package de.ganskef.test;

import java.io.File;

import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.BouncyCastleSslEngineSource;
import org.littleshoot.proxy.mitm.SubjectAlternativeNameHolder;

public class TrustedServer extends SecureServer {

    private String commonName;

    public TrustedServer(int port, String commonName) {
        super(port);
        this.commonName = commonName;
    }

    public TrustedServer(int port) {
        this(port, "localhost");
    }

    public Server start() throws Exception {
        BouncyCastleSslEngineSource es = new BouncyCastleSslEngineSource(
                new Authority(), true, true);
        SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();
        // san.addDomainName("localhost");
        es.initializeServerCertificates(commonName, san);
        File certChainFile = new File("littleproxy-mitm-" + commonName
                + "-cert.pem");
        File keyFile = new File("littleproxy-mitm-" + commonName + "-key.pem");
        return initServerContext(certChainFile, keyFile);
    }

    public static void main(String[] args) throws Exception {
        new TrustedServer(8083).start();
        waitUntilInterupted();
    }

}
