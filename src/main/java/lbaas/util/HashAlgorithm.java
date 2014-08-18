/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package lbaas.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;


public class HashAlgorithm {

    public static enum HashType {
        MD5,            // It's not so bad, but is a little slow.
//      MURMUR3_128,    // Slow
        MURMUR3_32,     // Fast and reliable, but not so good for small keys
//      GOOD_FAST_32,   // Super Fast, but with excessive collisions. Why this was released?
//      ADLER_32,       // Unreliable
//      CRC_32,         // Unreliable
//      SHA1,           // Slow and Unreliable
        SHA256,         // Reliable. Its a little slow, but not quite.
//      SHA512,         // Reliable, but very slow
        SIP24           // Fast and reliable. The best for small keys
    }

    private static final Map<String, HashType> HashTypeMap = new HashMap<>();
    static {
        for (HashType hash : EnumSet.allOf(HashType.class)) {
            HashTypeMap.put(hash.toString(), hash);
        }
    }

    private final HashType hashType;

    @Override
    public String toString() {
        return String.format("%s - hashType:%s", HashAlgorithm.class.getName(), hashType);
    }

    public HashAlgorithm(HashType hashType) {
        this.hashType = hashType;
    }

    public HashAlgorithm(String hashTypeStr) {
        this.hashType = HashTypeMap.containsKey(hashTypeStr) ? HashType.valueOf(hashTypeStr) : HashType.SIP24;
    }

    public int hash(Object key) {
        HashCode hashCode;
        HashFunction hashAlgorithm;
        switch (hashType) {
            case MD5:
                hashAlgorithm = Hashing.md5();
                break;
            case MURMUR3_32:
                hashAlgorithm = Hashing.murmur3_32();
                break;
            case SHA256:
                hashAlgorithm = Hashing.sha256();
                break;
            case SIP24:
                hashAlgorithm = Hashing.sipHash24();
                break;
            default:
                hashAlgorithm = Hashing.sipHash24();
                break;
        }
        if (key instanceof String) {
            hashCode = hashAlgorithm.newHasher().putString((String)key,Charsets.UTF_8).hash();
        } else if (key instanceof Long) {
            hashCode = hashAlgorithm.newHasher().putLong((Long)key).hash();
        } else {
            hashCode = hashAlgorithm.newHasher().hash();
        }
        return hashCode.asInt();
    }

}

