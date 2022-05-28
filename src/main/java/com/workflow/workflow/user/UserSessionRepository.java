package com.workflow.workflow.user;

import java.util.List;
import java.util.Optional;

import javax.persistence.OrderBy;

import org.springframework.data.repository.CrudRepository;

public interface UserSessionRepository extends CrudRepository<UserSession, Long> {
    Optional<UserSession> findBySession(String session);
    @OrderBy("last_used DESC")
    List<UserSession> findByUser(User user);
}
