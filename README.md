[![Build Status](https://travis-ci.org/ganskef/LittleProxy-parent.png?branch=master)](https://travis-ci.org/ganskef/LittleProxy-parent)

LittleProxy-mitm is an extension for 
[LittleProxy](https://github.com/adamfisk/LittleProxy) which enables 
Man-In-The-Middle. It provides for so all the filter capabilities of LittleProxy 
with HTTPS sites, too. See 
[Aldo Cortesi](http://corte.si/posts/code/mitmproxy/howitworks/index.html) for a 
detailed description of proxy interception processes. 

**Please use your browser directly for every security-critical transmission.** 
Mozilla Firefox and Google Chrome implements her own certificate handling for a 
reason. Handling security in Java like here must be less secure in most 
situations. See http://www.cs.utexas.edu/~shmat/shmat_ccs12.pdf "The Most 
Dangerous Code in the World: Validating SSL Certificates in Non-Browser 
Software".

The first run creates the key store for your Certificate Authority. It's used to 
generate server certificates on the fly. The ```littleproxy-mitm.pem``` file 
have to be imported in your browser or within the systems certificates, Mozilla 
for example:

<img src="https://github.com/ganskef/LittleProxy-mitm/blob/master/import-mozilla-1.png" height="210">
<img src="https://github.com/ganskef/LittleProxy-mitm/blob/master/import-mozilla-2.png" height="210">

Please set your browsers proxy settings to 9090. It's hard coded in the simple 
Launcher class. You may chose an other implementation, of course.


The MITM feature depends on the unreleased version of LittleProxy with little 
modifications. Please consider to use 
[ganskef/LittleProxy-parent](https://github.com/ganskef/LittleProxy-parent) to 
build both.


You can embed LittleProxy-mitm in your own projects only by cloning it, for this 
reason. It's not available in a public Maven repository. See LittleProxy CR 
[#173](https://github.com/adamfisk/LittleProxy/issues/173) and PR 
[#174](https://github.com/adamfisk/LittleProxy/pull/174).


Once you've included LittleProxy-mitm, you can start the server with the following:

```java
HttpProxyServer server =
    DefaultHttpProxyServer.bootstrap()
        .withPort(8080) // for both HTTP and HTTPS
        .withManInTheMiddle(new HostNameMitmManager())
        .start();
```

Alternatively you can use `CertificateSniffingMitmManager` which copies the 
Common Name and Subject Alternative Names from the upstream certificate.

Please give an `Authority` in the constructor to personalize your application. 
You impersonate certificates which is normally a bad thing. You have to describe 
the reason for.

Please refer to the documentation of 
[LittleProxy](https://github.com/adamfisk/LittleProxy) and the Javadoc of 
`org.littleshoot.proxy.HttpFilters` to filter HTTP/S contents.

Mostly you will need an URL to handle content in your filters. With HTTP it's 
provided by `originalRequest.getUri()`, but with HTTPS you have to get the host 
name from the initiating `CONNECT` request. Therefore you have to do something 
like this in your `FiltersSource` implementation: 

```java
    private static final AttributeKey<String> CONNECTED_URL = AttributeKey.valueOf("connected_url");

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext clientCtx) {
        String uri = originalRequest.getUri();
        if (originalRequest.getMethod() == HttpMethod.CONNECT) {
            if (clientCtx != null) {
                String prefix = "https://" + uri.replaceFirst(":443", "");
                clientCtx.channel().attr(CONNECTED_URL).set(prefix);
            }
            return new HttpFiltersAdapter(originalRequest, clientCtx);
        }
        String connectedUrl = clientCtx.channel().attr(CONNECTED_URL).get();
        if (connectedUrl == null) {
            return new MyHttpFilters(uri);
        }
        return new MyHttpFilters(connectedUrl + uri);
    }
```

 * On `CONNECT` you must **always** return a `HttpFiltersAdapter`, since it has 
 to  bypass all filtering. 
 * Without a saved `connected_url` in the context it's plain HTTP, no HTTPS.
 * Following requests on this channel have to concatenate the saved 
 `connected_url` with the URI from the `originalRequest`.

###### Known Problems

 * Using Mozilla Firefox (> 39) with the proxy running on Android (Version 
4.4.4) I've seen a general SSL connection failure with error code
`ssl_error_weak_server_ephemeral_dh_key`. A workaround found in the Web is to
open `about:config`, search for `ssl3` and disable the first two entries 
containing `dhe_rsa`:

<img src="https://github.com/ganskef/LittleProxy-mitm/blob/master/ssl-weak-android.png" width="350">
<img src="https://github.com/ganskef/LittleProxy-mitm/blob/master/ssl-weak-android-fix.png" width="350">

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
 * The LittleProxy features mitm and proxy chaining are mutually exclusive. See 
 the issue [#1133](https://github.com/netty/netty/issues/1133) - Netty Feature 
 request: Client proxy support - for a possibly solution resolved in Netty 5.
 * I'm not a natural English speaker/writer. So feel free to fix me if I'm wrong 
 (or always in generally) and don't feel sad about a phrase.

----
