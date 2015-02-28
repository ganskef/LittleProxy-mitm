package org.littleshoot.proxy.mitm;

import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class Launcher {

	public static void main(final String... args) {
		org.littleshoot.proxy.Launcher.pollLog4JConfigurationFileIfAvailable();
		try {
			final int port = 9090;

			System.out.println("About to start server on port: " + port);
			HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrapFromFile("./littleproxy.properties")
					.withPort(port).withAllowLocalOnly(false);

			bootstrap.withManInTheMiddle(new HostNameMitmManager());

			System.out.println("About to start...");
			bootstrap.start();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
