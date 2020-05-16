package org.littleshoot.proxy.mitm;

import java.util.concurrent.TimeUnit;

public class CacheConfig {

    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final long UNSET_SIZE = -1;
    static final long UNSET_DURATION = -1;

    private final int initialCapacity;
    private final long maxSize;
    private final long expireAfterCreate;
    private final long expireAfterUse;

    public CacheConfig(int initialCapacity, long maxSize, long expireAfterCreate, long expireAfterUse) {
        this.initialCapacity = initialCapacity;
        this.maxSize = maxSize;
        this.expireAfterCreate = expireAfterCreate;
        this.expireAfterUse = expireAfterUse;
    }

    public static Builder newConfig() {
        return new Builder();
    }

    public static CacheConfig defaultConfig() {
        return new Builder().build();
    }

    int initialCapacity() {
        return this.initialCapacity;
    }

    long maximumSize() {
        return this.maxSize;
    }

    long expireAfterCreate() {
        return this.expireAfterCreate;
    }

    long expireAfterUse() {
        return this.expireAfterUse;
    }


    public static class Builder {

        private int initialCapacity = DEFAULT_INITIAL_CAPACITY;
        private long maxSize = UNSET_SIZE;
        private long expireAfterCreate = UNSET_DURATION;
        private long expireAfterUse = UNSET_DURATION;

        private Builder() {}

        public CacheConfig build() {
            return new CacheConfig(initialCapacity, maxSize, expireAfterCreate, expireAfterUse);
        }

        public Builder initialCapacity(int capacity) {
            if (capacity < 0) {
                throw new IllegalArgumentException("Negative initial capacity");
            }
            this.initialCapacity = capacity;
            return this;
        }

        public Builder maximumSize(int size) {
            if (size < 0) {
                throw new IllegalArgumentException("Negative maximum size");
            }
            this.maxSize = size;
            return this;
        }

        public Builder expireAfterCreate(long duration, TimeUnit unit) {
            this.expireAfterCreate = unit.toMillis(duration);
            return this;
        }

        public Builder expireAfterUse(long duration, TimeUnit unit) {
            this.expireAfterUse = unit.toMillis(duration);
            return this;
        }

    }

}
