package com.github.shampoofactory.asynctest;

import java.util.ArrayList;
import java.util.List;

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

        private int retryCount = 3;
        private int retryDelay = 500;   // milliseconds
        private int maxConcurrent = 4;
        private int chunkLen = 65536;
        private int minChunks = 4;
        private int maxChunks = 64;

        public Builder retryCount(int retryCount) {
            if (retryCount < 1) {
                throw new IllegalArgumentException();
            }
            this.retryCount = retryCount;
            return this;
        }

        public Builder retryDelay(int retryDelay) {
            if (retryDelay < 1) {
                throw new IllegalArgumentException();
            }
            this.retryDelay = retryDelay;
            return this;
        }

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

        public Builder setMaxChunks(int maxChunks) {
            if (maxChunks < 1) {
                throw new IllegalArgumentException();
            }
            this.maxChunks = maxChunks;
            return this;
        }

        public Param build() {
            if (maxChunks < minChunks) {
                throw new IllegalArgumentException();
            }
            return new Param(retryCount, retryDelay, maxConcurrent,
                    chunkLen, minChunks, maxChunks);
        }
    }

    private final int retryCount;
    private final int retryDelay;   // milliseconds
    private final int maxConcurrent;
    private final int chunkLen;
    private final int minChunks;
    private final int maxChunks;

    private Param(int retryCount, int retryDelay, int maxConcurrent,
            int chunkLen, int minChunks, int maxChunks) {
        this.retryCount = retryCount;
        this.retryDelay = retryDelay;
        this.maxConcurrent = maxConcurrent;
        this.chunkLen = chunkLen;
        this.minChunks = minChunks;
        this.maxChunks = maxChunks;
    }

    public List<Split> split(long len) {
        long nChunks = len / chunkLen;
        long trim = len - nChunks * chunkLen;
        long div = nChunks / maxConcurrent;
        long mod = nChunks % maxConcurrent;
        ArrayList<Split> list = new ArrayList<>(maxConcurrent);
        long pos = 0;
        for (int i = 0; i < maxConcurrent; i++) {
            long partLen = div * chunkLen;
            if (mod != 0) {
                partLen += chunkLen;
                mod -= 1;
            }
            if (i == maxConcurrent - 1) {
                partLen += trim;
            }
            if (partLen == 0) {
                continue;
            }
            Split split = new Split(i, pos, pos + partLen);
            list.add(split);
            pos += partLen;
        }
        return list;

    }

    public int retryCount() {
        return retryCount;
    }

    public int retryDelay() {
        return retryDelay;
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

    public int maxChunks() {
        return maxChunks;
    }

    public long minLen() {
        return (long) minChunks * (long) chunkLen;
    }

    public long maxLen() {
        return (long) maxChunks * (long) chunkLen;
    }

    @Override
    public String toString() {
        return "Param{"
                + "retryCount=" + retryCount
                + ", retryDelay=" + retryDelay
                + ", maxConcurrent=" + maxConcurrent
                + ", chunkLen=" + chunkLen
                + ", minChunks=" + minChunks
                + ", maxChunks=" + maxChunks
                + '}';
    }
}
