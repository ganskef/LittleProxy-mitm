package de.ganskef.test;

import java.io.File;

import org.apache.log4j.xml.DOMConfigurator;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class Proxy implements IProxy {

    private HttpProxyServer proxy;

    private final int proxyPort;

    public Proxy(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @Override
    public int getProxyPort() {
        return proxyPort;
    }

    @Override
    public Proxy start() {
        if (proxy != null) {
            proxy.stop();
        }
        proxy = bootstrap().start();
        return this;
    }

    protected HttpProxyServerBootstrap bootstrap() {
        return DefaultHttpProxyServer.bootstrap().withPort(proxyPort);
    }

    @Override
    public void stop() {
        proxy.stop();
    }

    public static void main(final String... args) {
        File log4jConfigurationFile = new File("src/test/resources/log4j.xml");
        if (log4jConfigurationFile.exists()) {
            DOMConfigurator.configureAndWatch(
                    log4jConfigurationFile.getAbsolutePath(), 15);
        }
        new Proxy(9090).start();
        Server.waitUntilInterupted();
    }

}
