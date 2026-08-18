package com.iodsky.edifact_gateway.edifact;

import java.util.ArrayList;
import java.util.List;


public class Parser {

    private final Delimiters delimiters;

    public Parser(Delimiters delimiters) {
        this.delimiters = delimiters;
    }

    public List<String> parseSegments(String message) {

        if (message == null || message.isEmpty()) {
            throw new ParseException("Empty message", "EMPTY_INPUT");
        }

        List<String> segments = new ArrayList<>();
        // NAD+BY+?:123?:45+ACME ?+ Co+O?'Reilly'
        StringBuilder string = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < message.length(); i++) {

            char c = message.charAt(i);

            if (escaped) {
               string.append(c);
               escaped = false;
               continue;
            }

            if (c == delimiters.releaseCharacter()) {
                escaped = true;
                string.append(c);
                continue;
            }

            if (c == delimiters.segmentTerminator()) {
                segments.add(string.toString());
                string.setLength(0);
                continue;
            }

            if (c == '\r' || c == '\n') {
                continue;
            }

            string.append(c);
        }

        return segments;
    }

}
