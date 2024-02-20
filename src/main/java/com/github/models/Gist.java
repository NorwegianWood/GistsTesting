package com.github.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import java.util.Map;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Gist {
    private String url;
    private String id;
    private Map<String, GistFile> files;
    @JsonProperty("public")
    private boolean isPublic;
    @JsonProperty("created_at")
    private String createdAt;
    private String description;
    private Owner owner;
    private boolean truncated;
}
