package org.littleshoot.proxy.mitm;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;

import java.net.InetSocketAddress;
import java.net.Proxy.Type;

import org.littleshoot.proxy.ActivityTracker;
import org.littleshoot.proxy.ActivityTrackerAdapter;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.HostNameMitmManager;
import org.littleshoot.proxy.mitm.RootCertificateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ganskef.test.IProxy;

public class LittleProxyMitmProxy implements IProxy {

	private static final Logger log = LoggerFactory.getLogger(LittleProxyMitmProxy.class);

	private HttpProxyServer server;

	private final int proxyPort;

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
	public IProxy start() {
		if (server != null) {
			server.stop();
		}
		server = bootstrap().start();
		return this;
	}

	protected HttpProxyServerBootstrap bootstrap() {
		HttpFiltersSource filtersSource = new HttpFiltersSourceAdapter();
		ActivityTracker activityTracker = new ActivityTrackerAdapter() {
			@Override
			public void bytesSentToClient(FlowContext flowContext, int numberOfBytes) {
				log.warn("Bytes sent to client {}", numberOfBytes);
			}

			@Override
			public void responseSentToClient(FlowContext flowContext, HttpResponse httpResponse) {
				log.warn("Response sent to client {}", httpResponse);
				if (httpResponse instanceof HttpContent) {
					HttpContent content = (HttpContent) httpResponse;
					ByteBuf buffer = content.content();
					log.warn("Response sent to client {} {}", buffer.capacity(), buffer.nioBufferCount());
				}
			}
		};
		try {
			return DefaultHttpProxyServer.bootstrap() //
					.plusActivityTracker(activityTracker) //
					.withFiltersSource(filtersSource) //
					.withManInTheMiddle(new HostNameMitmManager(new Authority())).withPort(proxyPort);

		} catch (RootCertificateException e) {
			throw new IllegalStateException("Could not enable Man-In-The-Middle", e);
		}
	}

	@Override
	public void stop() {
		server.stop();
	}

	@Override
	public java.net.Proxy getHttpProxySettings() {
		InetSocketAddress isa = server.getListenAddress();
		return new java.net.Proxy(Type.HTTP, isa);
	}

}
