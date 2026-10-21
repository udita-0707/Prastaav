package com.prastav.auth.web;

import java.util.UUID;

/** Compact user reference embedded in other responses (owner, reviewer). */
public record UserSummary(UUID id, String name) {
}
