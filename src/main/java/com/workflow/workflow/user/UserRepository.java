package com.workflow.workflow.user;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
    List<User> findByDeletedPermanentlyFalseAndDeletedLessThan(Date date);
}
