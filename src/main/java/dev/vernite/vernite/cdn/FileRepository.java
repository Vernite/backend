package dev.vernite.vernite.cdn;

import org.springframework.data.repository.CrudRepository;

/**
 * Repository for files.
 */
public interface FileRepository extends CrudRepository<File, Long> {
    File findByHash(String hash);
}
