package de.ganskef.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;
import java.net.URI;

public class Client2 {

    private final LogLevel channelLogLevel;

    public Client2() {
        this(LogLevel.TRACE);
    }

    public Client2(LogLevel channelLogLevel) {
        this.channelLogLevel = channelLogLevel;
    }

    public File get(String url, IProxy proxy, String target) throws Exception {
        return get(new URI(url), url, "127.0.0.1", proxy.getProxyPort(), target);
    }

    public File get(String url, IProxy proxy) throws Exception {
        return get(url, proxy, "proxy.out");
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

    public File get(URI uri, String url, String proxyHost, int proxyPort,
            final String target) throws Exception {

        if (proxyPort == -1) {
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                proxyPort = 443;
            } else {
                proxyPort = 80;
            }
        }

        final Client2Handler handler = new Client2Handler(target);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch)
                                throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast("log",
                                    new LoggingHandler(channelLogLevel));
                            p.addLast("codec", new HttpClientCodec());
                            p.addLast("inflater", new HttpContentDecompressor());
                            p.addLast("aggregator", new HttpObjectAggregator(
                                    10 * 1024 * 1024));
                            p.addLast("handler", handler);
                        }
                    });

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
