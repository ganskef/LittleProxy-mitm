package de.ganskef.test;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.File;

import javax.net.ssl.SSLException;

public class SecureServer extends Server {

    public SecureServer(int port) {
        super(port);
    }

    public Server start() throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
        return initServerContext(ssc.certificate(), ssc.privateKey());
    }

    protected Server initServerContext(File certChainFile, File keyFile)
            throws SSLException, InterruptedException {
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
        new SecureServer(8083).start();
        waitUntilInterupted();
    }

}
