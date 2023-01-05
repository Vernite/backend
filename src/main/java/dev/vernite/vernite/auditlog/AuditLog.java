/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.auditlog;

import java.util.Date;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class AuditLog {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private long id;

    @Column(nullable = false)
    @Getter
    @Setter
    private Date date;
    
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auditlog_user"))
    @ManyToOne(optional = false)
    @Getter
    @Setter
    private User user;
    
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auditlog_project"))
    @ManyToOne(optional = false)
    @JsonIgnore
    @Getter
    @Setter
    private Project project;

    @Column(nullable = false)
    @Getter
    @Setter
    private String type;

    @Column(columnDefinition = "MEDIUMTEXT")
    @Setter
    private String oldValues;
    @Column(columnDefinition = "MEDIUMTEXT")
    @Setter
    private String newValues;
    @Column(columnDefinition = "MEDIUMTEXT")
    @Setter
    private String sameValues;

    /**
     * out[0] = oldValues
     * out[1] = newValues
     * out[2] = sameValues
     */
    public void apply(ObjectMapper mapper, JsonNode[] out) throws JsonProcessingException {
        if (out[0] != null) {
            this.setOldValues(mapper.writeValueAsString(out[0]));
        } else {
            this.setSameValues(null);
        }
        if (out[1] != null) {
            this.setNewValues(mapper.writeValueAsString(out[1]));
        } else {
            this.setSameValues(null);
        }
        if (out[2] != null) {
            this.setSameValues(mapper.writeValueAsString(out[2]));
        } else {
            this.setSameValues(null);
        }
    }

    public JsonNode getOldValues() throws JsonMappingException, JsonProcessingException {
        if (this.oldValues == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(this.oldValues);
    }

    public JsonNode getNewValues() throws JsonMappingException, JsonProcessingException {
        if (this.newValues == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(this.newValues);
    }

    public JsonNode getSameValues() throws JsonMappingException, JsonProcessingException {
        if (this.sameValues == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(this.sameValues);
    }
    
}
