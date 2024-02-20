package com.github.test;

import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class CreationPositiveTest extends BaseTest {

    /**
     * <a href="https://docs.github.com/en/rest/gists/gists?apiVersion=2022-11-28#authentication">...</a>
     */
    @DisplayName("Create a private gist")
    @Test
    public void createPrivateGist() throws IOException, ParseException {
        var privateBody = "{\"description\": \"%s\",\"public\": \"false\", \"files\": {\"%s\": {\"content\": \"%s\"}}";
        var responseJson = sendCreateGist(privateBody, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        assertFalse(gist.isPublic(), "Created gist is not private");
    }

    @DisplayName("Create a public gist")
    @Test
    public void createPublicGist() throws IOException, ParseException {
        var publicBody = "{\"description\": \"%s\",\"public\": \"true\", \"files\": {\"%s\": {\"content\": \"%s\"}}";
        var responseJson = sendCreateGist(publicBody, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        assertTrue(gist.isPublic(), "Gist is not public");
    }

    /**
     * <a href="https://docs.github.com/en/rest/gists/gists?apiVersion=2022-11-28#truncation">...</a>
     */
    @DisplayName("Check 1 MB truncation")
    @Test
    public void create1MBGist() throws IOException, ParseException {
        var chars = new char[1024 * 1024];
        Arrays.fill(chars, 'f');
        var truncated = "test_truncated";
        var file = new String(chars) + truncated;

        var responseJson = sendCreateGist(body, description, fileName, file);
        var gist = getGistFromResponse(responseJson);
        var gistFile = gist.getFiles().get(fileName);
        assertTrue(gistFile.isTruncated(), "Gist was not truncated");
        assertFalse(responseJson.endsWith(truncated), "Truncated part still shown");

        var getRequest = new HttpGet(gistFile.getRawUrl());
        addHeaders(getRequest);

        responseJson = clientBase.get(getRequest).getPayload();
        assertTrue(responseJson.endsWith(truncated), "Truncated part was lost");
    }

    @DisplayName("Check 300 files truncation")
    @Test
    public void check300FilesGist() throws IOException, ParseException {
        var files = new StringBuilder();
        for (var i = 0; i < 301; i++) {
            files
                    .append("\"")
                    .append(i)
                    .append("\": {\"content\": \"")
                    .append(i)
                    .append("\"}")
                    .append(",");
        }

        var lastComma = files.toString().lastIndexOf(",");
        var filesString = files.substring(0, lastComma);

        var filesBody = "{\"description\": \"%s\", \"files\": {" + filesString + "}";
        var postRequest = new HttpPost(baseUrl);
        var entity = new StringEntity(String.format(filesBody, description));
        postRequest.setEntity(entity);
        addHeaders(postRequest);
        var gistJson = clientBase.executeRequestWithEntity(postRequest).getPayload();
        var gist = getGistFromResponse(gistJson);
        assertTrue(gist.isTruncated(), "Gist is not truncated");
    }

    @DisplayName("Create a gist without description")
    @Test
    public void checkWithoutDescription() throws IOException, ParseException {
        var responseJson = sendCreateGist(body, "", fileName, content);
        var gist = getGistFromResponse(responseJson);
        assertEquals(gist.getDescription(), "", "Gist description is not empty");
    }

    @DisplayName("Create a gist without files")
    @Test
    public void checkWithoutFiles() throws IOException, ParseException {
        var responseJson = sendCreateGist(body, description, "", content);
        var gist = getGistFromResponse(responseJson);
        assertNotNull(gist.getFiles().get("gistfile1.txt"), "Files were not created automatically in the gist without files");
    }
}
