package com.github.test;

import org.apache.hc.core5.http.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <a href="https://docs.github.com/en/rest/gists/gists?apiVersion=2022-11-28#update-a-gist">...</a>
 */
@Tag("integration")
public class UpdateOperationTest extends BaseTest {
    /*
    To delete a file, set the whole file to null. For example: hello.py : null
    */
    @Test
    @DisplayName("Delete file by update")
    public void deleteFileByUpdate() throws IOException, ParseException {
        var responseJson = sendCreateGist(body, description, "test2", "test");
        var gist = getGistFromResponse(responseJson);
        responseJson = updateGist(gist.getId(), "{\"description\": \"%s\", \"files\": {\"%s\": null}", description, "test2", null);
        gist = getGistFromResponse(responseJson);
        checkGistDefaultState(fileName, content, description, gist);
        assertEquals(gist.getOwner().getLogin(), owner, "Gist owner is not as expected");
    }

    private static Stream<Arguments> params() {
        return Stream.of(
                //description is missing but files present
                Arguments.of(true, "{\"\": \"%s\", \"files\": {\"%s\": {\"content\": \"%s\"}}", "https://api.github.com/users/"),
                //files is missing but description present
                Arguments.of(true, "{\"description\": \"%s\", \"\": {\"%s\": {\"content\": \"%s\"}}", "https://api.github.com/users/"),
                //gist id is missing
                Arguments.of(false, "", "Not Found"));
    }

    @DisplayName("Check required update params")
    @ParameterizedTest
    @MethodSource("params")
    public void checkRequiredParams(boolean originalGistId, String newBody, String expectedResponse) throws IOException, ParseException {
        var responseJson = sendCreateGist(body, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        var id = originalGistId ? gist.getId() : "";
        responseJson = updateGist(id, newBody, null, fileName, content);
        assertThat("Response message is incorrect", responseJson, containsString(expectedResponse));
    }
}
