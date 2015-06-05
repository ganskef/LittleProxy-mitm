package org.littleshoot.proxy.mitm;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.littleshoot.proxy.MitmManager;

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

    public SSLEngine serverSslEngine(String serverHostAndPort) {
        return sslEngineSource.newSslEngine(serverHostAndPort);
    }

    public SSLEngine clientSslEngineFor(SSLSession serverSslSession,
            String serverHostAndPort) {
        try {
            String serverName = serverHostAndPort.split(":")[0];
            SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();
            return sslEngineSource.createCertForHost(serverName, san);
        } catch (Exception e) {
            throw new FakeCertificateException(
                    "Creation dynamic certificate failed for "
                            + serverHostAndPort, e);
        }
    }
}
