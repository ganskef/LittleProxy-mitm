[![Build Status](https://travis-ci.org/ganskef/LittleProxy-mitm.png?branch=master)](https://travis-ci.org/ganskef/LittleProxy-mitm)

LittleProxy-mitm is an extension for the [LittleProxy](https://github.com/adamfisk/LittleProxy)
which enables Man-In-The-Middle for HTTPS. See 
[Aldo Cortesi](http://corte.si/posts/code/mitmproxy/howitworks/index.html) for 
a detailed description of proxy interception processes.

The first run creates the key store for your Certificate Authority. It's used to
generate server certificates on the fly. The file (default littleproxy-mitm.pem)
have to be imported in your browser or within the systems certificates.


One option is to clone LittleProxy-mitm and run it from the command line. This is as simple as:

```
$ git clone git://github.com/ganskef/LittleProxy-mitm.git
$ cd LittleProxy-mitm
$ mvn clean install
$ java -jar target/littleproxy-mitm-1.1.0-beta1-SNAPSHOT-shade.jar
```

You can embed LittleProxy-mitm in your own projects through maven with the following:

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

Please refer the documentation of [LittleProxy](https://github.com/adamfisk/LittleProxy)
to filter HTTP/S contents.

---------------

