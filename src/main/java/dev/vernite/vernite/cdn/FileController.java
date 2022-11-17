package dev.vernite.vernite.cdn;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import dev.vernite.vernite.utils.ObjectNotFoundException;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileManager fileManager;

    @GetMapping("/{hash}")
    public ResponseEntity<StreamingResponseBody> getFile(@PathVariable String hash) {
        File f = fileRepository.findByHash(hash);
        if (f == null) {
            throw new ObjectNotFoundException();
        }
        try {
            Blob b = f.getFile();
            long len = b.length();
            InputStream is = b.getBinaryStream();
            return ResponseEntity.ok()
                    .contentLength(len)
                    .contentType(MediaType.parseMediaType(f.getContentType()))
                    .body(out -> {
                        is.transferTo(out);
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "sql error");
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public File uploadFile(String contentType, @RequestParam("file") MultipartFile file) {
        try {
            return fileManager.uploadFile(contentType, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
