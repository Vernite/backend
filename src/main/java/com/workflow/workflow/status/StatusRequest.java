package com.workflow.workflow.status;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class StatusRequest {
    private String name;
    private int color;
    private boolean isFinal;
    private boolean isBegin;
    private int ordinal;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be empty");
            } else if (name.length() > 50) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be longer than 50 characters");
            }
        }
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isBegin() {
        return isBegin;
    }

    public void setBegin(boolean isBegin) {
        this.isBegin = isBegin;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

}
