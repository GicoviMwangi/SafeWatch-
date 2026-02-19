package com.safewatch.util.reportRelated;

import com.safewatch.models.Status;

import java.util.Map;
import java.util.Set;

public final class StatusTransition {

    private static final Map<Status, Set<Status>> allowed = Map.of(
            Status.PENDING, Set.of(Status.VERIFIED, Status.REJECTED, Status.FLAGGED),
            Status.VERIFIED, Set.of(Status.PUBLISHED, Status.FLAGGED),
            Status.FLAGGED, Set.of(Status.VERIFIED, Status.REJECTED),
            Status.PUBLISHED, Set.of(),
            Status.REJECTED, Set.of()
    );

    public static void assertAllowed(Status from, Status to) {

        if (!allowed.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("Illegal transition : " + from + " -> " + to);
        }
    }
}
