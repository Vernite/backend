package dev.vernite.vernite.integration.git.github.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.vernite.vernite.release.Release;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GitHubRelease {

    @JsonProperty("tag_name")
    private String tagName;
    private String body;
    @JsonProperty("generate_release_notes")
    private boolean generateReleaseNotes;
    @JsonProperty("target_commitish")
    private String targetCommitish;

    public GitHubRelease(Release release) {
        this.tagName = release.getName().replace(" ", "-");
        this.body = release.getDescription();
        this.generateReleaseNotes = true;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTagName() {
        return tagName;
    }

    public void setGenerateReleaseNotes(boolean generateReleaseNotes) {
        this.generateReleaseNotes = generateReleaseNotes;
    }

    public boolean getGenerateReleaseNotes() {
        return generateReleaseNotes;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTargetCommitish() {
        return targetCommitish;
    }

    public void setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish;
    }
}
