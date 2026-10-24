package com.prastav.project;

import com.prastav.common.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The project module's load/save port for other modules (workflow, proposal, review).
 * They depend on this class instead of on {@link ProjectRepository}, which keeps the module boundary
 * and avoids a constructor cycle with {@link ProjectService}.
 */
@Component
public class ProjectStore {

    private final ProjectRepository projectRepository;

    public ProjectStore(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project getById(UUID projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project", projectId));
    }

    public Project save(Project project) {
        return projectRepository.save(project);
    }
}
