package com.github.test;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.core5.http.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainOperationsTest extends BaseTest {

    @DisplayName("CRUD critical path")
    @Test
    public void mainFlowTest() throws IOException, ParseException {
        //create
        var responseJson = sendCreateGist(body, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        assertEquals(gist.getOwner().getLogin(), owner);
        var initialId = gist.getId();

        //read
        responseJson = requestGist(initialId, false);
        gist = getGistFromResponse(responseJson);
        checkGistDefaultState(fileName, content, description, gist);
        assertEquals(gist.getId(), initialId, "Gist id is not as expected");
        assertEquals(gist.getOwner().getLogin(), owner, "Gist owner name is not as expected");

        //update
        var updatedDescription = description + "1";
        var updatedContent = content + "1";
        var updatedFileName = fileName + "1";
        var updatedGist = updateGist(initialId, body, updatedDescription, updatedFileName, updatedContent);
        gist = getGistFromResponse(updatedGist);
        checkGistDefaultState(updatedFileName, updatedContent, updatedDescription, gist);
        assertEquals(gist.getId(), initialId, "Gist id is not as expected after update");
        assertEquals(gist.getOwner().getLogin(), owner, "Gist owner name is not as expected after update");

        //delete
        var status = deleteGist(initialId);
        assertEquals(204, status, "Status is not as expected after gist deletion");
        checkGistDeletedById(initialId);
    }

    @DisplayName("Delete a not existing gist")
    @Test
    public void deleteNotExistingGistTest() throws IOException {
        var status = deleteGist("test");
        assertEquals(404, status, "Deletion of a not existing gist did not return an error");
    }

    @DisplayName("Delete gist anonymously")
    @Test
    public void deleteAnonymous() throws IOException, ParseException {
        var responseJson = sendCreateGist(body, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        var deleteRequest = new HttpDelete(baseUrl + "/" + gist.getId());
        var status = clientBase.delete(deleteRequest);
        assertEquals(404, status);
    }
}
