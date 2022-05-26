package com.workflow.workflow;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Tokens {
    public static final Tokens TOKENS;

    static {
        try {
            TOKENS = new ObjectMapper().readValue(new File("tokens.json"), Tokens.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String githubKey;
    public String maxmindKey;

    private Tokens() {
    }
}
