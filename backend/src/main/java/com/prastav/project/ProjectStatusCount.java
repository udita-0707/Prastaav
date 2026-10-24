package com.prastav.project;

import com.prastav.workflow.ProjectStatus;

/** Projection for the grouped count queries in {@link ProjectRepository}. */
public interface ProjectStatusCount {

    ProjectStatus getStatus();

    long getCount();
}
