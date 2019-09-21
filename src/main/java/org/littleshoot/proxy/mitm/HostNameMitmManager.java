package org.littleshoot.proxy.mitm;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.littleshoot.proxy.MitmManager;

import io.netty.handler.codec.http.HttpRequest;

/**
 * {@link MitmManager} that uses the given host name to create a dynamic
 * certificate for. If a port is given, it will be truncated.
 */
public class HostNameMitmManager implements MitmManager {

    private BouncyCastleSslEngineSource sslEngineSource;

    public HostNameMitmManager() throws RootCertificateException {
        this(new Authority());
    }

    public HostNameMitmManager(Authority authority)
            throws RootCertificateException {
        try {
            boolean trustAllServers = false;
            boolean sendCerts = true;
            sslEngineSource = new BouncyCastleSslEngineSource(authority,
                    trustAllServers, sendCerts);
        } catch (final Exception e) {
            throw new RootCertificateException(
                    "Errors during assembling root CA.", e);
        }
    }

    public SSLEngine serverSslEngine(String peerHost, int peerPort) {
        return sslEngineSource.newSslEngine(peerHost, peerPort);
    }

    @Override
    public SSLEngine serverSslEngine() {
        return sslEngineSource.newSslEngine();
    }

    public SSLEngine clientSslEngineFor(HttpRequest httpRequest, SSLSession serverSslSession) {
        String serverHostAndPort = httpRequest.getUri();
        try {
            String serverName = serverHostAndPort.split(":")[0];
            SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();
            san.addDomainName(serverName);
            return sslEngineSource.createCertForHost(serverName, san);
        } catch (Exception e) {
            throw new FakeCertificateException(
                    "Creation dynamic certificate failed for "
                            + serverHostAndPort, e);
        }
    }
}
