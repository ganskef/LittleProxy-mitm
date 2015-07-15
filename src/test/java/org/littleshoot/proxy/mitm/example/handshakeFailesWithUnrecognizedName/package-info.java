/**
 * The SNI implementation of Java 7 terminates with some misconfigured hosts on
 * receiving a "unrecognized_name" warning. See: <a href="<http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7127374">http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7127374</a>
 * I'm searching for a solution to reconnect if this happens.
 * 
 * <p><a href="https://github.com/Lekensteyn/OWASP-WebScarab/commit/8f2362eb021924cece9fb544f04bde5da7bfed4a">https://github.com/Lekensteyn/OWASP-WebScarab/commit/8f2362eb021924cece9fb544f04bde5da7bfed4a</a>
 * This describes a retry with a SSLSocket, but Netty is nonblocking using SSLEngine.
 * 
 * <p><a href="http://stackoverflow.com/questions/28770962/how-to-handle-ssl-handshake-failure-in-netty">http://stackoverflow.com/questions/28770962/how-to-handle-ssl-handshake-failure-in-netty</a>
 * This shows how to detect a handshake failure, but not how to react.
 * 
 * <p><a href="http://stackoverflow.com/questions/19739054/whats-the-best-way-to-reconnect-after-connection-closed-in-netty">
 * http://stackoverflow.com/questions/19739054/whats-the-best-way-to-reconnect-after-connection-closed-in-netty</a>
 * This shows how to reconnect a client with a new bootstrap.
 * 
 * <p><a href="http://netty.io/4.0/xref/io/netty/example/uptime/UptimeClientHandler.html">http://netty.io/4.0/xref/io/netty/example/uptime/UptimeClientHandler.html</a>
 * An example in Netty with uses the EventLoop from the Channel to restart with
 * a new Bootstrap, too. FIXME I have to use the EventLoopGroup to avoid
 * blocking with HTTPS, see: RetryClient.retry(EventLoop)
 * 
 * <p><a href="http://tterm.blogspot.de/2014/03/netty-tcp-client-with-reconnect-handling.html">http://tterm.blogspot.de/2014/03/netty-tcp-client-with-reconnect-handling.html</a>
 * An other example oft nearly the same.
 * 
 * <p><a href="https://github.com/netty/netty/issues/591">https://github.com/netty/netty/issues/591</a>
 * Could I reuse a bootstrap to reconnect?
 * 
 * <p><a href="https://github.com/grpc/grpc-java/blob/8f537e3ec6be1df01ea98f8f2e53eb233c3eb9b6/netty/src/main/java/io/grpc/transport/netty/Http2Negotiator.java#L123">https://github.com/grpc/grpc-java/blob/8f537e3ec6be1df01ea98f8f2e53eb233c3eb9b6/netty/src/main/java/io/grpc/transport/netty/Http2Negotiator.java#L123</a>
 * If I understand right, this code replace the sslHandler in the pipeline (in 
 * case of a handshake failure?).
 */
package org.littleshoot.proxy.mitm.example.handshakeFailesWithUnrecognizedName;

