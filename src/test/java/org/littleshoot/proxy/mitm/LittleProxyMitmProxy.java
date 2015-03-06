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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.littleshoot.proxy.DefaultHostResolver;
import org.littleshoot.proxy.HostResolver;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import de.ganskef.test.IProxy;

public class LittleProxyMitmProxy implements IProxy {

    private HttpProxyServer server;

    private final int proxyPort;

    private boolean connectionLimited;

    public LittleProxyMitmProxy() {
        this(9091);
    }

    public LittleProxyMitmProxy(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @Override
    public int getProxyPort() {
        return proxyPort;
    }

    @Override
    public LittleProxyMitmProxy start() {
        if (server != null) {
            server.stop();
        }
        server = bootstrap().start();
        return this;
    }

    protected HttpProxyServerBootstrap bootstrap() {

        HttpFiltersSource filtersSource = new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest,
                    ChannelHandlerContext ctx) {
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

        try {
            return DefaultHttpProxyServer
                    .bootstrap()
                    .withFiltersSource(filtersSource)
                    .withPort(proxyPort)
                    .withServerResolver(serverResolver)
                    .withManInTheMiddle(
                            new HostNameMitmManager(new Authority()));

        } catch (RootCertificateException e) {
            throw new IllegalStateException(
                    "Could not enable Man-In-The-Middle", e);
        }
    }

    @Override
    public void stop() {
        server.stop();
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

}
