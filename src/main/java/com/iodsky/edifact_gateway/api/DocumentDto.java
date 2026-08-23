package com.iodsky.edifact_gateway.api;

import com.iodsky.edifact_gateway.edifact.EdifactDocument;

public record DocumentDto(String una, InterchangeDto interchange) {

    public static DocumentDto from(EdifactDocument d) {
        return new DocumentDto(d.una(), InterchangeDto.from(d.interchange()));
    }

}
