package com.example.starter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "starter.auth.admin-password=test-admin", "starter.auth.analyst-password=test-viewer"
})
class AccountAccessTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> getAccount(String credentials) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/admin/analyst"));
        if (credentials != null) {
            var token = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + token);
        }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void anonymousRequestRequiresAuthentication() throws Exception {
        assertEquals(401, getAccount(null).statusCode());
    }

    @Test void viewerCannotReadAdminEndpoint() throws Exception {
        assertEquals(403, getAccount("analyst:test-viewer").statusCode());
    }

    @Test void adminCanReadTheAccount() throws Exception {
        var response = getAccount("admin:test-admin");
        assertEquals(200, response.statusCode());
        var account = json.readTree(response.body());
        assertEquals("analyst", account.path("username").asString());
        assertEquals("VIEWER", account.path("role").asString());
    }
}
