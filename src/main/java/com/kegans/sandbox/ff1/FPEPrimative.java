package com.kegans.sandbox.ff1;

import com.idealista.fpe.config.Alphabet;

public interface FPEPrimative {

    String value = null;

    Alphabet getAlphabet();
    String getDeliminators();
    byte[] getTweak(String word);
    byte[] getKey();
    String getValue();
    FPEPrimative setValue(String value);
}
