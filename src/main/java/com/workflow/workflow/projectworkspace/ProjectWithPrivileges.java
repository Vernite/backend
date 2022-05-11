package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.project.Project;

public record ProjectWithPrivileges(Project project, Long privileges) {
}
