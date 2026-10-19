package com.prastav.workflow;

import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectStateFactoryTest {

    @Test
    void everyStatusResolvesToItsState() {
        ProjectStateFactory factory = TestData.stateFactory();
        for (ProjectStatus status : ProjectStatus.values()) {
            assertEquals(status, factory.stateOf(status).status());
        }
    }

    @Test
    void missingStateFailsFast() {
        assertThrows(IllegalStateException.class, () -> new ProjectStateFactory(List.of(new DraftState())));
    }

    @Test
    void duplicateStateFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new ProjectStateFactory(List.of(new DraftState(), new DraftState())));
    }
}
