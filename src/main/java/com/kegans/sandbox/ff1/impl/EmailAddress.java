package com.kegans.sandbox.ff1.impl;

import com.idealista.fpe.config.Alphabet;
import com.kegans.sandbox.ff1.FPEPrimative;

/**
 * Example POC of a specific format data
 */
public class EmailAddress implements FPEPrimative {

    // a place to hold the value
    String value;

    @Override
    public Alphabet getAlphabet() {

        return new Alphabet() {

            char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-".toCharArray();

            @Override
            public char[] availableCharacters() {
                return alphabet;
            }

            @Override
            public Integer radix() {
                return alphabet.length;
            }
        };

    }

    @Override
    public String getDeliminators() {
        return "@.";
    }

    @Override
    public byte[] getTweak(String word) {
        return word.getBytes();
    }

    @Override
    public byte[] getKey() {
        return getValue().getBytes();
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public EmailAddress setValue(String value) {
        this.value = value;
        return this;
    }

}
