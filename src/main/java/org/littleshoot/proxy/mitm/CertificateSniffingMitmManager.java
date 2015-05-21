package org.littleshoot.proxy.mitm;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.littleshoot.proxy.MitmManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MitmManager} that tries to find the common name and subject
 * alternative names from the upstream certificate to create a dynamic
 * certificate with it.
 * 
 * @deprecated There is a problem with long subject alternate name lists in the
 *             upstream certificate. See the comments.
 */
@Deprecated
public class CertificateSniffingMitmManager implements MitmManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(CertificateSniffingMitmManager.class);

    private BouncyCastleSslEngineSource sslEngineSource;

    public CertificateSniffingMitmManager(Authority authority)
            throws RootCertificateException {
        try {
            sslEngineSource = new BouncyCastleSslEngineSource(authority, true,
                    true);
        } catch (final Exception e) {
            throw new RootCertificateException(
                    "Errors during assembling root CA.", e);
        }
    }

    public SSLEngine serverSslEngine(InetSocketAddress remoteAddress) {
        return sslEngineSource.newSslEngine(remoteAddress.getHostName(),
                remoteAddress.getPort());
    }

    public SSLEngine clientSslEngineFor(SSLSession serverSslSession,
            String serverHostAndPort) {
        try {
            X509Certificate upstreamCert = getCertificateFromSession(serverSslSession);
            // TODO store the upstream cert by commonName to review it later
            String commonName = getCommonName(upstreamCert);

            // Two reasons to not use the common name and the alternative names
            // from upstream certificate from serverSslSession to create the
            // dynamic certificate:
            //
            // 1. It's not necessary. The host name is accepted by the browser.
            //
            // 2. Googles developer.chrome.com certificate (for example) lists
            // more subject alternate names in the browser, than returned by
            // getSubjectAlternativeNames(). Therefore the fake certificate was
            // rejected. A Bug?
            //
            String serverName = serverHostAndPort.split(":")[0];
            SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();

            // This is an ugly trick to add the host name to the truncated list.
            // I's obsolete and names could be duplicated in this way.
            //
            san.addAll(upstreamCert.getSubjectAlternativeNames());
            san.addDomainName(serverName);

            LOG.debug("Subject Alternative Names: {}", san);
            return sslEngineSource.createCertForHost(commonName, san);

        } catch (Exception e) {
            throw new FakeCertificateException(
                    "Creation dynamic certificate failed for "
                            + serverHostAndPort, e);
        }
    }

    private X509Certificate getCertificateFromSession(SSLSession sslSession)
            throws SSLPeerUnverifiedException {
        Certificate[] peerCerts = sslSession.getPeerCertificates();
        Certificate peerCert = peerCerts[0];
        // log.debug("Upstream Certificate: {}", peerCert);
        if (peerCert instanceof java.security.cert.X509Certificate) {
            return (java.security.cert.X509Certificate) peerCert;
        }
        throw new IllegalStateException(
                "Required java.security.cert.X509Certificate, found: "
                        + peerCert);
    }

    private String getCommonName(X509Certificate c) {
        LOG.debug("Subject DN principal name: {}", c.getSubjectDN().getName());
        for (String each : c.getSubjectDN().getName().split(",\\s*")) {
            if (each.startsWith("CN=")) {
                String result = each.substring(3);
                LOG.debug("Common Name: {}", result);
                return result;
            }
        }
        throw new IllegalStateException("Missed CN in Subject DN: "
                + c.getSubjectDN());
    }
}
