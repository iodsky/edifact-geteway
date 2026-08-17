package com.iodsky.edifact_gateway.edifact;

public class ParseException extends RuntimeException {

    private final String code;
    private final Integer segmentIndex;
    private final Integer elementIndex;

    public ParseException(String message, String code) {
        super(message);
        this.code = code;
        this.segmentIndex = null;
        this.elementIndex = null;
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
