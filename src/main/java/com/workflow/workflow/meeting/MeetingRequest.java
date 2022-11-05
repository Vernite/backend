package com.workflow.workflow.meeting;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_ABSENT)
public class MeetingRequest {
    public static final String MISSING = "missing";
    public static final String NULL_VALUE = "null value";
    public static final String TOO_LONG = "too long";

    @Schema(maxLength = 50, minLength = 1, description = "The name of the meeting. Trailing and leading whitespace are removed.")
    private Optional<String> name = Optional.empty();
    @Schema(maxLength = 1000, description = "The description of the meeting. Trailing and leading whitespace are removed.")
    private Optional<String> description = Optional.empty();
    @Schema(maxLength = 1000, description = "The location of the meeting.")
    private Optional<String> location = Optional.empty();
    @Schema(description = "The start date of the meeting.")
    private Optional<Date> startDate = Optional.empty();
    @Schema(description = "The end date of the meeting.")
    private Optional<Date> endDate = Optional.empty();
    @Schema(description = "The list of id of participants of the meeting.")
    private Optional<List<Long>> participantIds = Optional.empty();

    public MeetingRequest() {
    }

    public MeetingRequest(String name, String description, String location, Date startDate, Date endDate,
            List<Long> participantIds) {
        this.name = Optional.ofNullable(name);
        this.description = Optional.ofNullable(description);
        this.location = Optional.ofNullable(location);
        this.startDate = Optional.ofNullable(startDate);
        this.endDate = Optional.ofNullable(endDate);
        this.participantIds = Optional.ofNullable(participantIds);
    }

    /**
     * Creates a new meeting entity from meeting request.
     * 
     * @return the meeting entity.
     * @throws FieldErrorException if the request is invalid.
     */
    public Meeting createEntity() {
        String nameString = getName().orElseThrow(() -> new FieldErrorException("name", MISSING));
        String descriptionString = getDescription().orElse("");
        Date startDateString = getStartDate().orElseThrow(() -> new FieldErrorException("startDate", MISSING));
        Date endDateString = getEndDate().orElseThrow(() -> new FieldErrorException("endDate", MISSING));

        if (startDateString.after(endDateString)) {
            throw new FieldErrorException("dates", "must be after startDate");
        }

        return new Meeting(nameString, descriptionString, startDateString, endDateString);
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public Optional<String> getLocation() {
        return location;
    }

    public Optional<Date> getStartDate() {
        return startDate;
    }

    public Optional<Date> getEndDate() {
        return endDate;
    }

    public Optional<List<Long>> getParticipantIds() {
        return participantIds;
    }

    public void setName(String name) {
        if (name == null) {
            throw new FieldErrorException("name", NULL_VALUE);
        }
        name = name.trim();
        if (name.isEmpty()) {
            throw new FieldErrorException("name", "empty");
        }
        if (name.length() > 50) {
            throw new FieldErrorException("name", TOO_LONG);
        }
        this.name = Optional.of(name);
    }

    public void setDescription(String description) {
        if (description == null) {
            throw new FieldErrorException("description", NULL_VALUE);
        }
        description = description.trim();
        if (description.length() > 1000) {
            throw new FieldErrorException("description", TOO_LONG);
        }
        this.description = Optional.of(description);
    }

    public void setLocation(String location) {
        if (location != null && location.length() > 1000) {
            throw new FieldErrorException("location", TOO_LONG);
        }
        this.location = Optional.ofNullable(location);
    }

    public void setStartDate(Date startDate) {
        if (startDate == null) {
            throw new FieldErrorException("startDate", NULL_VALUE);
        }
        this.startDate = Optional.of(startDate);
    }

    public void setEndDate(Date endDate) {
        if (endDate == null) {
            throw new FieldErrorException("endDate", NULL_VALUE);
        }
        this.endDate = Optional.of(endDate);
    }

    public void setParticipantIds(List<Long> participantIds) {
        if (participantIds == null) {
            throw new FieldErrorException("participantIds", NULL_VALUE);
        }
        this.participantIds = Optional.of(participantIds);
    }
}
