[![Build Status](https://travis-ci.org/ganskef/LittleProxy-mitm.png?branch=master)](https://travis-ci.org/ganskef/LittleProxy)

LittleProxy-mitm is an extension for the [LittleProxy](https://github.com/adamfisk/LittleProxy)
which enables Man-In-The-Middle for HTTPS. See 
[http://corte.si/posts/code/mitmproxy/howitworks/index.html](Aldo Cortesi) for 
a detailed description of proxy interception processes.

TODO: Build instructions

One option is to clone LittleProxy and run it from the command line. This is as simple as:

```
$ git clone git://github.com/ganskef/LittleProxy-mitm.git
$ cd LittleProxy-mitm
```

You can embed LittleProxy in your own projects through maven with the following:

```
    <dependency>
        <groupId>org.littleshoot</groupId>
        <artifactId>littleproxy-mitm</artifactId>
        <version>1.1.0-beta1</version>
    </dependency>
```

Once you've included LittleProxy-mitm, you can start the server with the following:

```java
HttpProxyServer server =
    DefaultHttpProxyServer.bootstrap()
        .withPort(8080)
        .withManInTheMiddle(new HostNameMitmManager());
        .start();
```

---------------

