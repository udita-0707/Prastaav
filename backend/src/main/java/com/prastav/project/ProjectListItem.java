package com.prastav.project;

import java.time.Instant;

/** One row of the project list: the project plus when its proposal was first submitted (null while never submitted). */
public record ProjectListItem(Project project, Instant submittedAt) {
}
