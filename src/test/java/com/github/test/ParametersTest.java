package com.github.test;

import org.apache.hc.core5.http.ParseException;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParametersTest extends BaseTest {
    private final String body = "{\"description\": \"%s\", \"files\": {\"%s\": {\"content\": \"%s\"}}";

    private static Stream<Arguments> errors() {
        var tooLongDescMessage = "\"field\":\"description\",\"message\":\"description is too long (maximum is 256 characters)\"}";
        var validationFailedMessage = "Validation Failed";
        var problemsParsingJsonMessage = "Problems parsing JSON";
        return Stream.of(
                Arguments.of(getStringWithLength(1025), getStringWithLength(1025), getStringWithLength(1025), tooLongDescMessage),
                Arguments.of("test", getStringWithLength(1025), getStringWithLength(1025), tooLongDescMessage),
                Arguments.of(getStringWithLength(1025), "test", getStringWithLength(1025), tooLongDescMessage),
                Arguments.of("   ", "  ", "  ", "\"resource\":\"Gist\",\"code\":\"missing_field\",\"field\":\"files\""),
                Arguments.of("Óèêèïåäèÿ", "Óèêèïåäèÿ", "Óèêèïåäèÿ", problemsParsingJsonMessage),
                Arguments.of("***", "№%:,.", ")(;.,+_(<<>±§`", problemsParsingJsonMessage),
                Arguments.of("<form action='h><input type='submit'></form>", "test", "<form action='h><input type='submit'></form>", validationFailedMessage),
                Arguments.of("<script>document.getElementByID('...').disabled=true</script>", "test",
                        "<script>document.getElementByID('...').disabled=true</script>", validationFailedMessage),
                Arguments.of(getStringWithLength(131072), "test", "test", ""),
                Arguments.of("U+2029\nsdgf", "test", "U+2029\nsdgf", problemsParsingJsonMessage)
        );
    }

    @DisplayName("Check errors on creation")
    @ParameterizedTest
    @MethodSource("errors")
    public void checkInputsExpectErrorsCreate(String fileName, String content, String description, String expectedMessage) throws IOException, ParseException {
        var responseJson = sendCreateGist(body, description, fileName, content);
        assertThat("Expected message is incorrect after gist creation", responseJson, containsString(expectedMessage));
    }

    private static Stream<Arguments> success() {
        return Stream.of(
                Arguments.of("Текст", "ἱερογλύφος", "<, >, !%:,."),
                Arguments.of("♣ ☺ ♂", "-", "-"),
                Arguments.of("SELECT * FROM gists WHERE gist LIKE 'a%';", "test", "SELECT * FROM gists WHERE gist LIKE 'a%'"),
                Arguments.of("<!--", "<!--", "--!>"));
    }

    @DisplayName("Check successful creation")
    @ParameterizedTest
    @MethodSource("success")
    public void checkInputsExpectSuccessCreate(String fileName, String content, String description) throws IOException, ParseException {
        var responseJson = sendCreateGist(body, description, fileName, content);
        var gist = getGistFromResponse(responseJson);
        checkGistDefaultState(fileName, content, description, gist);
        assertEquals(gist.getOwner().getLogin(), owner, "Gist owner is not as expected");
    }

    @DisplayName("Check successful update")
    @ParameterizedTest
    @MethodSource("success")
    public void checkInputsExpectSuccessUpdate(String fileName, String content, String description) throws IOException, ParseException {
        var responseJson = sendCreateGist(body, "test", "test", "test");
        var gist = getGistFromResponse(responseJson);
        responseJson = updateGist(gist.getId(), body, description, fileName, content);
        gist = getGistFromResponse(responseJson);
        checkGistDefaultState(fileName, content, description, gist);
        assertEquals(gist.getOwner().getLogin(), owner, "Gist owner is not as expected");
    }

    @DisplayName("Check errors on update")
    @ParameterizedTest
    @MethodSource("errors")
    public void checkInputsExpectErrorsUpdate(String fileName, String content, String description, String expectedMessage) throws IOException, ParseException {
        var responseJson = sendCreateGist(body, "test", "test", "test");
        var gist = getGistFromResponse(responseJson);

        responseJson = updateGist(gist.getId(), body, description, fileName, content);
        assertThat("Expected message is incorrect after gist update", responseJson, containsString(expectedMessage));
    }

    private static String getStringWithLength(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'f');
        return new String(chars);
    }
}
