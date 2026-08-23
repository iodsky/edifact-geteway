package com.iodsky.edifact_gateway.api;

import com.iodsky.edifact_gateway.edifact.Interchange;

import java.util.List;

public record InterchangeDto (SegmentDto header, List<MessageDto> messages, SegmentDto trailer) {
    public static InterchangeDto from(Interchange i) {
        return new InterchangeDto(SegmentDto.from(i.header()), i.messages().stream().map(MessageDto::from).toList(), SegmentDto.from(i.trailer()));
    }
}
