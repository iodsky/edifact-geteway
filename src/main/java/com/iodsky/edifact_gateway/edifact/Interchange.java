package com.iodsky.edifact_gateway.edifact;

import java.util.List;

public record Interchange(Segment header, List<Message> messages, Segment trailer) { }
