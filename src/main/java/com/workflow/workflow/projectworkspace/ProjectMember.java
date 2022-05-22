package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.user.User;

/**
 * Class representing user and privillages in a project.
 */
public record ProjectMember(User user, Long privileges) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProjectMember other = (ProjectMember) obj;
        if (privileges != other.privileges)
            return false;
        if (user == null)
            return other.user == null;
        return user.equals(other.user);
    }
}
