package com.prastav.proposal;

/** One proposal field in a version comparison: its value in both versions and whether it changed. */
public record FieldDiff(String name, String fromValue, String toValue, boolean changed) {
}
