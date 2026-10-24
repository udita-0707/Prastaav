package com.prastav.project;

import org.springframework.data.domain.Sort;

/** Orderings offered by the project list. Ties are broken by id so pages never overlap. */
public enum ProjectSort {
    LATEST(Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"))),
    OLDEST(Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id"))),
    TITLE(Sort.by(Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id")));

    private final Sort sort;

    ProjectSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
