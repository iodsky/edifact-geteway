package com.iodsky.edifact_gateway.edifact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private static final String MESSAGE = """
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

    private Parser parser;

    @BeforeEach
    void setup() {
        parser = new Parser(Delimiters.defaults());
    }

    @Nested
    class TokenizeTests {

        @Test
        void tokenize_splitsMessageOnTerminator() {
            List<String> segments = parser.tokenize(MESSAGE);

            assertEquals(22, segments.size());
            assertEquals("UNB+UNOA:2+BUYER123+SELLER456+240115:0930+REF001", segments.getFirst());
            assertEquals("UNZ+1+REF001", segments.getLast());
        }

        @Test
        void tokenize_stripsCrLfLineEndings() {
            String segment = "UNB+1'\r\nUNH+2'\r\n";
            List<String> segments = parser.tokenize(segment);

            assertEquals(2, segments.size());
            assertEquals(List.of("UNB+1", "UNH+2"), segments);
            assertTrue(segments.stream().noneMatch(s -> s.contains("\r")));
            assertTrue(segments.stream().noneMatch(s -> s.contains("\n")));
        }

        @Test
        void tokenize_keepsEscapedTerminator() {
            String segment = "NAD+BY+O?'Reilly'";
            List<String> segments = parser.tokenize(segment);

            assertEquals(List.of("NAD+BY+O?'Reilly"), segments);
        }

        @Test
        void tokenize_keepsEscapedReleaseCharacter() {
            String segment = "PRI+AAA:??10'";
            List<String> segments = parser.tokenize(segment);

            assertEquals(List.of("PRI+AAA:??10"), segments);
        }

        @Test
        void tokenize_treatsEvenRunOfReleaseCharsAsRealTerminator() {
            assertEquals(List.of("??"), parser.tokenize("??'"));
        }

    }

    @Nested
    class ParseSegmentsTests {

        @Test
        void parseSegments_returnsAllSegments() {
            List<String> rawSegments = parser.tokenize(MESSAGE);
            List<Segment> segments = parser.parseSegments(rawSegments);

            assertEquals(rawSegments.size(), segments.size());
        }

        @Test
        void parseSegments_parsesFirstSegment() {
            List<String> rawSegments = parser.tokenize(MESSAGE);
            List<Segment> segments = parser.parseSegments(rawSegments);

            assertEquals("UNB", segments.getFirst().tag());
        }

        @Test
        void parseSegments_parsesLastSegment() {
            List<String> rawSegments = parser.tokenize(MESSAGE);
            List<Segment> segments = parser.parseSegments(rawSegments);

            assertEquals("UNZ", segments.getLast().tag());
        }

    }

    @Nested
    class ParseSegmentTests {

        @Test
        void parseSegment_returnsTagAndSimpleElements() {
            Segment segment = parser.parseSegment("UNT+18+1");

            assertEquals("UNT", segment.tag());
            assertEquals(2, segment.elements().size());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().getFirst());
            assertEquals("18", simpleDataElement.value());
            SimpleDataElement simpleDataElement1 = assertInstanceOf(SimpleDataElement.class, segment.elements().get(1));
            assertEquals("1", simpleDataElement1.value());
        }

        @Test
        void parseSegment_splitsCompositeIntoComponents() {
            Segment segment = parser.parseSegment("UNB+UNOA:2+BUYER123");

            assertEquals("UNB", segment.tag());
            assertEquals(2, segment.elements().size());

            CompositeDataElement comp = assertInstanceOf(CompositeDataElement.class, segment.elements().getFirst());
            assertEquals(List.of("UNOA", "2"), comp.components());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().get(1));
            assertEquals("BUYER123", simpleDataElement.value());
        }

        @Test
        void parseSegment_splitsCompositeIntoAllComponents() {
            Segment segment = parser.parseSegment("DTM+137:20240115:102");

            assertEquals("DTM", segment.tag());
            assertEquals(1, segment.elements().size());

            CompositeDataElement compositeDataElement = assertInstanceOf(CompositeDataElement.class, segment.elements().getFirst());
            assertEquals(List.of("137", "20240115", "102"), compositeDataElement.components());
        }

        @Test
        void parseSegment_returnsTagWithNoElements() {
            Segment segment = parser.parseSegment("ABC");

            assertEquals("ABC", segment.tag());
            assertTrue(segment.elements().isEmpty());
        }

        @Test
        void parseSegment_preservesEmptyElementAndEmptyComponents() {
            Segment segment = parser.parseSegment("IMD+F++:::Premium Widget Model A");

            assertEquals("IMD", segment.tag());

            CompositeDataElement compositeDataElement = assertInstanceOf(CompositeDataElement.class, segment.elements().get(2));
            assertEquals(4, compositeDataElement.components().size());
            assertEquals(List.of("", "", "", "Premium Widget Model A"), compositeDataElement.components());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().get(1));
            assertEquals("", simpleDataElement.value());
        }

        @Test
        void parseSegment_preservesEmptyComponents() {
            Segment segment = parser.parseSegment("NAD+BY+5412345000016::9");

            assertEquals("NAD", segment.tag());

            CompositeDataElement compositeDataElement = assertInstanceOf(CompositeDataElement.class, segment.elements().get(1));
            assertEquals(3, compositeDataElement.components().size());
            assertEquals(List.of("5412345000016", "", "9"), compositeDataElement.components());
        }

        @Test
        void parseSegment_preservesEmptyElement() {
            Segment segment = parser.parseSegment("LIN+1++9780123456789:EN");

            assertEquals("LIN", segment.tag());
            assertEquals(3, segment.elements().size());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().get(1));
            assertEquals("", simpleDataElement.value());
        }

        @Test
        void parseSegment_unescapesEscapedTerminator() {
            Segment segment = parser.parseSegment("NAD+BY+O?'Reilly");

            assertEquals("NAD", segment.tag());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().get(1));
            assertEquals("O'Reilly", simpleDataElement.value());
        }

        @Test
        void parseSegment_unescapesEscapedReleaseCharacter() {
            Segment segment = parser.parseSegment("PRI+AAA:??10");

            assertEquals("PRI", segment.tag());
            CompositeDataElement compositeDataElement = assertInstanceOf(CompositeDataElement.class, segment.elements().getFirst());
            assertEquals(List.of("AAA", "?10"), compositeDataElement.components());
        }

        @Test
        void parseSegment_keepsEscapedDataElementSeparatorInValue() {
            Segment segment = parser.parseSegment("AG+ACME ?+ Co");

            assertEquals("AG", segment.tag());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().getFirst());
            assertEquals("ACME + Co", simpleDataElement.value());
        }

        @Test
        void parseSegment_keepsEscapedComponentSeparatorInValue() {
            Segment segment = parser.parseSegment("TAG+foo?:bar");

            assertEquals("TAG", segment.tag());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().getFirst());
            assertEquals("foo:bar", simpleDataElement.value());
        }

        @Test
        void parseSegment_treatsEvenRunOfReleaseCharsAsRealDelimiter() {
            Segment segment = parser.parseSegment("TAG+A??+B");

            assertEquals("TAG", segment.tag());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().getFirst());
            SimpleDataElement simpleDataElement2 = assertInstanceOf(SimpleDataElement.class, segment.elements().get(1));

            assertEquals("A?", simpleDataElement.value());
            assertEquals("B", simpleDataElement2.value());
        }

        @Test
        void parseSegment_treatsOddRunOfReleaseCharsAsEscapedDelimiter() {
            Segment segment = parser.parseSegment("TAG+X???+Y");

            assertEquals("TAG", segment.tag());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().getFirst());
            assertEquals("X?+Y", simpleDataElement.value());
        }

        @Test
        void parseSegment_dropsTrailingReleaseCharacter() {
            Segment segment = parser.parseSegment("TAG+foo?");

            assertEquals("TAG", segment.tag());
            SimpleDataElement simpleDataElement = assertInstanceOf(SimpleDataElement.class, segment.elements().getFirst());
            assertEquals("foo", simpleDataElement.value());
        }

        @Test
        void parseSegment_returnsEmptyTagForEmptySegment() {
            Segment segment = parser.parseSegment("");

            assertEquals("", segment.tag());
            assertTrue(segment.elements().isEmpty());
        }

    }

    @Nested
    class ParseTests {

        @Test
        void parse_returnsFullDocument_withoutUna() {
            EdifactDocument document = Parser.parse(MESSAGE);

            assertNull(document.una());
            assertEquals("UNB", document.interchange().header().tag());
            assertEquals(1, document.interchange().messages().size());
            assertEquals("UNZ", document.interchange().trailer().tag());
        }

        @Test
        void parse_returnsFullDocument_withUna_andAppliesCustomDelimiters() {
            String raw = "UNA|*,~ !UNB*UNOA|3*SENDER*RECEIVER*240816|1200*1!UNH*1*ORDERS|D|96A|UN!BGM*220*ORD001!UNT*3*1!UNZ*1*1!";

            EdifactDocument document = Parser.parse(raw);

            assertEquals("UNA|*,~ !", document.una());
            assertEquals("UNB", document.interchange().header().tag());

            DataElement first = document.interchange().header().elements().getFirst();
            CompositeDataElement composite = assertInstanceOf(CompositeDataElement.class, first);
            assertEquals(List.of("UNOA", "3"), composite.components());

            Message message = document.interchange().messages().getFirst();
            assertEquals("UNH", message.header().tag());
            assertEquals("BGM", message.segments().getFirst().tag());
            assertEquals("UNT", message.trailer().tag());

            assertEquals("UNZ", document.interchange().trailer().tag());
        }

        @Test
        void parse_returnsFullDocument_withUnaUsingDefaultDelimiters() {
            String raw = "UNA:+.? 'UNB+UNOA:3+SENDER+RECEIVER+240816:1200+1'UNH+1+ORDERS:D:96A:UN'BGM+220+ORD001'UNT+3+1'UNZ+1+1'";

            EdifactDocument document = Parser.parse(raw);

            assertEquals("UNA:+.? '", document.una());
            assertEquals("UNB", document.interchange().header().tag());

            DataElement first = document.interchange().header().elements().getFirst();
            CompositeDataElement composite = assertInstanceOf(CompositeDataElement.class, first);
            assertEquals(List.of("UNOA", "3"), composite.components());

            assertEquals(1, document.interchange().messages().size());
            assertEquals("UNZ", document.interchange().trailer().tag());
        }

        @Test
        void parse_throwsInvalidUna_whenUnaIsShorterThanNineChars() {
            ParseException ex = assertThrows(ParseException.class, () -> Parser.parse("UNA:+.?"));
            assertEquals("INVALID_UNA", ex.getCode());
        }

        @Test
        void parse_throwsParseException_whenMessageIsEmpty() {
            ParseException ex = assertThrows(ParseException.class, () -> Parser.parse(""));
            assertEquals("EMPTY_INPUT", ex.getCode());
        }

        @Test
        void parse_throwsParseException_whenMessageIsNull() {
            ParseException ex = assertThrows(ParseException.class, () -> Parser.parse(null));
            assertEquals("EMPTY_INPUT", ex.getCode());
        }
    }

}