package com.workflow.workflow.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface UserSessionRepository extends CrudRepository<UserSession, Long> {
    Optional<UserSession> findBySession(String session);
    List<UserSession> findByUser(User user);
}
