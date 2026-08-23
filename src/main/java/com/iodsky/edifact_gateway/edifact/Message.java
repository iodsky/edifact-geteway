package com.iodsky.edifact_gateway.edifact;

import java.util.List;

public record Message(Segment header, List<Segment> segments, Segment trailer) { }
