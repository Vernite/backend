package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.user.User;

/**
 * Class representing user and privillages in a project.
 */
public record ProjectMember(User user, Long privileges) {
}
