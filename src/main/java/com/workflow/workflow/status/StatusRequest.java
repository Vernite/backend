package com.workflow.workflow.status;

public class StatusRequest {
    private String name;
    private Integer color;
    private Boolean isFinal;
    private Boolean isBegin;
    private Integer ordinal;

    public StatusRequest() {
    }

    public StatusRequest(String name, Integer color, Boolean isFinal, Boolean isBegin, Integer ordinal) {
        this.name = name;
        this.color = color;
        this.isFinal = isFinal;
        this.isBegin = isBegin;
        this.ordinal = ordinal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            name = name.trim();
        }
        this.name = name;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public Boolean isFinal() {
        return isFinal;
    }

    public void setFinal(Boolean isFinal) {
        this.isFinal = isFinal;
    }

    public Boolean isBegin() {
        return isBegin;
    }

    public void setBegin(Boolean isBegin) {
        this.isBegin = isBegin;
    }

    public Integer getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(Integer ordinal) {
        this.ordinal = ordinal;
    }

}
