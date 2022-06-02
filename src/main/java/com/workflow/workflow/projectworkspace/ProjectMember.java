package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.user.User;

/**
 * Class representing user and privillages in a project.
 */
public record ProjectMember(User user, long privileges) {
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProjectMember other = (ProjectMember) obj;
        if (privileges != other.privileges)
            return false;
        if (user == null)
            return other.user == null;
        if (other.user == null)
            return false;
        return user.getId() == other.user.getId();
    }
}
