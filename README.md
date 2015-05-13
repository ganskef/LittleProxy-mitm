[![Build Status](https://travis-ci.org/ganskef/LittleProxy-parent.png?branch=master)](https://travis-ci.org/ganskef/LittleProxy-parent)

LittleProxy-mitm is an extension for the [LittleProxy](https://github.com/adamfisk/LittleProxy) which enables Man-In-The-Middle for HTTPS. See [Aldo Cortesi](http://corte.si/posts/code/mitmproxy/howitworks/index.html) for a detailed description of proxy interception processes.

The first run creates the key store for your Certificate Authority. It's used to generate server certificates on the fly. The ```littleproxy-mitm.pem``` file have to be imported in your browser or within the systems certificates.

Please set your browsers proxy settings to 9090. It's hard coded in the simple Launcher class. You may chose an other implementation, of course.


The MITM feature depends on the unreleased version of LittleProxy with little modifications. Please consider to use [ganskef/LittleProxy-parent](https://github.com/ganskef/LittleProxy-parent) to build both.


You can embed LittleProxy-mitm in your own projects only by cloning it, for this reason. It's not available in a public Maven repository. See LittleProxy CR [#173](https://github.com/adamfisk/LittleProxy/issues/173) and PR [#174](https://github.com/adamfisk/LittleProxy/pull/174).


Once you've included LittleProxy-mitm, you can start the server with the following:

```java
HttpProxyServer server =
    DefaultHttpProxyServer.bootstrap()
        .withPort(8080)
        .withManInTheMiddle(new HostNameMitmManager());
        .start();
```

Please refer the documentation of [LittleProxy](https://github.com/adamfisk/LittleProxy) to filter HTTP/S contents.

---------------

