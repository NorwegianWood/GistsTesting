package com.github.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.models.Gist;
import com.github.rest.ClientBase;
import io.qameta.allure.Step;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.awaitility.Awaitility;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    String body = "{\"description\": \"%s\", \"files\": {\"%s\": {\"content\": \"%s\"}}";
    String fileName = "testFile";
    String content = "testContent";
    String description = "testDescription";
    static String baseUrl;
    static String owner;
    static String token;
    ClientBase clientBase = new ClientBase();

    @BeforeAll
    public static void setUp() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/base.properties"));
        baseUrl = properties.getProperty("baseUrl");
        owner = properties.getProperty("owner");
        token = System.getenv("API_TOKEN");
        if (Objects.isNull(token)) {
            token = properties.getProperty("token");
        }
    }

    @AfterEach
    public final void tearDown() throws IOException, ParseException {
        String response;
        List<Gist> allGists;
        do {
            response = getUsersGists(Map.of());
            allGists = parseGists(response);
            for (var gist : allGists) {
                var status = deleteGist(gist.getId());
                assertEquals(204, status, "Gist with id '" + gist.getId() + "' wasn't deleted");
                checkGistDeletedById(gist.getId());
            }
        } while (!allGists.isEmpty());
    }

    public static List<Gist> parseGists(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        var listType = objectMapper.getTypeFactory().constructCollectionType(List.class, Gist.class);
        return objectMapper.readValue(jsonString, listType);
    }

    @Step("Parse gist from JSON")
    Gist getGistFromResponse(String gistJson) {
        var mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try {
            return mapper.readValue(gistJson, Gist.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Received gist could not be parsed: " + gistJson +
                    ". Original error: " + e.getMessage());
        }
    }

    void addHeaders(HttpUriRequestBase request) {
        request.addHeader("Accept", "application/vnd.github.v3+json");

        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader("X-GitHub-Api-Version", "2022-11-28");
        request.addHeader("Content-Type", "application/json");
    }

    @Step("Check that gist is deleted by id")
    void checkGistDeletedById(String id) {
        var getRequest = new HttpGet(baseUrl + "/" + id);
        addHeaders(getRequest);
        Awaitility.await().pollDelay(1, TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).until(
                () -> {
                    var responseJson = clientBase.get(getRequest).getPayload();
                    return responseJson.contains("Not Found");
                });
    }

    @Step("Create a gist")
    String sendCreateGist(String privateBody, String description, String fileName, String content) throws IOException, ParseException {
        var postRequest = new HttpPost(baseUrl);
        var entity = new StringEntity(String.format(privateBody, description, fileName, content));
        postRequest.setEntity(entity);
        addHeaders(postRequest);
        return clientBase.executeRequestWithEntity(postRequest).getPayload();
    }

    @Step("Check gist default properties")
    void checkGistDefaultState(String fileName, String content, String description, Gist gist) {
        var file = gist.getFiles().get(fileName);
        if (Objects.nonNull(file)) {
            assertEquals(file.getFilename(), fileName, "Filename is not as expected");
            assertEquals(file.getContent(), content, "File content is not as expected");
            assertFalse(file.isTruncated(), "File is not truncated");
        }

        assertEquals(description, gist.getDescription(), "Gist description is not as expected");
        assertFalse(gist.isPublic(), "Gist is public");
        assertFalse(gist.isTruncated(), "Gist is not truncated");
    }

    @Step("Delete gist {gistId}")
    int deleteGist(String gistId) throws IOException {
        var deleteRequest = new HttpDelete(baseUrl + "/" + gistId);
        addHeaders(deleteRequest);
        return clientBase.delete(deleteRequest);
    }

    @Step("Request gist {gistId}")
    String requestGist(String gistId, boolean isRequestAnonymous) throws IOException, ParseException {
        var getRequest = new HttpGet(baseUrl + "/" + gistId);
        if (!isRequestAnonymous) {
            addHeaders(getRequest);
        }
        return clientBase.get(getRequest).getPayload();
    }

    @Step("Get current user's gists")
    String getUsersGists(Map<String, String> requestParams) throws IOException, ParseException {
        var url = baseUrl.replace("/gists", "")
                + "/users/" + owner + "/gists";
        var gistsPerPage = requestParams.get("per_page");
        var page = requestParams.get("page");
        var since = requestParams.get("since");
        if (Objects.nonNull(page)) {
            url = url + "?page=" + page;
        }
        //hardcoded options here, 'per_page' and 'since' should be provided only if 'page' is provided
        if (Objects.nonNull(gistsPerPage)) {
            url = url + "&per_page=" + gistsPerPage;
        }
        if (Objects.nonNull(since)) {
            url = url + "&since=" + since;
        }
        var getRequest = new HttpGet(url);
        addHeaders(getRequest);
        return clientBase.get(getRequest).getPayload();
    }

    @Step("Update gist")
    String updateGist(String gistId, String privateBody, String description, String fileName, String content) throws IOException, ParseException {
        var patchRequest = new HttpPatch(baseUrl + "/" + gistId);
        addHeaders(patchRequest);
        var entity = new StringEntity(String.format(privateBody, description, fileName, content));
        patchRequest.setEntity(entity);
        return clientBase.executeRequestWithEntity(patchRequest).getPayload();
    }
}
