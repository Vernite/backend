package dev.vernite.vernite.cdn;

import org.springframework.data.repository.CrudRepository;

public interface FileRepository extends CrudRepository<File, Long> {
    File findByHash(String hash);
}
