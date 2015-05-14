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
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.channels.FileChannel;

import org.apache.commons.io.IOUtils;

public class NettyClient_NoHttps {

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
        return get(uri, uri.getRawPath(), host, port, target);
    }

    private File get(URI uri, String url, String proxyHost, int proxyPort,
            final String target) throws Exception {
        if (url.toLowerCase().startsWith("https")) {
            System.out.println("HTTPS is not supported, try HTTP for " + url);
        }
        if (proxyPort == -1) {
            proxyPort = 80;
        }
        final NettyClientHandler handler = new NettyClientHandler(target);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new NettyClientInitializer(handler));

            Channel ch = b.connect(proxyHost, proxyPort).sync().channel();

            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                    HttpMethod.GET, url);
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
        }
    }

    public File getFile() {
        return file;
    }
}

class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    private ChannelHandler handler;

    public NettyClientInitializer(ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("log", new LoggingHandler(LogLevel.TRACE));
        p.addLast("codec", new HttpClientCodec());
        p.addLast("inflater", new HttpContentDecompressor());
        // p.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
        p.addLast("handler", handler);
    }

}