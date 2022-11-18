package dev.vernite.vernite.cdn;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HexFormat;

import javax.sql.rowset.serial.SerialBlob;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
public class FileManager {

    @Autowired
    private FileRepository fileRepository;
    
    /**
     * Saves a file to the database or returns the existing one.
     * @param contentType
     * @param data
     * @return the file
     */
    public File uploadFile(String contentType, byte[] data) {
        String hash = calculateHash(data);
        File f = fileRepository.findByHash(hash);
        if (f != null) {
            return f;
        }
        f = new File();
        f.setContentType(contentType);
        f.setHash(hash);
        f.setUploaded(new Date());
        try {
            f.setFile(new SerialBlob(data));
        } catch (SQLException e) {
            // never happens
        }
        f = fileRepository.save(f);
        return f;
    }

    private static String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
