[![Build Status](https://travis-ci.org/ganskef/LittleProxy-mitm.png?branch=master)](https://travis-ci.org/ganskef/LittleProxy-mitm)

LittleProxy-mitm is an extension for the [LittleProxy](https://github.com/adamfisk/LittleProxy) which enables Man-In-The-Middle for HTTPS. See [Aldo Cortesi](http://corte.si/posts/code/mitmproxy/howitworks/index.html) for a detailed description of proxy interception processes.

The first run creates the key store for your Certificate Authority. It's used to generate server certificates on the fly. The littleproxy-mitm.pem file have to be imported in your browser or within the systems certificates.


You have to clone the modified version of LittleProxy to build LittleProxy-mitm. This is as simple as:

```
$ git clone git://github.com/ganskef/LittleProxy.git
$ cd LittleProxy
$ mvn clean install
$ cd ..
$ git clone git://github.com/ganskef/LittleProxy-mitm.git
$ cd LittleProxy-mitm
$ mvn clean install
$ java -jar target/littleproxy-mitm-1.1.0-beta1-SNAPSHOT-shade.jar
$ curl -k -x localhost:9090 https://www.google.com/humans.txt
```

Please set your browsers proxy settings to 9090. Its hard coded in the simple Launcher class.


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

Please refer the documentation of [LittleProxy](https://github.com/adamfisk/LittleProxy) to filter HTTP/S contents.

---------------

