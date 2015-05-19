package de.ganskef.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
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
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.channels.FileChannel;

import org.apache.commons.io.IOUtils;

public class NettyClient_NoHttps implements IClient {

    public File get(String url, IProxy proxy, String target) throws Exception {
        return get(new URI(url), url, "127.0.0.1", proxy.getProxyPort(), target);
    }

    public File get(String url, IProxy proxy) throws Exception {
        return get(new URI(url), url, "127.0.0.1", proxy.getProxyPort(),
                "proxy.out");
    }

    public File get(String url) throws Exception {
        return get(url, "client.out");
    }

    public File get(String url, String target) throws Exception {
        URI uri = new URI(url);
        String host = uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if (isSecured(uri)) {
                port = 443;
            } else {
                port = 80;
            }
        }
        return get(uri, uri.getRawPath(), host, port, target);
    }

    private boolean isSecured(URI uri) {

        // XXX https via offline proxy won't work with this client. I mean, this
        // was my experience while debugging it. I had no success with Apache
        // HC, too. Only URLConnection works like expected for me.
        //
        // It seems to me we have to wait for a proper solution - see:
        // https://github.com/netty/netty/issues/1133#event-299614098
        // normanmaurer modified the milestone: 4.1.0.Beta5, 4.1.0.Beta6
        //
        // return uri.getScheme().equalsIgnoreCase("https");

        return false;
    }

    private File get(URI uri, String url, String proxyHost, int proxyPort,
            final String target) throws Exception {
        final SslContext sslCtx;
        if (isSecured(uri)) {
            sslCtx = SslContext
                    .newClientContext(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslCtx = null;
        }
        final NettyClientHandler handler = new NettyClientHandler(target);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new NettyClientInitializer(sslCtx, handler));
            // .handler(new HttpSnoopClientInitializer(sslCtx));

            Channel ch = b.connect(proxyHost, proxyPort).sync().channel();

            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, url);
            request.headers().set(HttpHeaders.Names.HOST, uri.getHost());
            request.headers().set(HttpHeaders.Names.CONNECTION,
                    HttpHeaders.Values.CLOSE);
            request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING,
                    HttpHeaders.Values.GZIP);

            ch.writeAndFlush(request);
            ch.closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }
        return handler.getFile();
    }

}

class NettyClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private File file;

    public NettyClientHandler(String target) {
        File dir = new File("src/test/resources/tmp");
        dir.mkdirs();
        file = new File(dir, target);
        file.delete();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
            throws Exception {
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            RandomAccessFile output = null;
            FileChannel oc = null;
            try {
                output = new RandomAccessFile(file, "rw");
                oc = output.getChannel();
                oc.position(oc.size());
                ByteBuf buffer = content.content();
                for (int i = 0, len = buffer.nioBufferCount(); i < len; i++) {
                    oc.write(buffer.nioBuffers()[i]);
                }
            } finally {
                IOUtils.closeQuietly(oc);
                IOUtils.closeQuietly(output);
            }
            if (content instanceof LastHttpContent) {
                ctx.close();
            }
        }
    }

    public File getFile() {
        return file;
    }

}

class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    private ChannelHandler handler;

    private SslContext sslCtx;

    public NettyClientInitializer(SslContext sslCtx, ChannelHandler handler) {
        this.sslCtx = sslCtx;
        this.handler = handler;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast("log", new LoggingHandler(LogLevel.TRACE));
        p.addLast("codec", new HttpClientCodec());
        p.addLast("inflater", new HttpContentDecompressor());
        // p.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
        p.addLast("handler", handler);
    }

}