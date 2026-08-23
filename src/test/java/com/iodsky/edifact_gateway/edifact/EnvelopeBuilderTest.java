package com.iodsky.edifact_gateway.edifact;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvelopeBuilderTest {

    @Test
    void build_singleMessage() {
        Interchange interchange = EnvelopeBuilder.build(segments("UNB", "UNH", "BGM", "UNT", "UNZ"));

        assertEquals("UNB", interchange.header().tag());
        assertEquals("UNZ", interchange.trailer().tag());
        assertEquals(1, interchange.messages().size());

        Message message = interchange.messages().getFirst();
        assertEquals("UNH", message.header().tag());
        assertEquals(1, message.segments().size());
        assertEquals("BGM", message.segments().getFirst().tag());
        assertEquals("UNT", message.trailer().tag());
    }

    @Test
    void build_multipleMessages() {
        Interchange interchange = EnvelopeBuilder.build(segments("UNB", "UNH", "BGM", "UNT", "UNH", "DTM", "UNT", "UNZ"));

        assertEquals("UNB", interchange.header().tag());
        assertEquals("UNZ", interchange.trailer().tag());
        assertEquals(2, interchange.messages().size());

        Message first  = interchange.messages().get(0);
        Message second = interchange.messages().get(1);

        assertEquals("UNH", first.header().tag());
        assertEquals("BGM", first.segments().getFirst().tag());
        assertEquals("UNT", first.trailer().tag());

        assertEquals("UNH", second.header().tag());
        assertEquals("DTM", second.segments().getFirst().tag());
        assertEquals("UNT", second.trailer().tag());
    }

    @Test
    void build_throwsEmptyInput_whenNoSegments() {
        ParseException ex = assertThrows(ParseException.class, () -> {
            EnvelopeBuilder.build(List.of());
        });

        assertEquals("EMPTY_INPUT", ex.getCode());
    }

    @Test
    void build_throwsMissingUnb_whenFirstSegmentIsNotUnb() {
        ParseException ex = assertThrows(ParseException.class, () -> {
            EnvelopeBuilder.build(segments("UNH", "BGM", "UNT", "UNZ"));
        });

        assertEquals("MISSING_UNB", ex.getCode());
    }

    @Test
    void build_throwsMissingUnz_whenNoUnz() {
        ParseException ex = assertThrows(ParseException.class, () -> {
            EnvelopeBuilder.build(segments("UNB", "UNH", "BGM", "UNT"));
        });

        assertEquals("MISSING_UNZ", ex.getCode());
    }

    @Test
    void build_throwsUnclosedMessage_whenMessageNeverClosed() {
        ParseException ex = assertThrows(ParseException.class, () -> {
            EnvelopeBuilder.build(segments("UNB", "UNH", "BGM"));
        });

        assertEquals("UNCLOSED_MESSAGE", ex.getCode());
    }

    @Test
    void build_throwsUnclosedMessage_whenNestedUnh() {
        ParseException ex = assertThrows(ParseException.class, () -> {
            EnvelopeBuilder.build(segments("UNB", "UNH", "UNH"));
        });

        assertEquals("UNCLOSED_MESSAGE", ex.getCode());
    }

    @Test
    void build_throwsUnexpectedEnd_whenDataOutsideMessage() {
        ParseException ex = assertThrows(ParseException.class, () -> {
            EnvelopeBuilder.build(segments("UNB", "BGM"));
        });

        assertEquals("UNEXPECTED_END", ex.getCode());
    }

    @Test
    void build_throwsUnexpectedEnd_whenDataAfterUnz() {
        ParseException ex = assertThrows(ParseException.class, () -> {
            EnvelopeBuilder.build(segments("UNB", "UNH", "BGM", "UNT", "UNZ", "BGM"));
        });

        assertEquals("UNEXPECTED_END", ex.getCode());
    }

    private List<Segment> segments(String... tags) {
        return Arrays.stream(tags).map(this::segment).toList();
    }

    private Segment segment(String tag) {
        return new Segment(tag, List.of());
    }

}
