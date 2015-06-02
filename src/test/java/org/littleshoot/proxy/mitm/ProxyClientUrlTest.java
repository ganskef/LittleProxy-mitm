package org.littleshoot.proxy.mitm;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class ProxyClientUrlTest extends ProxyClientNettyTest {

	@Override
	protected NettyClient5 newClient() {
		return new UrlClient();
	}

}

class UrlClient extends NettyClient5 {

	private static final int PROXY_PORT_UNDEFINED = ProxyClientNettyTest.PROXY_PORT_UNDEFINED;

	@Override
	File get(String url, int proxyPort) throws Exception {
		System.out.println(url + " via " + proxyPort);
		System.out.flush();
		return get(new URI(url), proxyPort);
	}

	private File get(URI uri, int proxyPort) throws Exception {
		URLConnection con = createConnection(uri.toURL(), proxyPort);
		final boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
		if (ssl) {
			return callHttpsGet(con);
		} else {
			return callHttpGet(con);
		}
	}

	private URLConnection createConnection(URL url, int proxyPort)
			throws MalformedURLException, IOException {
		URLConnection result;
		if (proxyPort != PROXY_PORT_UNDEFINED) {
			SocketAddress sa = new InetSocketAddress(proxyPort);
			Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
			result = url.openConnection(proxy);
		} else {
			result = url.openConnection();
		}
		return result;
	}

	private File callHttpGet(URLConnection con) throws IOException {
		con.connect();
		return read(con);
	}

	private File callHttpsGet(URLConnection con)
			throws GeneralSecurityException, IOException {
		SSLContext context = initSslContext();

		SSLSocketFactory sslSocketFactory = context.getSocketFactory();

		((HttpsURLConnection) con).setSSLSocketFactory(sslSocketFactory);
		con.connect();
		return read(con);
	}

	protected SSLContext initSslContext() throws GeneralSecurityException,
			IOException {
		SSLContext context = SSLContext.getInstance("TLS");

		// String algorithm = TrustManagerFactory.getDefaultAlgorithm();
		// TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		// tmf.init((KeyStore) null);

		TrustManagerFactory tmf = InsecureTrustManagerFactory.INSTANCE;

		TrustManager[] trustManagers = tmf.getTrustManagers();
		context.init(null, trustManagers, null);
		return context;
	}

	private File read(URLConnection con) throws IOException {
		BufferedReader input = null;
		try {
			input = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			while (input.ready()) {
				System.err.println(input.readLine());
			}
		} finally {
			System.err.flush();
			if (input != null) {
				input.close();
			}
		}
		return null;
	}
}