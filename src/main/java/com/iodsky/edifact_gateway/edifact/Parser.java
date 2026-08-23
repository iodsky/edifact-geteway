package com.iodsky.edifact_gateway.edifact;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final Delimiters delimiters;

    public Parser(Delimiters delimiters) {
        this.delimiters = delimiters;
    }

    public static EdifactDocument parse(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            throw new ParseException("Empty message", "EMPTY_INPUT");
        }

        String una = null;
        Delimiters d;
        if (rawText.startsWith("UNA")) {
            String unaSegment = rawText.length() >= 9 ? rawText.substring(0, 9) : rawText;
            una = unaSegment;
            d = Delimiters.fromUna(unaSegment);
        } else {
            d = Delimiters.defaults();
        }

        Parser p = new Parser(d);

        String body = una != null ? rawText.substring(9) : rawText;
        List<String> tokenized = p.tokenize(body);
        List<Segment> segments = p.parseSegments(tokenized);

        Interchange interchange = EnvelopeBuilder.build(segments);
        return new EdifactDocument(una, interchange);
    }

    List<String> tokenize(String message) {
        String normalized = message.replace("\n", "").replace("\r", "");

        return splitDropTrailing(normalized, delimiters.segmentTerminator());
    }

    List<Segment> parseSegments(List<String> rawSegments) {
        List<Segment> segments = new ArrayList<>();
        for (String rawSegment: rawSegments) {
            Segment segment = parseSegment(rawSegment);
            segments.add(segment);
        }

        return segments;
    }

    Segment parseSegment(String rawSegment) {

        List<String> rawElements = split(rawSegment, delimiters.dataElementSeparator());
        String tag = unescape(rawElements.getFirst());

        List<DataElement> elements = new ArrayList<>();

        for (int i = 1; i < rawElements.size(); i++) {
            List<String> rawComponents = split(rawElements.get(i), delimiters.componentSeparator());

            if (rawComponents.size() == 1) {
                String value = rawComponents.getFirst();
                elements.add(new SimpleDataElement(unescape(value)));
            } else {
                List<String> list = rawComponents.stream().map(this::unescape).toList();
                elements.add(new CompositeDataElement(list));
            }
        }

        return new Segment(tag, elements);
    }

    private List<String> split(String input, char delimiter) {
        return split(input, delimiter, true);
    }

    private List<String> splitDropTrailing(String input, char delimiter) {
        return split(input, delimiter, false);
    }

    private List<String> split(String input, char delimiter, boolean keepTrailingEmpty) {

        List<String> elements = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        boolean escaped = false;

        for (char c: input.toCharArray()) {
            if (escaped) {
                stringBuilder.append(c);
                escaped = false;
                continue;
            } else if (c == delimiters.releaseCharacter()) {
                escaped = true;
                stringBuilder.append(c);
                continue;
            } else if (c == delimiter) {
                elements.add(stringBuilder.toString());
                stringBuilder.setLength(0);
                continue;
            }

            stringBuilder.append(c);
        }

        if (!stringBuilder.isEmpty() || keepTrailingEmpty) {
            elements.add(stringBuilder.toString());
        }

        return elements;
    }

    private String unescape(String input) {

        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;

        while (index < input.length()) {
            char c = input.charAt(index);

            // ??10 -> ?10
            if (c == delimiters.releaseCharacter()) {
                int nextIndex = index + 1;
                if (nextIndex < input.length()) {
                    stringBuilder.append(input.charAt(nextIndex));
                    index += 2;
                } else {
                    index++;
                }
            } else {
                stringBuilder.append(c);
                index++;
            }
        }

        return stringBuilder.toString();
    }

}
