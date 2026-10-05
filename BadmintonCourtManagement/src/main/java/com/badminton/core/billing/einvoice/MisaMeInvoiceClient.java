package com.badminton.core.billing.einvoice;

import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MISA meInvoice integration API client.
 *
 * Flow: password-grant token → publish invoice → poll status → download/send.
 * Endpoint paths are configurable ({@code einvoice.misa.*-path}) because MISA
 * environments differ between test and production tenants.
 *
 * Never logs credentials or response payloads — they may carry invoice data.
 */
@Slf4j
@Component
public class MisaMeInvoiceClient {

    @Autowired
    private EInvoiceProperties properties;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Cached token — expiresIn counts from issue time.
    private String cachedToken;
    private Instant tokenExpiry = Instant.MIN;

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    private synchronized String token() throws BusinessException {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(30))) {
            return cachedToken;
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("appid", properties.getAppId());
        form.put("taxcode", properties.getTaxCode());
        form.put("username", properties.getUsername());
        form.put("password", properties.getPassword());
        String body = form.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl() + properties.getTokenPath()))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        JsonObject json = sendForJson(request);
        String accessToken = json.has("access_token") ? json.get("access_token").getAsString() : null;
        if (accessToken == null) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    "MISA token request was rejected — check einvoice.misa credentials");
        }
        long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 1800;
        cachedToken = accessToken;
        tokenExpiry = Instant.now().plusSeconds(expiresIn);
        return accessToken;
    }

    /** POST {baseUrl}{publishPath} with the built invoice payload. */
    public JsonObject publish(Map<String, Object> payload) throws BusinessException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl() + properties.getPublishPath()))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token())
                .POST(HttpRequest.BodyPublishers.ofString(new com.google.gson.Gson().toJson(payload)))
                .build();
        return sendForJson(request);
    }

    /** GET {baseUrl}{statusPath}?transactionID=&invoiceNo= */
    public JsonObject status(String transactionId, String invoiceNo) throws BusinessException {
        StringBuilder url = new StringBuilder(properties.getBaseUrl() + properties.getStatusPath());
        url.append("?transactionID=").append(URLEncoder.encode(transactionId, StandardCharsets.UTF_8));
        if (invoiceNo != null && !invoiceNo.isBlank()) {
            url.append("&invoiceNo=").append(URLEncoder.encode(invoiceNo, StandardCharsets.UTF_8));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + token())
                .GET()
                .build();
        return sendForJson(request);
    }

    /** GET {baseUrl}{downloadPath}?invoiceNo= — binary PDF. */
    public byte[] download(String invoiceNo) throws BusinessException {
        String url = properties.getBaseUrl() + properties.getDownloadPath()
                + "?invoiceNo=" + URLEncoder.encode(invoiceNo, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + token())
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2 || response.body() == null || response.body().length == 0) {
                throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                        "MISA download failed (HTTP " + response.statusCode() + ")");
            }
            return response.body();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap("download", e);
        }
    }

    private JsonObject sendForJson(HttpRequest request) throws BusinessException {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() / 100 != 2) {
                // Log status only — the body can carry provider details but no
                // secrets; still keep it out of logs beyond a short excerpt.
                String excerpt = body != null && body.length() > 200 ? body.substring(0, 200) : body;
                log.error("MISA {} {} -> HTTP {}: {}", request.method(), request.uri().getPath(),
                        response.statusCode(), excerpt);
                throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                        "MISA request failed (HTTP " + response.statusCode() + ")");
            }
            JsonObject json = JsonParser.parseString(body != null ? body : "{}").getAsJsonObject();
            return json;
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw wrap(request.uri().getPath(), e);
        } catch (Exception e) {
            throw wrap(request.uri().getPath(), e);
        }
    }

    private BusinessException wrap(String op, Exception e) {
        log.error("MISA {} call failed: {}", op, e.getMessage());
        return new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                "MISA request failed: " + e.getMessage());
    }
}
