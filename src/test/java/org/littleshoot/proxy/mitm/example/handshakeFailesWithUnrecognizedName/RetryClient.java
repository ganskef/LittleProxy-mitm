package org.littleshoot.proxy.mitm.example.handshakeFailesWithUnrecognizedName;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.File;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example client with handling for reconnect after a handshake failure
 * unrecognized_name occurs.
 * 
 * FIXME state in member variables is save for only one request, I think. It
 * should be handled within Netty instead.
 */
public class RetryClient {

    private static final Logger LOG = LoggerFactory
            .getLogger(RetryClient.class);

    private EventLoopGroup group = new NioEventLoopGroup();

    private URI uri;

    private SslContext sslCtx;

    public static void main(String[] args) throws Exception {
        new RetryClient().get("https://wiki.gnome.org/");
    }

    public File get(String url) throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)) {
            LOG.info("Only HTTP(S) is supported.");
            return null;
        }
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port;
        if (uri.getPort() == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }
        this.uri = new URI(scheme, uri.getUserInfo(), host, port,
                uri.getPath(), uri.getQuery(), uri.getFragment());

        final boolean ssl = "https".equalsIgnoreCase(scheme);
        if (ssl) {
            sslCtx = SslContext
                    .newClientContext(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslCtx = null;
        }

        connect(false, group);

        return null;
    }

    private void connect(final boolean retry, EventLoopGroup loop)
            throws InterruptedException {
        Bootstrap b = new Bootstrap();
        b.group(loop);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    if (retry) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    } else {
                        p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(),
                                uri.getPort()));
                    }
                }
                p.addLast(new HttpClientCodec());
                p.addLast(new RetryClientHandler(RetryClient.this));
            }
        });
        Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.GET, uri.toASCIIString());
        request.headers().set(HttpHeaders.Names.HOST, uri.getHost());
        request.headers().set(HttpHeaders.Names.CONNECTION,
                HttpHeaders.Values.CLOSE);

        ch.writeAndFlush(request);
        ch.closeFuture().sync();
    }

    void retry(EventLoop loop) throws InterruptedException {
        // FIXME the loop from the first connect is blocking (?) so use the
        // group here
        //
        connect(true, group);
    }

    void stop() {
        group.shutdownGracefully();
    }

}

class RetryClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger LOG = LoggerFactory
            .getLogger(RetryClientHandler.class);

    private RetryClient client;

    public RetryClientHandler(RetryClient client) {
        this.client = client;
    }

    private boolean unrecognizedName;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        LOG.info(">>> userEventTriggered " + evt);
        if (evt instanceof SslHandshakeCompletionEvent) {
            SslHandshakeCompletionEvent hce = (SslHandshakeCompletionEvent) evt;
            if (!hce.isSuccess()
                    && hce.cause().getMessage().contains("unrecognized_name")) {
                LOG.info(">>> unrecognized_name");
                ctx.close();
                unrecognizedName = true;
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        if (!unrecognizedName) {
            super.exceptionCaught(ctx, cause);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info(">>> channelUnregistered");
        if (unrecognizedName) {
            LOG.info(">>> unrecognizedName retry");
            final EventLoop loop = ctx.channel().eventLoop();
            loop.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        client.retry(loop);
                    } catch (InterruptedException e) {
                        LOG.info(">>> retry interrupted, shutdown");
                        client.stop();
                    }
                }
            });
        } else {
            LOG.info(">>> shutdown sucessfully");
            client.stop();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        LOG.info(String.valueOf(msg));
        if (msg instanceof LastHttpContent) {
            LOG.info(">>> close sucessfully");
            ctx.close();
        }
    }

}
