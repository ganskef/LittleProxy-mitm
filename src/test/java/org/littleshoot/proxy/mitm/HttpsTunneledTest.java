package org.littleshoot.proxy.mitm;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ganskef.test.Client;
import de.ganskef.test.IClient;
import de.ganskef.test.IProxy;
import de.ganskef.test.Server;
import de.ganskef.test.TrustedServer;
import de.ganskef.test.Proxy;

public class HttpsTunneledTest {

    private static final String IMAGE_PATH = "src/test/resources/www/netty-in-action.gif";

    private static IProxy proxy;
    private static Server secureServer;

    @AfterClass
    public static void afterClass() {
        secureServer.stop();
        proxy.stop();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        secureServer = new TrustedServer(9092).start();
        proxy = new Proxy(9093).start();
    }

    protected IClient newClient() {
        return new Client();
    }

    // https://github.com/adamfisk/LittleProxy/pull/208
    @Test
    public void testSimpleImageWithoutManInTheMiddle() throws Exception {
        String url = secureServer.getBaseUrl() + "/" + IMAGE_PATH;
        File direct = newClient().get(url);

        File proxied = newClient().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

}
