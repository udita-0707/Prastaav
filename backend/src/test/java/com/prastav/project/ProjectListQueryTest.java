package com.prastav.project;

import com.prastav.common.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectListQueryTest {

    @Test
    void missingValuesFallBackToFirstPageOfTenLatestFirst() {
        ProjectListQuery query = ProjectListQuery.of(null, "   ", null, null, null, null, null, null);

        assertNull(query.q());
        assertTrue(query.statuses().isEmpty());
        assertEquals(ProjectSort.LATEST, query.sort());
        assertEquals(PageRequest.of(0, ProjectListQuery.DEFAULT_SIZE, ProjectSort.LATEST.toSort()), query.pageable());
    }

    @Test
    void searchTextIsTrimmed() {
        assertEquals("crop", ProjectListQuery.of(null, "  crop ", null, null, null, null, null, null).q());
    }

    @Test
    void branchIsTrimmedAndUpperCasedAndBlankCohortIsNoFilter() {
        ProjectListQuery cohort = ProjectListQuery.of(null, null, null, 2023, " csai ", null, null, null);
        ProjectListQuery blank = ProjectListQuery.of(null, null, null, null, "  ", null, null, null);

        assertEquals("CSAI", cohort.branch());
        assertTrue(cohort.filtersByCohort());
        assertNull(blank.branch());
        assertFalse(blank.filtersByCohort());
        assertTrue(ProjectListQuery.of(null, null, null, 2024, null, null, null, null).filtersByCohort());
    }

    @Test
    void pageAndSizeArePassedThroughWithinBounds() {
        Pageable largest = ProjectListQuery.of(null, null, null, null, null, ProjectSort.TITLE, 3, ProjectListQuery.MAX_SIZE).pageable();
        Pageable smallest = ProjectListQuery.of(null, null, null, null, null, null, 0, 1).pageable();

        assertEquals(PageRequest.of(3, 50, ProjectSort.TITLE.toSort()), largest);
        assertEquals(1, smallest.getPageSize());
    }

    @Test
    void negativePageIsRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> ProjectListQuery.of(null, null, null, null, null, null, -1, null));
        assertEquals("page must be 0 or greater", error.getMessage());
    }

    @Test
    void sizeOutsideOneToFiftyIsRejected() {
        assertThrows(ValidationException.class, () -> ProjectListQuery.of(null, null, null, null, null, null, 0, 0));
        ValidationException tooBig = assertThrows(ValidationException.class,
                () -> ProjectListQuery.of(null, null, null, null, null, null, 0, 51));
        assertEquals("size must be between 1 and 50", tooBig.getMessage());
    }

    @Test
    void likeWildcardsInSearchTextAreEscaped() {
        assertEquals("50\\%\\_off\\\\", ProjectSpecifications.escapeLike("50%_off\\"));
    }
}
