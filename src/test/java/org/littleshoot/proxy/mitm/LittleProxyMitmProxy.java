package org.littleshoot.proxy.mitm;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.xml.DOMConfigurator;
import org.littleshoot.proxy.DefaultHostResolver;
import org.littleshoot.proxy.HostResolver;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyUtils;

import de.ganskef.test.IProxy;
import de.ganskef.test.Server;

public class LittleProxyMitmProxy extends de.ganskef.test.Proxy implements
        IProxy {

    private boolean connectionLimited;

    private final MitmManager mitmManager;

    public LittleProxyMitmProxy(int proxyPort) throws Exception {
        this(proxyPort, new CertificateSniffingMitmManager());
    }

    public LittleProxyMitmProxy(int proxyPort, MitmManager mitmManager) {
        super(proxyPort);
        this.mitmManager = mitmManager;
    }

    @Override
    public LittleProxyMitmProxy start() {
        return (LittleProxyMitmProxy) super.start();
    }

    protected HttpProxyServerBootstrap bootstrap() {

        HttpFiltersSource filtersSource = new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest,
                    ChannelHandlerContext ctx) {

                // The connect request must bypass the filter! Otherwise the
                // handshake will fail.
                //
                if (ProxyUtils.isCONNECT(originalRequest)) {
                    return new HttpFiltersAdapter(originalRequest);
                }

                return new HttpFiltersAdapter(originalRequest) {

                    /**
                     * This filter delivers special responses if connection
                     * limited
                     */
                    @Override
                    public HttpResponse clientToProxyRequest(
                            HttpObject httpObject) {
                        if (isConnectionLimited()) {
                            return createOfflineResponse();
                        } else {
                            return super.clientToProxyRequest(httpObject);
                        }
                    }

                    /**
                     * This proxy expect aggregated chunks only, with https too
                     */
                    @Override
                    public HttpObject proxyToClientResponse(
                            HttpObject httpObject) {
                        if (httpObject instanceof FullHttpResponse) {
                            return super.proxyToClientResponse(httpObject);
                        } else {
                            throw new IllegalStateException(
                                    "Response is not been aggregated");
                        }
                    }
                };
            }

            /** This proxy must aggregate chunks */
            @Override
            public int getMaximumResponseBufferSizeInBytes() {
                return 10 * 1024 * 1024;
            }
        };

        HostResolver serverResolver = new DefaultHostResolver() {
            /** This proxy uses unresolved adresses while offline */
            @Override
            public InetSocketAddress resolve(String host, int port)
                    throws UnknownHostException {
                if (isConnectionLimited()) {
                    return new InetSocketAddress(host, port);
                }
                return super.resolve(host, port);
            }
        };

        return DefaultHttpProxyServer
                .bootstrap()
                .withFiltersSource(filtersSource)
                .withPort(getProxyPort())
                .withServerResolver(serverResolver)
                .withManInTheMiddle(mitmManager);

    }

    public boolean isConnectionLimited() {
        return connectionLimited;
    }

    public void setConnectionLimited() {
        connectionLimited = true;
    }

    public void setConnectionUnlimited() {
        connectionLimited = false;
    }

    private HttpResponse createOfflineResponse() {
        ByteBuf buffer = Unpooled.wrappedBuffer("Offline response".getBytes());
        HttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        HttpHeaders.setContentLength(response, buffer.readableBytes());
        HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE,
                "text/html");
        return response;
    }

    public static void main(final String... args) throws Exception {
        File log4jConfigurationFile = new File("src/test/resources/log4j.xml");
        if (log4jConfigurationFile.exists()) {
            DOMConfigurator.configureAndWatch(
                    log4jConfigurationFile.getAbsolutePath(), 15);
        }
        new LittleProxyMitmProxy(9090).start();
        Server.waitUntilInterupted();
    }

}
