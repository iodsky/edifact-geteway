package com.iodsky.edifact_gateway.edifact;

import java.util.ArrayList;
import java.util.List;

public class EnvelopeBuilder {

    public static Interchange build(List<Segment> segments) {
        // [ UNB, UNH, BGM, DTM, UNT, UNH, BGM, UNT, UNZ ]
        if (segments.isEmpty()) {
            throw new ParseException("EDIFACT message must not be empty", "EMPTY_INPUT");
        }

        if (!segments.getFirst().tag().equals("UNB")) {
            throw new ParseException("Expected UNB as first segment but found " + segments.getFirst().tag(), "MISSING_UNB", 1, null);
        }

        final Segment interchangeHeader = segments.getFirst();
        final List<Message> messages = new ArrayList<>();
        Segment currentHeader = null;
        List<Segment> currentSegments = null;
        Segment interchangeTrailer = null;

        for (int i = 1; i < segments.size(); i++) {
            Segment segment = segments.get(i);

            switch (segment.tag()) {
                case "UNH" -> {
                    if (currentHeader != null) {
                        throw new ParseException("UNH encountered before previous message was closed with UNT", "UNCLOSED_MESSAGE", i + 1, null);
                    }

                    currentHeader = segment;
                    currentSegments = new ArrayList<>();
                }
                case "UNT" -> {
                    if (currentHeader == null) {
                        throw new ParseException("UNT encountered without a matching UNH", "UNEXPECTED_END", i + 1, null);
                    }

                    messages.add(new Message(currentHeader, currentSegments, segment));
                    currentHeader = null;
                }
                case "UNZ" -> {
                    if (currentHeader != null) {
                        throw new ParseException("UNZ encountered before message was closed with UNT", "UNCLOSED_MESSAGE", i + 1, null);
                    }

                    interchangeTrailer = segment;
                }
                default -> {
                    if (currentHeader != null) {
                        currentSegments.add(segment);
                    } else {
                        throw new ParseException("Segment " + segment.tag() + " encountered outside a message", "UNEXPECTED_END", i + 1, null);
                    }
                }
            }
        }

        if (currentHeader != null) {
            throw new ParseException("Message not closed: missing UNT", "UNCLOSED_MESSAGE");
        } else if (interchangeTrailer == null) {
            throw new ParseException("Interchange not closed: missing UNZ segment", "MISSING_UNZ");
        }

        return new Interchange(interchangeHeader, messages, interchangeTrailer);
    }

}
