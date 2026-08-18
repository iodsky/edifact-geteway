package com.iodsky.edifact_gateway.edifact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private Parser parser;

    @BeforeEach
    void setup() {
        parser = new Parser(Delimiters.defaults());
    }

    @Test
    void parseSegments_splitsMessageOnTerminator() {
        String message = """
        UNB+UNOA:2+BUYER123+SELLER456+240115:0930+REF001'
        UNH+1+ORDERS:D:96A:UN'
        BGM+220+PO-2024-001+9'
        DTM+137:20240115:102'
        DTM+2:20240130:102'
        NAD+BY+5412345000016::9++ACME Corporation+123 Business Street+New York+NY+10001+US'
        NAD+SU+4012345000094::9++Global Supplies Ltd+456 Supplier Road+Chicago+IL+60601+US'
        CUX+2:USD:4'
        LIN+1++9780123456789:EN'
        PIA+1+WIDGET-A:SA'
        IMD+F++:::Premium Widget Model A'
        QTY+21:100'
        PRI+AAA:25.99'
        LIN+2++9780987654321:EN'
        PIA+1+GADGET-B:SA'
        IMD+F++:::Standard Gadget Model B'
        QTY+21:50'
        PRI+AAA:49.99'
        UNS+S'
        CNT+2:2'
        UNT+18+1'
        UNZ+1+REF001'
        """;
        List<String> segments = parser.parseSegments(message);

        assertEquals(22, segments.size());
        assertEquals("UNB+UNOA:2+BUYER123+SELLER456+240115:0930+REF001", segments.getFirst());
        assertEquals("UNZ+1+REF001", segments.getLast());
    }

    @Test
    void parseSegments_stripsCrLfLineEndings() {
        String segment = "UNB+1'\r\nUNH+2'\r\n";
        List<String> segments = parser.parseSegments(segment);

        assertEquals(2, segments.size());
        assertEquals(List.of("UNB+1", "UNH+2"), segments);
        assertTrue(segments.stream().noneMatch(s -> s.contains("\r")));
        assertTrue(segments.stream().noneMatch(s -> s.contains("\n")));
    }

    @Test
    void parseSegments_keepsEscapedTerminator() {
        String segment = "NAD+BY+O?'Reilly'";
        List<String> segments = parser.parseSegments(segment);

        assertEquals(List.of("NAD+BY+O?'Reilly"), segments);
    }

    @Test
    void parseSegments_keepsEscapedReleaseCharacter() {
        String segment = "PRI+AAA:??10'";
        List<String> segments = parser.parseSegments(segment);

        assertEquals(List.of("PRI+AAA:??10"), segments);
    }

    @Test
    void parseSegments_treatsEvenRunOfReleaseCharsAsRealTerminator() {
        assertEquals(List.of("??"), parser.parseSegments("??'"));
    }

    @Test
    void parseSegments_throwsParseException_whenMessageIsEmpty() {
        ParseException ex = assertThrows(ParseException.class, () -> parser.parseSegments(""));
        assertEquals("EMPTY_INPUT", ex.getCode());
    }

    @Test
    void parseSegments_throwsParseException_whenMessageIsNull() {
        ParseException ex = assertThrows(ParseException.class, () -> parser.parseSegments(null));
        assertEquals("EMPTY_INPUT", ex.getCode());
    }


}