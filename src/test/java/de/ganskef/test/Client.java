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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private final LogLevel channelLogLevel;

    public Client() {
        this(LogLevel.TRACE);
    }

    public Client(LogLevel channelLogLevel) {
        this.channelLogLevel = channelLogLevel;
    }

    public File get(String url, IProxy proxy, String target) throws Exception {
        URLConnection conn = new URL(url).openConnection(proxy
                .getHttpProxySettings());
        conn.connect();
        OutputStream os = null;
        InputStream is = null;
        try {
            os = new FileOutputStream(target);
            is = conn.getInputStream();
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
            IOUtils.close(conn);
        }
        return new File(target);
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
        if (port == -1) {
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            final boolean ssl = "https".equalsIgnoreCase(scheme);
            if (ssl) {
                port = 443;
            } else {
                port = 80;
            }
        }
        return get(uri, uri.getRawPath(), host, port, target);
    }

    public File get(URI uri, String url, String proxyHost, int proxyPort,
            final String target) throws Exception {

        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();

        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContext
                    .newClientContext(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslCtx = null;
        }

        long ms = System.currentTimeMillis();
        final ClientHandler handler = new ClientHandler(target);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch)
                                throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            p.addLast("log",
                                    new LoggingHandler(channelLogLevel));
                            p.addLast("codec", new HttpClientCodec());
                            p.addLast("inflater", new HttpContentDecompressor());
                            p.addLast("aggregator", new HttpObjectAggregator(
                                    10 * 1024 * 1024));
                            p.addLast("handler", handler);
                        }
                    });

            // Make the connection attempt.
            Channel ch = b.connect(proxyHost, proxyPort).sync().channel();

            // Prepare the HTTP request.
            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
                    HttpMethod.GET, url);
            request.headers().set(HttpHeaders.Names.HOST, uri.getHost());
            request.headers().set(HttpHeaders.Names.CONNECTION,
                    HttpHeaders.Values.CLOSE);
            request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING,
                    HttpHeaders.Values.GZIP);

            log.debug("Before write {}ms", System.currentTimeMillis() - ms);
            // Send the HTTP request.
            ch.writeAndFlush(request);
            log.debug("After write {}ms", System.currentTimeMillis() - ms);

            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } finally {
            log.debug("Before shutdown {}ms", System.currentTimeMillis() - ms);
            // Shut down executor threads to exit.
            group.shutdownGracefully();
            log.info("After shutdown {}ms", System.currentTimeMillis() - ms);
        }
        return handler.getFile();
    }

    public static void main(String[] args) throws Exception {
        new Client().get("https://www.google.com/humans.txt");
    }

}
