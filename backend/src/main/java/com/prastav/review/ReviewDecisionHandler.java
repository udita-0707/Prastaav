package com.prastav.review;

import com.prastav.auth.Actor;

import java.util.UUID;

/**
 * <b>Strategy pattern.</b> One implementation per {@link ReviewDecision}; each one triggers the matching
 * workflow transition.
 * <p>
 * Why: {@link ReviewService} looks the handler up by decision instead of switching on it, so a new
 * decision type is a new class and the service does not change (Open/Closed). All handlers are
 * interchangeable behind this interface (Liskov).
 */
public interface ReviewDecisionHandler {

    ReviewDecision decision();

    void apply(UUID projectId, Actor actor, String comments);
}
