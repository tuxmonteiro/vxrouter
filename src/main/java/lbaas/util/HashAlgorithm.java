/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.util;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;


public class HashAlgorithm {

    public static enum HashType {
        MD5,
        MURMUR3_128,
    }

    private final HashType hashType;

    public HashAlgorithm(HashType hashType) {
        this.hashType = hashType;
    }

    public int hash(String key) {
        HashCode hashCode;
        HashFunction hashAlgorithm;
        switch (hashType) {
            case MD5:
                hashAlgorithm = Hashing.md5();
                break;
            case MURMUR3_128:
                hashAlgorithm = Hashing.murmur3_128();
                break;
            default:
                hashAlgorithm = Hashing.murmur3_128();
                break;
        }
        hashCode = hashAlgorithm.newHasher().putString(key,Charsets.UTF_8).hash();

        return hashCode.asInt();
    }

}

