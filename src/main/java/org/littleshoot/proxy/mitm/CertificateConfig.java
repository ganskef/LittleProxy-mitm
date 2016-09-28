package org.littleshoot.proxy.mitm;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Dynamically generated certificate configuration
 */
public class CertificateConfig {

    private static final int DEFAULT_ROOT_KEY_SIZE = 2048;

    private static final int DEFAULT_SERVER_KEY_SIZE = 1024;

    /**
     * Current time minus 1 year, just in case software clock goes back due to
     * time synchronization.
     *
     * Time gets recomputed each time certificate is generating,
     * that prevents producing too long living certificates, that my cause issues in some cases
     * (if not nullified by improper certificate caching strategy)
     */
    private static final DateProvider DEFAULT_NOT_BEFORE = DateProvider.Common.beforeNow(365, TimeUnit.DAYS);

    /**
     * The maximum possible value in X.509 specification: 9999-12-31 23:59:59,
     * new Date(253402300799000L), but Apple iOS 8 fails with a certificate
     * expiration date grater than Mon, 24 Jan 6084 02:07:59 GMT (issue #6).
     *
     * Time gets recomputed each time certificate is generating,
     * that prevents producing expired certificates (if not nullified by improper certificate caching strategy).
     */
    private static final DateProvider DEFAULT_NOT_AFTER = DateProvider.Common.afterNow(365, TimeUnit.DAYS);

    private Authority authority;
    private int rootKeySize;
    private int serverKeySize;
    private DateProvider notBefore;
    private DateProvider notAfter;

    private CertificateConfig(Authority authority,
            int rootKeySize, int serverKeySize,
            DateProvider notBefore, DateProvider notAfter) {
        this.authority = authority;
        this.rootKeySize = rootKeySize;
        this.serverKeySize = serverKeySize;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
    }

    public static Builder newConfig(Authority authority) {
        return new Builder(authority);
    }

    public Authority authority() {
        return authority;
    }

    public int rootKeySize() {
        return rootKeySize;
    }

    public int serverKeySize() {
        return serverKeySize;
    }

    public Date notBefore() {
        return notBefore.getDate();
    }

    public Date notAfter() {
        return notAfter.getDate();
    }


    public static class Builder {

        private final Authority authority;
        private int rootKeySize = DEFAULT_ROOT_KEY_SIZE;
        private int serverKeySize = DEFAULT_SERVER_KEY_SIZE;
        private DateProvider notBefore = DEFAULT_NOT_BEFORE;
        private DateProvider notAfter = DEFAULT_NOT_AFTER;

        public Builder(Authority authority) {
            if (authority == null) {
                throw new NullPointerException("authority is null");
            }
            this.authority = authority;
        }

        public CertificateConfig build() {
            return new CertificateConfig(authority, rootKeySize, serverKeySize, notBefore, notAfter);
        }

        public Builder rootKeySize(int size) {
            this.rootKeySize = size;
            return this;
        }

        public Builder serverKeySize(int size) {
            this.serverKeySize = size;
            return this;
        }

        public Builder validNotBefore(Date notBefore) {
            this.notBefore = DateProvider.Common.constant(notBefore);
            return this;
        }

        public Builder validNotAfter(Date notAfter) {
            this.notAfter = DateProvider.Common.constant(notAfter);
            return this;
        }

        /**
         * How many time certificate is valid after generation
         * @param validAfter time interval length in given units
         * @param unit duration unit
         * @return
         */
        public Builder validAfter(long validAfter, TimeUnit unit) {
            this.notBefore = DateProvider.Common.beforeNow(validAfter, unit);
            return this;
        }

        /**
         * How many time certificate was valid before generation
         * @param validBefore duration in given units
         * @param unit duration unit
         * @return
         */
        public Builder validBefore(long validBefore, TimeUnit unit) {
            this.notAfter = DateProvider.Common.afterNow(validBefore, unit);
            return this;
        }

    }

}