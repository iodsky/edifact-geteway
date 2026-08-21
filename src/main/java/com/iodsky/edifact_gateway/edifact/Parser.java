package com.iodsky.edifact_gateway.edifact;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final Delimiters delimiters;

    public Parser(Delimiters delimiters) {
        this.delimiters = delimiters;
    }

    public List<String> tokenize(String message) {

        if (message == null || message.isEmpty()) {
            throw new ParseException("Empty message", "EMPTY_INPUT");
        }

        String normalized = message.replace("\n", "").replace("\r", "");

        return splitDropTrailing(normalized, delimiters.segmentTerminator());
    }

    public List<Segment> parseSegments(List<String> rawSegments) {
        List<Segment> segments = new ArrayList<>();
        for (String rawSegment: rawSegments) {
            Segment segment = parseSegment(rawSegment);
            segments.add(segment);
        }

        return segments;
    }

    public Segment parseSegment(String rawSegment) {

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
