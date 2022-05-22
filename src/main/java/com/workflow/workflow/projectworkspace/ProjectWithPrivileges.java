package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.project.Project;

/**
 * Class representing project and privileges for user.
 */
public record ProjectWithPrivileges(Project project, Long privileges) implements Comparable<ProjectWithPrivileges> {
    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(privileges);
        hash = prime * hash + (project == null ? 0 : project.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProjectWithPrivileges other = (ProjectWithPrivileges) obj;
        if (privileges != other.privileges)
            return false;
        if (project == null)
            return other.project == null;
        return project.equals(other.project);
    }

    @Override
    public int compareTo(ProjectWithPrivileges o) {
        return project.compareTo(o.project);
    }
}
