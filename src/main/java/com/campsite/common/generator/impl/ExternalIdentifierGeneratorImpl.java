package com.campsite.common.generator.impl;

import com.campsite.common.generator.ExternalIdentifierGenerator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static java.lang.String.valueOf;

public class ExternalIdentifierGeneratorImpl implements ExternalIdentifierGenerator {
    private final String PREFIX = "RSV";
    private final SecureRandom sr;
    public ExternalIdentifierGeneratorImpl() {
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
            sr.generateSeed(1000);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String getNext() {
        return PREFIX.concat(valueOf(sr.nextLong()));
    }
}
