package org.littleshoot.proxy.mitm;

import java.io.File;

import org.apache.log4j.xml.DOMConfigurator;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class Launcher {

    public static void main(final String... args) {
        File log4jConfigurationFile = new File(
                "src/test/resources/log4j.xml");
        if (log4jConfigurationFile.exists()) {
            DOMConfigurator.configureAndWatch(
                    log4jConfigurationFile.getAbsolutePath(), 15);
        }
        try {
            final int port = 9090;

            System.out.println("About to start server on port: " + port);
            HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer
                    .bootstrapFromFile("./littleproxy.properties")
                    .withPort(port).withAllowLocalOnly(false);

            bootstrap.withManInTheMiddle(new CertificateSniffingMitmManager());

            System.out.println("About to start...");
            bootstrap.start();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
