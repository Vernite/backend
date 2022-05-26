package com.workflow.workflow.utils;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Interface for automatic throwing not found status code exception when object
 * with given id was not found.
 */
@NoRepositoryBean
public interface NotFoundRepository<T, I> extends CrudRepository<T, I> {

    /**
     * Retrives an entity by its id.
     * 
     * @param id must not be {@literal null}
     * @return the entity with the given id.
     * @throws IllegalArgumentException if {@literal id} is {@literal null}.
     * @throws ResponseStatusException  if entity with given id is not found.
     */
    default T findByIdOrThrow(I id) {
        return findById(id).orElseThrow(NotFoundRepository::getException);
    }

    /**
     * Creates ResponseStatusException with not found status.
     * 
     * @return the exception with message "Object not found".
     */
    public static ResponseStatusException getException() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found");
    }
}
