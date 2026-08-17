package com.iodsky.edifact_gateway.edifact;

public record Delimiters(
        char componentSeparator,
        char dataElementSeparator,
        char decimalMark,
        char releaseCharacter,
        char segmentTerminator
) {

    public static Delimiters defaults() {
        return new Delimiters(
                ':',
                '+',
                '.',
                '?',
                '\''
        );
    }

    public static Delimiters fromUna(String una) {
        /*
         * U N A : + . ?   '
         * 0 1 2 3 4 5 6 7 8
         */
        if (una == null || !una.startsWith("UNA") || una.length() != 9) {
            throw new ParseException("Invalid UNA", "INVALID_UNA");
        }

        return new Delimiters(una.charAt(3), una.charAt(4), una.charAt(5), una.charAt(6), una.charAt(8));
    }
}
