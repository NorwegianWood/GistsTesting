package com.github.rest;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.classic.methods.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import java.util.logging.Logger;

public class ClientBase {
    private static final Logger logger = Logger.getLogger(ClientBase.class.getName());
    private final CloseableHttpClient client = HttpClients.createDefault();

    public static class HttpResponseReader {
        private final CloseableHttpResponse response;
        private String payload;

        HttpResponseReader(CloseableHttpResponse response) {
            this.response = response;
        }

        public String getPayload() throws IOException, ParseException {
            if (Objects.isNull(payload)) {
                payload = EntityUtils.toString(response.getEntity());
            }
            return payload;
        }
    }

    private void logRequest(ClassicHttpRequest request) {
        logger.info("Request: " + request.toString());
        logger.info("Headers: " + Arrays.toString(request.getHeaders()));
    }

    private HttpResponseReader execute(ClassicHttpRequest request) throws IOException {
        var response = client.execute(request);
        return new HttpResponseReader(response);
    }

    public HttpResponseReader executeRequestWithEntity(ClassicHttpRequest httpRequest) throws IOException {
        logRequest(httpRequest);
        return execute(httpRequest);
    }

    public int delete(HttpDelete httpDelete) throws IOException {
        logRequest(httpDelete);
        var response = client.execute(httpDelete);
        EntityUtils.consumeQuietly(response.getEntity());
        return response.getCode();
    }

    public HttpResponseReader get(HttpGet httpGet) throws IOException {
        logRequest(httpGet);
        return execute(httpGet);
    }
}
