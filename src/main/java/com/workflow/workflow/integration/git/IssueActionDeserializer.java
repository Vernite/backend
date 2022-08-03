package com.workflow.workflow.integration.git;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserializer for {@link IssueAction} enum. It deserializes either string with
 * enum name or JSON object with issue.
 */
public class IssueActionDeserializer extends JsonDeserializer<IssueAction> {

    @Override
    public IssueAction deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String action = jp.getText();
        switch (action.toLowerCase()) {
            case "detach":
                return IssueAction.DETACH;
            case "create":
                return IssueAction.CREATE;
            default:
                IssueAction issueAction = IssueAction.ATTACH;
                issueAction.setIssue(jp.getCodec().readValue(jp, Issue.class));
                return issueAction;
        }
    }
}
