package org.littleshoot.proxy.mitm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;

public class SubjectAlternativeNameHolder {

    private final List<ASN1Encodable> sans = new ArrayList<ASN1Encodable>();

    public void addIpAddress(String ipAddress) {
        sans.add(new GeneralName(GeneralName.iPAddress, ipAddress));
    }

    public void addDomainName(String subjectAlternativeName) {
        sans.add(new GeneralName(GeneralName.dNSName, subjectAlternativeName));
    }

    public void fillInto(X509v3CertificateBuilder certGen)
            throws CertIOException {
        if (!sans.isEmpty()) {
            ASN1Encodable[] encodables = sans.toArray(new ASN1Encodable[sans
                    .size()]);
            certGen.addExtension(Extension.subjectAlternativeName, false,
                    new DERSequence(encodables));
        }
    }

    @Deprecated
    public void addAll(Collection<List<?>> subjectAlternativeNames) {
        // FIXME or remove it
        throw new UnsupportedOperationException();
    }

}
