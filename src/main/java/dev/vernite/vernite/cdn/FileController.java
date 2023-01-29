package dev.vernite.vernite.cdn;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import dev.vernite.vernite.utils.ObjectNotFoundException;
import lombok.AllArgsConstructor;

/**
 * Controller for file serving.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/cdn")
public class FileController {

    private FileRepository fileRepository;

    /**
     * Returns file stored on the server.
     * 
     * @param req  request
     * @param hash hash of the file
     * @return file
     */
    @GetMapping("/{hash}")
    public ResponseEntity<StreamingResponseBody> getFile(NativeWebRequest req, @PathVariable String hash) {
        File f = fileRepository.findByHash(hash);
        if (f == null) {
            throw new ObjectNotFoundException();
        }
        if (req.checkNotModified(f.getHash(), f.getUploaded().getTime())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        try {
            Blob b = f.getFile();
            long len = b.length();
            InputStream is = b.getBinaryStream();
            return ResponseEntity.ok()
                    .contentLength(len)
                    .header("Cache-Control", "public, max-age=604800, immutable")
                    .contentType(MediaType.parseMediaType(f.getContentType()))
                    .body(out -> {
                        is.transferTo(out);
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "sql error");
        }
    }

}
