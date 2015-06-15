package org.littleshoot.proxy.mitm.example.handshakeFailesWithUnrecognizedName;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
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

import de.ganskef.test.Client;

/**
 * An example client with handling for reconnect after a handshake failure
 * unrecognized_name occurs.
 * 
 * It works, but... Restarting the event loop group on unrecognized name which
 * is a sledge hammer solution.
 */
public class RestartClient extends Client {

    private static final Logger LOG = LoggerFactory
            .getLogger(RestartClient.class);

    private SslContext sslCtx;
    private String host;
    private int port;
    private String url;
    private boolean unrecognizedName;

    public RestartClient() {
        this(false);
    }

    public RestartClient(boolean unrecognizedName) {
        this.unrecognizedName = unrecognizedName;
    }

    public static void main(String[] args) throws Exception {
        new RestartClient().get("https://wiki.gnome.org/");
    }

    @Override
    public File get(final String url) throws Exception {
        this.url = url;
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
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

        if (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)) {
            LOG.warn("Only HTTP(S) is supported.");
            return null;
        }

        final boolean ssl = "https".equalsIgnoreCase(scheme);
        if (ssl) {
            sslCtx = SslContext
                    .newClientContext(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslCtx = null;
        }

        final EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group);
            b.channel(NioSocketChannel.class);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        if (unrecognizedName) {
                            p.addLast(sslCtx.newHandler(ch.alloc()));
                        } else {
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                        }
                    }
                    p.addLast(new HttpClientCodec());
                    p.addLast(new HandshakeClientHandler());
                }
            });
            Channel ch = b.connect(host, port).sync().channel();
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, url);
            request.headers().set(HttpHeaders.Names.HOST, host);
            request.headers().set(HttpHeaders.Names.CONNECTION,
                    HttpHeaders.Values.CLOSE);

            ch.writeAndFlush(request);
            ch.closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }
        return null;
    }

    class HandshakeClientHandler extends SimpleChannelInboundHandler<Object> {

        private boolean unrecognizedName;

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
                throws Exception {
            LOG.info(">>> userEventTriggered " + evt);
            if (evt instanceof SslHandshakeCompletionEvent) {
                SslHandshakeCompletionEvent hce = (SslHandshakeCompletionEvent) evt;
                if (!hce.isSuccess()
                        && hce.cause().getMessage()
                                .contains("unrecognized_name")) {
                    LOG.info(">>> unrecognized_name");
                    ctx.close();
                    unrecognizedName = true;
                    return;
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            LOG.info(">>> handlerAdded");
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            LOG.info(">>> channelActive");
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            LOG.info(">>> channelInactive Client disconnected!");
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx)
                throws Exception {
            LOG.info(">>> channelUnregistered");
            if (unrecognizedName) {
                LOG.info(">>> unrecognizedName retry");
                new RestartClient(unrecognizedName).get(url);
            }
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg)
                throws Exception {
            LOG.info(">>> channelRead0");
            LOG.info(String.valueOf(msg));
            if (msg instanceof LastHttpContent) {
                LOG.info(">>> close sucessfully");
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOG.warn(">>> exceptionCaught");
            if (unrecognizedName) {
                return; // ignore this exception
            }
            cause.printStackTrace();
            ctx.close();
        }
    }

}
