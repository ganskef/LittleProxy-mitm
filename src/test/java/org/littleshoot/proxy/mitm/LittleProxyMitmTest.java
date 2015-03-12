package org.littleshoot.proxy.mitm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ganskef.test.Client2;
import de.ganskef.test.SecureServer;
import de.ganskef.test.Server;

public class LittleProxyMitmTest {

    private static final String IMAGE_PATH = "src/test/resources/www/netty-in-action.gif";

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
        String url = server.getBaseUrl() + "/" + IMAGE_PATH;
        File direct = //
        // new File(IMAGE_PATH);
        new Client2().get(url);

        File proxied = new Client2().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    @Test
    public void testSecuredImage() throws Exception {
        String url = secureServer.getBaseUrl() + "/" + IMAGE_PATH;
        File direct = //
        // new File(IMAGE_PATH);
        new Client2().get(url);

        File proxied = new Client2().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    @Test
    public void testOnlineTextSecured() throws Exception {
        String url = "https://www.google.com/humans.txt";
        try {
            File direct = new Client2().get(url);

            File proxied = new Client2().get(url, proxy);
            assertEquals(direct.length(), proxied.length());

        } catch (ConnectException ignored) {
            System.out.println("Ignored testOnlineText while offline");
        } catch (UnknownHostException ignored) {
            System.out.println("Ignored testOnlineText while offline");
        } catch (UnresolvedAddressException ignored) {
            System.out.println("Ignored testOnlineText while offline");
        }
    }

    @Test
    public void testCachedResponse() throws Exception {
        proxy.setConnectionLimited();
        String url = "http://somehost/somepath";
        File proxied = new Client2().get(url, proxy);
        assertEquals("Offline response", FileUtils.readFileToString(proxied));
    }

    // This works with Cromium and Mozilla Firefox browser, but fails with hc
    // and with URLConnection too. The handshake is done with the target host
    // instead of doing it with the proxy. What's wrong here?
    // @Test
    public void testCachedResponseSecured() throws Exception {
        proxy.setConnectionLimited();
        String url = "https://somehost/somepath";
        File proxied = new Client2().get(url, proxy);
        assertEquals("Offline response", FileUtils.readFileToString(proxied));
    }

}
