package org.littleshoot.proxy.mitm;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ProxyClientNettyTest {

	/*
	 * Netty client via proxy fails with LittleProxy if MITM enabled
	 * 
	 * I've build a lot of Netty based clients for testing MITM and I have
	 * noticed a difficult, a little volatile problem. This issue is relevant
	 * for the client part of LittleProxy also, to support MITM with Chained
	 * Proxy #202, #87 and Multiple Chained Proxies #195.
	 * 
	 * A simple Netty based client works for me with connecting the proxy
	 * address and retrieving a http request with the address of the upstream
	 * server. This fails with an https URL in connect with a SSL error (ok).
	 * 
	 * Nettys feature request: Client proxy support netty/netty#1133 pronounces
	 * a solution for it. It's available in Netty 5 and 4.1.0.Beta5, but the
	 * milestone is modified to 4.1.0.Beta6. I've tested the same behavior with
	 * Netty 4.1 and 5 against LittleProxy in a separate process:
	 * 
	 * My Netty based clients on top of the new module netty-handler-proxy fails
	 * with LittleProxy with MITM enabled. It fails with https and http, too.
	 * 
	 * It works with LittleProxy without MITM with http and https tunneled. It
	 * works with an other tunneled proxy (GlimmerBlocker). It works with an
	 * other MITM enabled proxy (WWWOFFLE).
	 * 
	 * But, the failure is not happens with an URLConnection based client with
	 * https and http or with any browser I've tried.
	 * 
	 * Based on the working situations, I suppose a problem within LittleProxy.
	 * I would be very glad to see any suggestions.
	 */
	private static final int PROXY_PORT = 9090;

	static final int PROXY_PORT_UNDEFINED = -1;

	protected NettyClient5 newClient() {
		return new NettyClient5();
	}

	@Test
	public void testHttps() throws Exception {
		newClient().get("https://www.freebsd.org/", PROXY_PORT_UNDEFINED);
	}

	@Test
	public void testHttp() throws Exception {
		newClient().get("http://www.freebsd.org/", PROXY_PORT_UNDEFINED);
	}

	@Test
	public void testProxyHttps() throws Exception {
		newClient().get("https://www.freebsd.org/", PROXY_PORT);
	}

	@Test
	public void testProxyHttp() throws Exception {
		newClient().get("http://www.freebsd.org/", PROXY_PORT);
	}

	@Test
	public void testProxyLocal() throws Exception {
		newClient().get("http://localhost/", PROXY_PORT);
	}
}

class NettyClient5 {

	private static final int PROXY_PORT_UNDEFINED = ProxyClientNettyTest.PROXY_PORT_UNDEFINED;

	// single threaded
	private List<Throwable> exceptions = new ArrayList<Throwable>();

	private boolean isSecured(URI uri) {
		return uri.getScheme().equalsIgnoreCase("https");
	}

	File get(String url, final int proxyPort) throws Exception {
		System.out.println(url + " via " + proxyPort);
		System.out.flush();

		final SslContext sslCtx;
		URI uri = new URI(url);
		if (isSecured(uri)) {
			sslCtx = SslContext
					.newClientContext(InsecureTrustManagerFactory.INSTANCE);
		} else {
			sslCtx = null;
		}

		final String peerHost = uri.getHost();
		final int peerPort;
		if (uri.getPort() == PROXY_PORT_UNDEFINED) {
			if (isSecured(uri)) {
				peerPort = 443;
			} else {
				peerPort = 80;
			}
		} else {
			peerPort = uri.getPort();
		}

		final HttpProxyHandler[] proxyHandlers;
		if (proxyPort < 0) {
			proxyHandlers = null;
		} else {
			proxyHandlers = new HttpProxyHandler[] {//
			new HttpProxyHandler(new InetSocketAddress("localhost", proxyPort)),//
			};
		}

		final ChannelHandler handler = new SimpleChannelInboundHandler<HttpObject>() {

			protected void messageReceived(ChannelHandlerContext ctx,
					HttpObject msg) throws Exception {
				if (msg instanceof HttpContent) {
					HttpContent content = (HttpContent) msg;
					System.err.print(content.content().toString(
							CharsetUtil.ISO_8859_1));
					System.err.flush();
					if (content instanceof LastHttpContent) {
						ctx.close();
					}
				}
			}

			@Override
			public void exceptionCaught(ChannelHandlerContext ctx,
					Throwable cause) throws Exception {
				exceptions.add(cause);
				ctx.close();
			}

			protected void channelRead0(ChannelHandlerContext ctx,
					HttpObject msg) throws Exception {
				messageReceived(ctx, msg);
			}
		};

		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group);
			b.channel(NioSocketChannel.class);
			// b.resolver(DefaultNameResolverGroup.INSTANCE);
			b.handler(new ChannelInitializer<Channel>() {
				@Override
				protected void initChannel(Channel ch) {
					ChannelPipeline p = ch.pipeline();
					if (proxyHandlers != null) {
						p.addLast(proxyHandlers);
					}
					if (sslCtx != null) {
						p.addLast(sslCtx.newHandler(ch.alloc()));
					}
					p.addLast(new LoggingHandler(LogLevel.TRACE));
					p.addLast(new HttpClientCodec());
					p.addLast(new HttpContentDecompressor());
					// p.addLast(new HttpObjectAggregator(10 * 1024 *
					// 1024));
					p.addLast(handler);
				}

				@Override
				public void exceptionCaught(ChannelHandlerContext ctx,
						Throwable cause) throws Exception {
					exceptions.add(cause);
					ctx.close();
				}
			});

			Channel ch = b.connect(peerHost, peerPort).sync().channel();

			HttpRequest request = new DefaultFullHttpRequest(
					HttpVersion.HTTP_1_1, HttpMethod.GET, url);
			request.headers().set(HttpHeaderNames.HOST, uri.getHost());
			request.headers().set(HttpHeaderNames.CONNECTION,
					HttpHeaderValues.CLOSE);

			ch.writeAndFlush(request);
			ch.closeFuture().sync();

		} finally {
			group.shutdownGracefully();
		}
		if (!exceptions.isEmpty()) {
			for (Throwable each : exceptions) {
				each.printStackTrace();
			}
			throw new IllegalStateException("Client get failed",
					exceptions.get(0));
		}
		return null;
	}

}
