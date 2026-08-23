package com.iodsky.edifact_gateway.api;

public record ErrorDto(
        int status,
        String code,
        String message,
        Integer segmentIndex,
        Integer elementIndex
) {
}
