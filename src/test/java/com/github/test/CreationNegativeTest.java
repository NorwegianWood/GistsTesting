package com.github.test;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag("integration")
public class CreationNegativeTest extends BaseTest {

    @DisplayName("Create a gist without required fields")
    @Test
    public void checkWithoutRequiredFields() throws IOException, ParseException {
        var error = "{\"message\":\"Validation Failed\",\"errors\":[{\"resource\":\"Gist\",\"code\":\"missing_field\",\"field\":\"files\"}";

        var errorJson = sendCreateGist(body, description, fileName, "");
        assertThat("Gist creation without content didn't return expected error", errorJson, containsString(error));

        errorJson = sendCreateGist(body, description, "", "");
        assertThat("Gist creation without file didn't return expected error", errorJson, containsString(error));

        errorJson = sendCreateGist("{}", null, null, null);
        assertThat("Response message is incorrect", errorJson, containsString("Invalid input: object is missing required key: files."));
    }

    @DisplayName("Create a gist without token")
    @Test
    public void checkCreationWithoutToken() throws IOException, ParseException {
        var entity = new StringEntity(String.format(body, description, fileName, content));
        var postRequest = new HttpPost(baseUrl);
        postRequest.setEntity(entity);
        var errorJson = clientBase.executeRequestWithEntity(postRequest).getPayload();
        assertThat("Gist creation without token didn't return expected error", errorJson, containsString("\"message\":\"Requires authentication\""));
    }
}
