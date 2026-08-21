package com.iodsky.edifact_gateway.edifact;

/**
 * Utilizing sealed interface to only permit SimpleDataElement and CompositeDataElement to implement this interface
 */
public sealed interface DataElement permits SimpleDataElement, CompositeDataElement { }
