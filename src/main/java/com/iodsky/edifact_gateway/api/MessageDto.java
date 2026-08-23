package com.iodsky.edifact_gateway.api;

import com.iodsky.edifact_gateway.edifact.Message;

import java.util.List;

public record MessageDto(SegmentDto header, List<SegmentDto> segments, SegmentDto trailer) {
    public static MessageDto from(Message m) {
        return new MessageDto(
                SegmentDto.from(m.header()),
                m.segments().stream().map(SegmentDto::from).toList(),
                SegmentDto.from(m.trailer())
        );
    }
}
