package com.github.shampoofactory.asynctest;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Hash {

    static byte[] get(ByteBuffer src, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(src);
        return md.digest();
    }

    private Hash() {
    }
}
