### Issue: Netty client via proxy fails with LittleProxy if MITM enabled

I've build a lot of Netty based clients for testing MITM and I have noticed a difficult, a little volatile problem. This issue is relevant for the client part of LittleProxy also, to support MITM with Chained Proxy adamfisk/LittleProxy#202, adamfisk/LittleProxy#87 and Multiple Chained Proxies adamfisk/LittleProxy#195.

A simple Netty based client works for me with connecting the proxy address and retrieving a http request with the address of the upstream server. This fails with an https URL in connect with a SSL error (ok).

Nettys feature request: Client proxy support netty/netty#1133 pronounces a solution for it. It's available in Netty 5 and 4.1.0.Beta5, but the milestone is modified to 4.1.0.Beta6. I've tested the same behavior with Netty 4.1 and 5 against LittleProxy in a separate process:

 * My Netty based clients on top of the new module netty-handler-proxy fails with LittleProxy with MITM enabled. It fails with https and http, too.

 * It works with LittleProxy without MITM with http and https tunneled. It works with an other tunneled proxy (GlimmerBlocker). It works with an other MITM enabled proxy (WWWOFFLE).

 * But, the failure is not happens with an URLConnection based client with https and http or with any browser I've tried. 

Based on the working situations, I suppose a problem within LittleProxy.

**I would be very glad to see any suggestions.**
---
[![Build Status](https://travis-ci.org/ganskef/LittleProxy-parent.png?branch=master)](https://travis-ci.org/ganskef/LittleProxy-parent)

LittleProxy-mitm is an extension for [LittleProxy](https://github.com/adamfisk/LittleProxy) which enables Man-In-The-Middle. It provides for so all the filter capabilities of LittleProxy with HTTPS sites, too. See [Aldo Cortesi](http://corte.si/posts/code/mitmproxy/howitworks/index.html) for a detailed description of proxy interception processes. 

**Please use your browser directly for every security-critical transmission.** Mozilla Firefox and Google Chrome implements her own certificate handling for a reason. Handling security in Java like here must be less secure in most situations. See http://www.cs.utexas.edu/~shmat/shmat_ccs12.pdf "The Most Dangerous Code in the World: Validating SSL Certificates in Non-Browser Software".

The first run creates the key store for your Certificate Authority. It's used to generate server certificates on the fly. The ```littleproxy-mitm.pem``` file have to be imported in your browser or within the systems certificates, Mozilla for example:

<img src="https://github.com/ganskef/LittleProxy-mitm/blob/master/import-mozilla-1.png" height="210">
<img src="https://github.com/ganskef/LittleProxy-mitm/blob/master/import-mozilla-2.png" height="210">

Please set your browsers proxy settings to 9090. It's hard coded in the simple Launcher class. You may chose an other implementation, of course.


The MITM feature depends on the unreleased version of LittleProxy with little modifications. Please consider to use [ganskef/LittleProxy-parent](https://github.com/ganskef/LittleProxy-parent) to build both.


You can embed LittleProxy-mitm in your own projects only by cloning it, for this reason. It's not available in a public Maven repository. See LittleProxy CR [#173](https://github.com/adamfisk/LittleProxy/issues/173) and PR [#174](https://github.com/adamfisk/LittleProxy/pull/174).


Once you've included LittleProxy-mitm, you can start the server with the following:

```java
HttpProxyServer server =
    DefaultHttpProxyServer.bootstrap()
        .withPort(8080) // for both HTTP and HTTPS
        .withManInTheMiddle(new HostNameMitmManager())
        .start();
```

Please refer to the documentation of [LittleProxy](https://github.com/adamfisk/LittleProxy) to filter HTTP/S contents.

###### Known Problems

 * Connection failure with some HTTPS sites like https://www.archlinux.org/ for example. You have to use [Java Cryptography Extension](http://en.wikipedia.org/wiki/Java_Cryptography_Extension) to fix it.
```
387481 2015-05-19 21:34:39,061 WARN  [LittleProxy-ProxyToServerWorker-6] impl.ProxyToServerConnection - (HANDSHAKING) [id: 0x7e0de7f2, /192.168.178.30:1475 => www.archlinux.org/66.211.214.131:443]: Caught exception on proxy -> web connection
io.netty.handler.codec.DecoderException: java.lang.RuntimeException: Could not generate DH keypair
    at io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:346)
...
Caused by: java.security.InvalidAlgorithmParameterException: Prime size must be multiple of 64, and can only range from 512 to 1024 (inclusive)
    at com.sun.crypto.provider.DHKeyPairGenerator.initialize(DHKeyPairGenerator.java:120)
...
```
 * The LittleProxy features mitm and proxy chaining are mutually exclusive. See the issue [#1133](https://github.com/netty/netty/issues/1133) - Netty Feature request: Client proxy support - for a possibly solution resolved in Netty 5.
 * I'm not a natural English speaker/writer. So feel free to fix me if I'm wrong (or always in generally) and don't feel sad about a phrase.

----
