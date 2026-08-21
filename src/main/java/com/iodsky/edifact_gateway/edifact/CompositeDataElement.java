package com.iodsky.edifact_gateway.edifact;

import java.util.List;

public record CompositeDataElement(List<String> components) implements DataElement { }
