package com.prastav.project;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectListQueryTest {

    @Test
    void blankTextAndMissingValuesFallBackToDefaults() {
        ProjectListQuery query = new ProjectListQuery(null, "   ", null, null, null, null);

        assertNull(query.q());
        assertTrue(query.statuses().isEmpty());
        assertEquals(ProjectSort.LATEST, query.sort());
        assertTrue(query.pageable().isUnpaged());
    }

    @Test
    void searchTextIsTrimmed() {
        assertEquals("crop", new ProjectListQuery(null, "  crop ", null, null, null, null).q());
    }

    @Test
    void pageSizeIsClampedAndPageIsNeverNegative() {
        Pageable tooBig = new ProjectListQuery(null, null, null, null, 0, 500).pageable();
        Pageable tooSmall = new ProjectListQuery(null, null, null, null, -3, 0).pageable();
        Pageable defaulted = new ProjectListQuery(null, null, null, null, 2, null).pageable();

        assertEquals(ProjectListQuery.MAX_SIZE, tooBig.getPageSize());
        assertEquals(1, tooSmall.getPageSize());
        assertEquals(0, tooSmall.getPageNumber());
        assertEquals(ProjectListQuery.DEFAULT_SIZE, defaulted.getPageSize());
        assertEquals(2, defaulted.getPageNumber());
    }

    @Test
    void likeWildcardsInSearchTextAreEscaped() {
        assertEquals("50\\%\\_off\\\\", ProjectSpecifications.escapeLike("50%_off\\"));
    }
}
