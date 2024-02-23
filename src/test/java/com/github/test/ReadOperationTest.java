package com.github.test;

import org.apache.hc.core5.http.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <a href="https://docs.github.com/en/rest/gists/gists?apiVersion=2022-11-28#get-a-gist">...</a>
 */
@Tag("integration")
public class ReadOperationTest extends BaseTest {

    /**
     * <a href="https://docs.github.com/en/rest/gists/gists?apiVersion=2022-11-28#list-gists-for-the-authenticated-user">...</a>
     */
    @DisplayName("Read current user's gists")
    @Test
    public void readUsersGistsTest() throws IOException, ParseException {
        //create two gists
        var responseJson = sendCreateGist(body, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        var firstGistId = gist.getId();

        responseJson = sendCreateGist(body, description, fileName, content);
        gist = getGistFromResponse(responseJson);
        var secondGistId = gist.getId();

        responseJson = getUsersGists(Map.of());
        assertThat("First created gist is not found", responseJson, containsString(firstGistId));
        assertThat("Second created gist is not found", responseJson, containsString(secondGistId));
    }

    /**
     * <a href="https://docs.github.com/en/rest/gists/gists?apiVersion=2022-11-28#list-gists-for-the-authenticated-user">...</a>
     */
    @DisplayName("Read current user's gists with pagination")
    @Test
    public void readUsersGistsTestWithPagination() throws IOException, ParseException, InterruptedException {
        //create 30 gists
        var fullAmount = 30;
        var timestamp = LocalTime.now().toString();
        for (var i = 1; i < fullAmount; i++) {
            var gistResponse = sendCreateGist(body, description, fileName, content);
            if (i == 15) {
                Thread.sleep(100);
                //remember timestamp
                gistResponse = sendCreateGist(body, description, fileName, content);
                var middleGist = getGistFromResponse(gistResponse);
                timestamp = middleGist.getCreatedAt();
            }
        }

        //check 30 is default
        var response = getUsersGists(Map.of());
        var allGists = parseGists(response);
        assertEquals(30, allGists.size(), "30 gists per page is not default");
        //check 30 gists were created
        response = getUsersGists(Map.of("per_page", "100", "page", "1"));
        allGists = parseGists(response);
        assertEquals(fullAmount, allGists.size(), "Not 30 gists were created");
        //make request on 2nd page, check 0 gists
        response = getUsersGists(Map.of("page", "2"));
        allGists = parseGists(response);
        assertEquals(0, allGists.size(), "Second page has gists");
        //check setting 10 per page + pagination
        response = getUsersGists(Map.of("per_page", "10", "page", "2"));
        allGists = parseGists(response);
        assertEquals(10, allGists.size(), "Second page has unexpected number of gists");

        //get timestamp of the middle gist, check result has half gists
        response = getUsersGists(Map.of("page", "1", "since", timestamp));
        allGists = parseGists(response);

        assertThat("Filtering by timestamp returned unexpected number of gists", ((double) allGists.size()), closeTo(((double) fullAmount / 2), 2.0));
    }

    /**
     * <a href="https://docs.github.com/en/rest/gists/gists?apiVersion=2022-11-28#authentication">...</a>
     */
    @DisplayName("Read a public gist anonymously")
    @Test
    public void readPublicGistAnonymously() throws IOException, ParseException {
        var publicBody = "{\"description\": \"%s\",\"public\": \"true\", \"files\": {\"%s\": {\"content\": \"%s\"}}";
        var responseJson = sendCreateGist(publicBody, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        var gistId = gist.getId();
        responseJson = requestGist(gistId, true);
        assertThat("Created gist is not found", responseJson, containsString(gistId));
    }

    /**
     * <a href="https://docs.github.com/en/get-started/writing-on-github/editing-and-sharing-content-with-gists/creating-gists">...</a>
     * Secret gists aren't private. If you send the URL of a secret gist to a friend, they'll be able to see it.
     */
    @DisplayName("Read a private gist anonymously")
    @Test
    public void readPrivateGistAnonymously() throws IOException, ParseException {
        var publicBody = "{\"description\": \"%s\",\"public\": \"false\", \"files\": {\"%s\": {\"content\": \"%s\"}}";
        var responseJson = sendCreateGist(publicBody, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        var gistId = gist.getId();
        responseJson = requestGist(gistId, true);
        assertThat("Created gist is not found", responseJson, containsString(gistId));
    }

    @DisplayName("Read a not existing gist")
    @Test
    public void readNotExistingGistTest() throws IOException, ParseException {
        var responseJson = requestGist("test", false);
        assertThat("Not existing gist was found", responseJson, containsString("Not Found"));
    }

    @DisplayName("Read a gist without mandatory parameter")
    @Test
    public void readGistWithoutMandatoryIdTest() throws IOException, ParseException {
        var responseJson = requestGist("", false);
        assertThat("Not existing gist was found", responseJson, containsString("Not Found"));
    }
}
