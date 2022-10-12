package com.campsite.common;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.valueOf;

public class ExternalIdentifierGeneratorImpl implements ExternalIdentifierGenerator {
    private final String PREFIX = "RSV";
    private final SecureRandom sr;

    public ExternalIdentifierGeneratorImpl() {
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNext() {
        return PREFIX.concat(valueOf(sr.nextLong()));
    }
}
