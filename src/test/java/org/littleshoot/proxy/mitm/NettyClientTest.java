package org.littleshoot.proxy.mitm;

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

}
