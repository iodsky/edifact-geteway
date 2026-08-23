package com.iodsky.edifact_gateway.api;

import com.iodsky.edifact_gateway.edifact.CompositeDataElement;
import com.iodsky.edifact_gateway.edifact.DataElement;
import com.iodsky.edifact_gateway.edifact.Segment;
import com.iodsky.edifact_gateway.edifact.SimpleDataElement;

import java.util.ArrayList;
import java.util.List;

public record SegmentDto(String tag, List<Object> elements) {

    public static SegmentDto from(Segment segment) {

        List<Object> elements = new ArrayList<>();
        for (DataElement element: segment.elements()) {
            switch (element) {
                case SimpleDataElement s -> {
                    elements.add(s.value());
                }
                case CompositeDataElement c -> {
                    elements.add(c.components());
                }
            }
        }

        return new SegmentDto(segment.tag(), elements);
    }
}
