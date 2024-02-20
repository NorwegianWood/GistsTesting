package com.github.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GistFile {
    private String filename;
    @JsonProperty("raw_url")
    private String rawUrl;
    private boolean truncated;
    private String content;
}
