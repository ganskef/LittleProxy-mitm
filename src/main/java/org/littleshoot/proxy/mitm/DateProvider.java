package org.littleshoot.proxy.mitm;

import java.util.Date;
import java.util.concurrent.TimeUnit;


interface DateProvider {

    Date getDate();


    class Common {

        public static DateProvider constant(final Date date) {
            return new DateProvider() {
                @Override
                public Date getDate() {
                    return date;
                }
            };
        }

        /**
         * Provider, that produces {@link Date} computed by adding or subtracting given offset from current time as returned by {@link System#currentTimeMillis()}.
         * @param offset date offset in given time units. Offset could be positive if you need date in the future or negative if in the past.
         *               If offset is 0, provider will always return current time.
         * @param unit time unit for offset
         */
        public static DateProvider relativeToNow(final long offset, final TimeUnit unit) {
            return new DateProvider() {
                @Override
                public Date getDate() {
                    return new Date(System.currentTimeMillis() + unit.toMillis(offset));
                }
            };
        }

        /**
         * Provider, that produces {@link Date} in the past by given offset relative to current time as returned by {@link System#currentTimeMillis()}.
         * @param offset date offset in given time units. Only non-negative offsets allowed.
         * @param unit time unit for offset
         * @throws IllegalArgumentException if offset is negative
         */
        public static DateProvider beforeNow(final long offset, final TimeUnit unit) {
            if (offset < 0) {
                throw new IllegalArgumentException("Negative offset not allowed");
            }
            return relativeToNow(-offset, unit);
        }

        /**
         * Provider, that produces {@link Date} in the future by given offset relative to current time as returned by {@link System#currentTimeMillis()}.
         * @param offset date offset in given time units. Only non-negative offsets allowed.
         * @param unit time unit for offset
         * @throws IllegalArgumentException if offset is negative
         */
        public static DateProvider afterNow(final long offset, final TimeUnit unit) {
            if (offset < 0) {
                throw new IllegalArgumentException("Negative offset not allowed");
            }
            return relativeToNow(offset, unit);
        }

    }

}