package org.littleshoot.proxy.mitm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.UnknownHostException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ganskef.test.Client;
import de.ganskef.test.SecureServer;
import de.ganskef.test.Server;

public class LittleProxyMitmTest {

    private static final String IMAGE_PATH = "/src/test/resources/www/netty-in-action.gif";

    private static LittleProxyMitmProxy proxy;
    private static Server server;
    private static Server secureServer;

    @AfterClass
    public static void afterClass() {
        secureServer.stop();
        server.stop();
        proxy.stop();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = new Server(9091).start();
        secureServer = new SecureServer(9092).start();
        proxy = new LittleProxyMitmProxy(9093).start();
    }

    @Before
    public void before() {
        proxy.setConnectionUnlimited();
    }

    @Test
    public void testSimpleImage() throws Exception {
        String url = server.getBaseUrl() + IMAGE_PATH;
        File direct = new Client().get(url);

        File proxied = new Client().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    @Test
    public void testSecuredImage() throws Exception {
        String url = secureServer.getBaseUrl() + IMAGE_PATH;
        File direct = new Client().get(url);

        File proxied = new Client().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    @Test
    public void testOnlineTextSecured() throws Exception {
        String url = "https://www.google.com/humans.txt";
        try {
            File direct = new Client().get(url);

            File proxied = new Client().get(url, proxy);
            assertEquals(direct.length(), proxied.length());
        } catch (UnknownHostException ignored) {
            System.out.println("Ignored testOnlineText while offline");
        }
    }

    @Test
    public void testCachedResponse() throws Exception {
        proxy.setConnectionLimited();
        String url = "http://somehost/somepath";
        File proxied = new Client().get(url, proxy);
        assertEquals("Offline response", FileUtils.readFileToString(proxied));
    }

    // FIXME Client must handshake with proxy or this test will fail
    // @Test
    public void testCachedResponseSecured() throws Exception {
        proxy.setConnectionLimited();
        String url = "https://somehost/somepath";
        File proxied = new Client().get(url, proxy);
        assertEquals("Offline response", FileUtils.readFileToString(proxied));
    }

}
