package org.littleshoot.proxy.mitm;

import org.junit.Ignore;
import org.junit.Test;

import de.ganskef.test.IClient;
import de.ganskef.test.NettyClient_NoHttps;

/**
 * This test was intended to demonstrate a problem with blocking connections
 * with a Netty based client. This problem is fixed, but there are more issues
 * with mitm and proxied connections.
 */
public class NettyClientTest extends LittleProxyMitmTest {

    @Override
    protected IClient newClient() {
        return new NettyClient_NoHttps();
    }

    @Ignore
    @Test
    @Override
    public void testOnlineTextSecured() throws Exception {
        // disabled, since this client supports no https
    }

    @Ignore
    @Test
    @Override
    public void testCachedResponseSecured() throws Exception {
        // disabled, since this client supports no https
    }

    @Ignore
    @Test
    @Override
    public void testSecuredImage() throws Exception {
        // disabled, since this client supports no https
    }

    @Ignore
    @Test
    @Override
    public void testOnlineServerNameIndicationIssue207() throws Exception {
        // disabled, since this client supports no https
    }

}
