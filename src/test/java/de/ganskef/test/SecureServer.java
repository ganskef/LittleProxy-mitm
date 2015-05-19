package de.ganskef.test;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class SecureServer extends Server {

    public SecureServer(int port) {
        super(port);
    }

    public Server start() throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
        SslContext sslCtx = SslContext.newServerContext(SslProvider.JDK,
                ssc.certificate(), ssc.privateKey());
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
