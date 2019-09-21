package org.littleshoot.proxy.mitm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectAlternativeNameHolder {

    private static final Logger log = LoggerFactory.getLogger(SubjectAlternativeNameHolder.class);

    /**
     * @see org.bouncycastle.asn1.x509.GeneralName
     * @see <a href="https://tools.ietf.org/html/rfc5280#section-4.2.1.6">RFC 5280, § 4.2.1.6. Subject Alternative Name</a>
     */
    private static final Pattern TAGS_PATTERN = Pattern.compile("[012345678]");

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

    public void addAll(Collection<List<?>> subjectAlternativeNames) {
        if (subjectAlternativeNames != null) {
            for (List<?> each : subjectAlternativeNames) {
                if (isValidNameEntry(each)) {
                    int tag = Integer.valueOf(String.valueOf(each.get(0)));
                    String name = String.valueOf(each.get(1));
                    sans.add(new GeneralName(tag, name));
                } else {
                    log.warn("Invalid name entry ignored: {}", each);
                }
                
            }
        }
    }

    private boolean isValidNameEntry(List<?> nameEntry) {
        if (nameEntry == null || nameEntry.size() != 2) {
            return false;
        }
        String tag = String.valueOf(nameEntry.get(0));
        return TAGS_PATTERN.matcher(tag).matches();
    }
}
