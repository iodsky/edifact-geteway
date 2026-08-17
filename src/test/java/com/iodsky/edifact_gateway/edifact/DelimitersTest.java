package com.iodsky.edifact_gateway.edifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelimitersTest {

    public static final String INVALID_UNA = "INVALID_UNA";

    @Test
    void defaults_returnsStandardUnEdifactDelimiters() {

        var delimiters = Delimiters.defaults();
        assertEquals(':', delimiters.componentSeparator());
        assertEquals('+', delimiters.dataElementSeparator());
        assertEquals('.', delimiters.decimalMark());
        assertEquals('?', delimiters.releaseCharacter());
        assertEquals('\'', delimiters.segmentTerminator());
    }

    @Test
    void fromUna_parsesAllFiveDelimitersAndSkipsReservedCharacter() {

        /*
         * U N A | * , ~   !
         * 0 1 2 3 4 5 6 7 8
         */
        var delimiters = Delimiters.fromUna("UNA|*,~ !");
        assertEquals('|', delimiters.componentSeparator());
        assertEquals('*', delimiters.dataElementSeparator());
        assertEquals(',', delimiters.decimalMark());
        assertEquals('~', delimiters.releaseCharacter());
        assertEquals('!', delimiters.segmentTerminator());
    }

    @Test
    void fromUna_throwsParseException_whenInputIsNull() {
        ParseException ex = assertThrows(ParseException.class, () -> Delimiters.fromUna(null));
        assertEquals(INVALID_UNA, ex.getCode());
    }

    @Test
    void fromUna_throwsParseException_whenLengthIsNotNine() {
        ParseException ex = assertThrows(ParseException.class, () -> Delimiters.fromUna("UNA|*,~! @"));
        assertEquals(INVALID_UNA, ex.getCode());
    }

    @Test
    void fromUna_throwsParseException_whenPrefixIsNotUna() {
        ParseException ex = assertThrows(ParseException.class, () -> Delimiters.fromUna("UNB|*,~ !"));
        assertEquals(INVALID_UNA, ex.getCode());
    }

}