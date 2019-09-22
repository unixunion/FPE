package com.kegans.sandbox;

import com.idealista.fpe.FormatPreservingEncryption;
import com.idealista.fpe.builder.FormatPreservingEncryptionBuilder;
import com.idealista.fpe.component.functions.prf.DefaultPseudoRandomFunction;
import com.idealista.fpe.config.Alphabet;
import com.idealista.fpe.config.GenericDomain;
import com.idealista.fpe.config.GenericTransformations;
import com.idealista.fpe.config.LengthRange;
import com.kegans.sandbox.ff1.FPEPrimative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

@Component
public class GenericHasher {

    private static Logger logger = LoggerFactory.getLogger(GenericHasher.class);

    private String alphabetString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private char[] alphabetChars;
    private char[] numberChars = "0123456789".toCharArray(); // {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    // the token separators, will be copied in-place into output to maintain format
    private String deliminators = "  .,|-";

    // alphabets
    Alphabet alphabet;
    Alphabet numbers;

    // digest
    MessageDigest md = MessageDigest.getInstance("SHA-256");

    // a key for if not perItem key mode
    byte[] key = new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00

    };

    // tweak for not if perItem tweak mode
    byte[] tweak = new byte[] {(byte) 0xEf5, (byte) 0x03, (byte) 0xF9};

    // use perItem or pre-configured keys
    boolean perItemKey = true;
    boolean perItemTweak = true;
    boolean copyDeliminators = true;


    /**
     * @throws NoSuchAlgorithmException
     */
    public GenericHasher() throws NoSuchAlgorithmException {
        super();
        initAlphabetChars();
        this.createAlphabet();
        this.createNumberAlphabet();
    }


    // tweak is just the data itself for now
    // this should return something better
    public byte[] getTweak(String data) {
        if (perItemTweak) {
            return data.getBytes();
        } else {
            return tweak;
        }
    }


    public byte[] getKey(String data) {
        if (perItemKey) {
            return md.digest(data.getBytes());
        } else  {
            return key;
        }
    }



    @Async("threadPoolTaskExecutor")
    public CompletableFuture<byte[]> ff1(FPEPrimative data) {

        StringTokenizer st = new StringTokenizer(data.getValue(), data.getDeliminators(), true);

        final byte[][] encoded = new byte[1][1];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

        byte[] mytweak = data.getTweak(data.getValue());
        byte[] mykey = md.digest(data.getKey());

        FormatPreservingEncryption formatPreservingEncryption = FormatPreservingEncryptionBuilder
                .ff1Implementation()
                .withDomain(new GenericDomain(data.getAlphabet(), new GenericTransformations(data.getAlphabet().availableCharacters()), new GenericTransformations(data.getAlphabet().availableCharacters())))
                .withPseudoRandomFunction(new DefaultPseudoRandomFunction(mykey))
                .withLengthRange(new LengthRange(2, data.getValue().length()))
                .build();

        st.asIterator().forEachRemaining(word -> {

            if (String.valueOf(word).length() > 1) {

                try {
                    outputStream.write(formatPreservingEncryption.encrypt(String.valueOf(word), data.getTweak(String.valueOf(word))).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // copy deliminators in-place to maintain format
                if (copyDeliminators && st.hasMoreTokens()) {
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

        return CompletableFuture.completedFuture(outputStream.toByteArray());
    }


    /**
     * The main hashing function,
     *
     * @param data string of data
     * @return
     */
    @Async("threadPoolTaskExecutor")
    public CompletableFuture<byte[]> encrypt(String data) {

        final byte[][] encoded = new byte[1][1];

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

        StringTokenizer st = new StringTokenizer(data, deliminators, true);

        st.asIterator().forEachRemaining(word -> {

//            logger.info(String.valueOf(word));

            if (String.valueOf(word).length() > 1) {

                if (isNumber((String) word)) {
                    try {
                        encoded[0] = encryptNumber((String) word);
                        outputStream.write(encoded[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        encoded[0] = encryptWord((String) word);
                        outputStream.write(encoded[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (copyDeliminators && st.hasMoreTokens()) {
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

        return CompletableFuture.completedFuture(outputStream.toByteArray());

    }

    /**
     * encrypt a string containing a number, uses the MessageDigest of the string
     * as the key, and the string itself as the tweak.
     *
     * @param word the number to encrypt,  in a string
     * @return
     */
    private byte[] encryptNumber(String word) {

        byte[] mytweak = getTweak(word); // word.getBytes();
        byte[] mykey = getKey(word); //md.digest(word.getBytes());

        FormatPreservingEncryption formatPreservingEncryption = FormatPreservingEncryptionBuilder
                .ff1Implementation()
                .withDomain(new GenericDomain(numbers, new GenericTransformations(numbers.availableCharacters()), new GenericTransformations(numbers.availableCharacters())))
                .withPseudoRandomFunction(new DefaultPseudoRandomFunction(mykey))
                .withLengthRange(new LengthRange(2, word.length()+1))
                .build();
        return formatPreservingEncryption.encrypt(word, mytweak).getBytes();
    }


    /**
     * encrypt a string, uses the MessageDigest of the string
     * as the key, and the string itself as the tweak.
     *
     * @param word string to encrypt
     * @return
     */
    private byte[] encryptWord(String word) {

//        logger.info(word);

        byte[] mytweak = getTweak(word); //word.getBytes();
        byte[] mykey = getKey(word); //md.digest(word.getBytes());

        FormatPreservingEncryption formatPreservingEncryption = FormatPreservingEncryptionBuilder
                .ff1Implementation()
                .withDomain(new GenericDomain(alphabet, new GenericTransformations(alphabet.availableCharacters()), new GenericTransformations(alphabet.availableCharacters())))
                .withPseudoRandomFunction(new DefaultPseudoRandomFunction(mykey))
                .withLengthRange(new LengthRange(2, word.length()+1))
                .build();
        return formatPreservingEncryption.encrypt(word, mytweak).getBytes();
    }


    /**
     * Determine if a string can be converted to a number.
     *
     * @param word
     * @return
     */
    private boolean isNumber(String word) {
        try {
            Long.valueOf(word);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Creates the alphabet
     */
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


    /**
     * Creates the alphabet for numbers
     */
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

    // alphabet initializer
    private void initAlphabetChars() {

//        Set<Character> latinChars = findCharactersInUnicodeBlock(Character.UnicodeBlock.BASIC_LATIN);
//
//        findCharactersInUnicodeBlock(Character.UnicodeBlock.BASIC_LATIN).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });

//        findCharactersInUnicodeBlock(Character.UnicodeBlock.GREEK).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });

//        findCharactersInUnicodeBlock(Character.UnicodeBlock.LATIN_EXTENDED_A).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });
//        findCharactersInUnicodeBlock(Character.UnicodeBlock.LATIN_EXTENDED_B).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });
//        findCharactersInUnicodeBlock(Character.UnicodeBlock.LATIN_EXTENDED_C).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });
//        findCharactersInUnicodeBlock(Character.UnicodeBlock.LATIN_EXTENDED_D).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });
//        findCharactersInUnicodeBlock(Character.UnicodeBlock.LATIN_EXTENDED_E).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });
//        findCharactersInUnicodeBlock(Character.UnicodeBlock.LATIN_1_SUPPLEMENT).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });
//        findCharactersInUnicodeBlock(Character.UnicodeBlock.CYRILLIC).iterator().forEachRemaining(c -> {
//            latinChars.add(c);
//        });
//
//        latinChars.iterator().forEachRemaining(c -> {
//            if (Pattern.matches("\\p{IsLatin}", String.valueOf(c))) {
//                alphabetString = alphabetString + (char)c;
//            }
//        });

        alphabetString = alphabetString + "-'";

        alphabetChars = alphabetString.toCharArray();
        logger.info("alphabet: {}", alphabetString);

    }

    // get characters from a unicode block
    public static Set<Character> findCharactersInUnicodeBlock(final Character.UnicodeBlock block) {
        final Set<Character> chars = new HashSet<Character>();
        for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (block == Character.UnicodeBlock.of(codePoint)) {
                chars.add((char) codePoint);
            }
        }
        return chars;
    }


    public String getDelminators() {
        return this.deliminators;
    }
}
