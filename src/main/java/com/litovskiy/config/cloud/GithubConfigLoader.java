package com.litovskiy.config.cloud;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GithubConfigLoader {

    private static final String API_TEMPLATE = "https://api.github.com/repos/%s/%s/contents/%s?ref=%s";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private GithubConfigLoader() {
    }

    public static Map<String, Object> load(GithubConfigProperties props) throws IOException {
        byte[] body = fetch(props);
        return parseYaml(body);
    }

    private static byte[] fetch(GithubConfigProperties props) throws IOException {
        URI uri = URI.create(String.format(
            API_TEMPLATE,
            props.owner(),
            props.repo(),
            props.path(),
            props.ref()
        ));

        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(HTTP_TIMEOUT)
            .header("Accept", "application/vnd.github.raw")
            .header("Authorization", "Bearer " + props.token())
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET()
            .build();

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build()) {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("GitHub returned HTTP " + response.statusCode() + " for " + props.describe());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching GitHub config", e);
        }
    }

    private static Map<String, Object> parseYaml(byte[] body) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(body) {
            @Override
            public String getFilename() {
                return "cloud-config.yml";
            }
        };

        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("cloud-config", resource);
        Map<String, Object> merged = new LinkedHashMap<>();
        for (PropertySource<?> source : sources) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    merged.put(name, enumerable.getProperty(name));
                }
            }
        }
        return merged;
    }
}
