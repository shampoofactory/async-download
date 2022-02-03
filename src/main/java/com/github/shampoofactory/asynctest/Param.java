package com.github.shampoofactory.asynctest;

/**
 * Immutable parameters.
 * 
 * @author vin
 */
public final class Param {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int maxConcurrent = 4;
        private int chunkLen = 65536;
        private int minChunks = 4;

        public Builder setMaxConcurrent(int maxConcurrent) {
            if (maxConcurrent < 1) {
                throw new IllegalArgumentException();
            }
            this.maxConcurrent = maxConcurrent;
            return this;
        }

        public Builder setChunkLen(int chunkLen) {
            if (chunkLen < 0x1000) {
                throw new IllegalArgumentException();
            }
            this.chunkLen = chunkLen;
            return this;
        }

        public Builder setMinChunks(int minChunks) {
            if (minChunks < 1) {
                throw new IllegalArgumentException();
            }
            this.minChunks = minChunks;
            return this;
        }

        public Param build() {
            return new Param(maxConcurrent, chunkLen, minChunks);
        }
    }

    private final int maxConcurrent;
    private final int chunkLen;
    private final int minChunks;

    private Param(int maxConcurrent, int chunkLen, int minChunks) {
        this.maxConcurrent = maxConcurrent;
        this.chunkLen = chunkLen;
        this.minChunks = minChunks;
    }

    public int maxConcurrent() {
        return maxConcurrent;
    }

    public int chunkLen() {
        return chunkLen;
    }

    public int minChunks() {
        return minChunks;

    }

    public long minLen() {
        return (long) minChunks * (long) chunkLen;
    }

    @Override
    public String toString() {
        return "Param{"
                + "maxConcurrent=" + maxConcurrent
                + ", chunkLen=" + chunkLen
                + ", minChunks=" + minChunks
                + '}';
    }
}
