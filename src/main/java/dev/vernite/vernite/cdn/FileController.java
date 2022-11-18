package dev.vernite.vernite.cdn;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
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

import dev.vernite.vernite.utils.ErrorType;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/cdn")
public class FileController {

    @Autowired
    private FileRepository fileRepository;

    @Operation(summary = "Returns file", description = "Returns file stored on the server")
    @ApiResponse(description = "Content of the file", responseCode = "200")
    @ApiResponse(description = "File not found", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
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
