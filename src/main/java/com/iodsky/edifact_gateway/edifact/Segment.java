package com.iodsky.edifact_gateway.edifact;

import java.util.List;

public record Segment(String tag, List<DataElement> elements) { }