package org.littleshoot.proxy.mitm;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This test is intended to validate the alternate
 * CertificateSniffingMitmManager implementation.
 */
public class CertificateSniffingTest extends LittleProxyMitmTest {

    /**
     * Shadows the method of the base class to use the alternate MitmManager
     * implementation in the tests.
     */
    @BeforeClass
    public static void initProxy() throws Exception {
        proxy = new LittleProxyMitmProxy(9093, new CertificateSniffingMitmManager()).start();
    }

    @Ignore
    @Test
    @Override
    public void testSimpleImage() throws Exception {
        // Disabled, since it's a duplicated test case (no mitm).
    }

    @Ignore
    @Test
    @Override
    public void testCachedResponse() throws Exception {
        // Disabled, since it's a duplicated test case (no mitm).
    }

    @Ignore
    @Test
    @Override
    public void testCachedResponseSecured() throws Exception {
        // It's impossible to get the session to sniff while offline, so you
        // can't use CertificateSniffingMitmManager for it.
    }

}
