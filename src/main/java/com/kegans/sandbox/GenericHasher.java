package com.kegans.sandbox;

import com.idealista.fpe.FormatPreservingEncryption;
import com.idealista.fpe.builder.FormatPreservingEncryptionBuilder;
import com.idealista.fpe.component.functions.prf.DefaultPseudoRandomFunction;
import com.idealista.fpe.config.Alphabet;
import com.idealista.fpe.config.GenericDomain;
import com.idealista.fpe.config.GenericTransformations;
import com.idealista.fpe.config.LengthRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

public class GenericHasher {

    private static Logger logger = LoggerFactory.getLogger(GenericHasher.class);

    private char[] alphabetChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private char[] numberChars = "0123456789".toCharArray(); // {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private String deliminators = " .,|";

    Alphabet alphabet;
    Alphabet numbers;

    // a key
    byte[] key = new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00

    };

    byte[] tweak = new byte[] {(byte) 0xEf5, (byte) 0x03, (byte) 0xF9};


    public GenericHasher() {
        super();
        this.createAlphabet();
        this.createNumberAlphabet();
    }

    public byte[] hash(String data, int outputLength) {

        logger.info("data to encode: =>{}<=", data);

        final byte[][] dataBytes = {data.getBytes()};
        final byte[][] encoded = new byte[1][1];

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

        StringTokenizer st = new StringTokenizer(data, deliminators, true);

        st.asIterator().forEachRemaining(word -> {

            // logger.info("=>{}<=", word);

            if (String.valueOf(word).length()>1) {

                if (isNumber((String) word)) {
                    try {
                        encoded[0] = hashNumber((String) word, ((String) word).length());
                        outputStream.write(encoded[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        encoded[0] = hashWord((String) word, ((String) word).length());
                        outputStream.write(encoded[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


                logger.info("{} => {}", word, new String(encoded[0]));

                if (st.hasMoreTokens()) {
                    try {
                        outputStream.write(st.nextToken().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {

                try {
                    outputStream.write(String.valueOf(word).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        return outputStream.toByteArray();

    }

    private byte[] hashNumber(String word, int maxLength) {
        FormatPreservingEncryption formatPreservingEncryption = FormatPreservingEncryptionBuilder
                .ff1Implementation()
                .withDomain(new GenericDomain(numbers, new GenericTransformations(numbers.availableCharacters()), new GenericTransformations(numbers.availableCharacters())))
                .withPseudoRandomFunction(new DefaultPseudoRandomFunction(key))
                .withLengthRange(new LengthRange(2, word.length()+1))
                .build();
        return formatPreservingEncryption.encrypt(word, tweak).getBytes();
    }


    private byte[] hashWord(String word, int maxLength) {
        FormatPreservingEncryption formatPreservingEncryption = FormatPreservingEncryptionBuilder
                .ff1Implementation()
                .withDomain(new GenericDomain(alphabet, new GenericTransformations(alphabet.availableCharacters()), new GenericTransformations(alphabet.availableCharacters())))
                .withPseudoRandomFunction(new DefaultPseudoRandomFunction(key))
                .withLengthRange(new LengthRange(2, word.length()+1))
                .build();
        return formatPreservingEncryption.encrypt(word, tweak).getBytes();
    }



    private boolean isNumber(String word) {
        try {
            long number = Long.valueOf(word);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public void createAlphabet() {
        alphabet = new Alphabet() {

            private char[] chars = alphabetChars;

            @Override
            public char[] availableCharacters() {
                return chars;
            }

            @Override
            public Integer radix() {
                return chars.length;
            }
        };
    }


    public void createNumberAlphabet() {
        numbers = new Alphabet() {

            private char[] chars = numberChars;

            @Override
            public char[] availableCharacters() {
                return chars;
            }

            @Override
            public Integer radix() {
                return chars.length;
            }
        };
    }

}
