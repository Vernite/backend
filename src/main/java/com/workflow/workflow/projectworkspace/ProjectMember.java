package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.user.User;

public record ProjectMember(User user, Long privileges) {}
