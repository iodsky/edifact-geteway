package com.iodsky.edifact_gateway.edifact;

public class ParseException extends RuntimeException {

    private final String code;
    private final Integer segmentIndex;
    private final Integer elementIndex;

    public ParseException(String message, String code) {
        this(message, code, null, null);
    }

    public ParseException(String message, String code, Integer segmentIndex, Integer elementIndex) {
        super(message);
        this.code = code;
        this.segmentIndex = segmentIndex;
        this.elementIndex = elementIndex;
    }

    public String getCode() {
        return code;
    }

    public Integer getSegmentIndex() {
        return segmentIndex;
    }

    public Integer getElementIndex() {
        return elementIndex;
    }
}
