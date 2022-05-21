package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.project.Project;

/**
 * Class representing project and privileges for user.
 */
public record ProjectWithPrivileges(Project project, Long privileges) {
}
