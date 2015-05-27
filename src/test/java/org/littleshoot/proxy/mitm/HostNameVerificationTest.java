package org.littleshoot.proxy.mitm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ganskef.test.IClient;
import de.ganskef.test.IProxy;
import de.ganskef.test.Server;
import de.ganskef.test.TrustedClient;
import de.ganskef.test.TrustedServer;

// https://github.com/adamfisk/LittleProxy/pull/210
// https://github.com/adamfisk/LittleProxy/issues/207
public class HostNameVerificationTest {

    private static final String IMAGE_PATH = "src/test/resources/www/netty-in-action.gif";

    private static IProxy proxy;
    private static Server trustedServer, invalidServer;

    @AfterClass
    public static void afterClass() {
        trustedServer.stop();
        invalidServer.stop();
        proxy.stop();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        trustedServer = new TrustedServer(9091).start();
        invalidServer = new TrustedServer(9092, "wrong_name").start();
        proxy = new LittleProxyMitmProxy(9093).start();
    }

    protected IClient newClient() {
        return new TrustedClient();
    }

    @Test
    public void testWithTrustedCertificates() throws Exception {
        String url = trustedServer.getBaseUrl() + "/" + IMAGE_PATH;
        File direct = newClient().get(url);

        File proxied = newClient().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    // Java 6 throws an IOException, Java 7 SSLException here
    @Test(expected = IOException.class)
    public void testDirectFailWithWrongHost() throws Exception {
        newClient().get(invalidServer.getBaseUrl() + "/" + IMAGE_PATH);
    }

    @Test(expected = IOException.class)
    public void testProxiedFailWithWrongHost() throws Exception {
        newClient().get(invalidServer.getBaseUrl() + "/" + IMAGE_PATH, proxy);
    }

}
