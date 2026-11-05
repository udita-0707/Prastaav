package com.prastav.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectSortTest {

    @Test
    void latestIsMostRecentlyUpdatedFirst() {
        assertEquals(Sort.Direction.DESC, ProjectSort.LATEST.toSort().getOrderFor("updatedAt").getDirection());
    }

    @Test
    void oldestIsLeastRecentlyUpdatedFirst() {
        assertEquals(Sort.Direction.ASC, ProjectSort.OLDEST.toSort().getOrderFor("updatedAt").getDirection());
    }

    @Test
    void titleIgnoresCase() {
        Sort.Order title = ProjectSort.TITLE.toSort().getOrderFor("title");

        assertEquals(Sort.Direction.ASC, title.getDirection());
        assertTrue(title.isIgnoreCase());
        assertFalse(ProjectSort.LATEST.toSort().getOrderFor("updatedAt").isIgnoreCase());
    }

    @ParameterizedTest
    @EnumSource(ProjectSort.class)
    void everyOrderEndsWithTheIdSoPagesNeverOverlap(ProjectSort sort) {
        List<Sort.Order> orders = sort.toSort().toList();

        assertEquals("id", orders.get(orders.size() - 1).getProperty());
    }
}
