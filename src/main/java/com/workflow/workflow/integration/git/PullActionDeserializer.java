package com.workflow.workflow.integration.git;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserializer for {@link PullAction} enum. It deserializes either string with
 * enum name or JSON object with pull request.
 */
public class PullActionDeserializer extends JsonDeserializer<PullAction> {

    @Override
    public PullAction deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String action = jp.getText();
        if (action.equalsIgnoreCase("detach")) {
            return PullAction.DETACH;
        } else {
            PullAction pullAction = PullAction.ATTACH;
            pullAction.setPullRequest(jp.getCodec().readValue(jp, PullRequest.class));
            return pullAction;
        }
    }
}
