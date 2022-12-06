package dev.vernite.vernite.cdn;

import java.sql.Blob;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;

    @Column(nullable = false)
    private Date uploaded;

    @Column(nullable = false, unique = true)
    private String hash;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    @JsonIgnore
    private Blob file;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getUploaded() {
        return uploaded;
    }

    public void setUploaded(Date uploaded) {
        this.uploaded = uploaded;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contetType) {
        this.contentType = contetType;
    }

    public Blob getFile() {
        return file;
    }

    public void setFile(Blob file) {
        this.file = file;
    }

    public String getURL() {
        return "/api/cdn/" + hash;
    }
}
