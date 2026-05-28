package com.litovskiy.config.cloud;

import org.springframework.core.env.Environment;

public record GithubConfigProperties(
    boolean enabled,
    String owner,
    String repo,
    String path,
    String ref,
    String token
) {

    private static final String PREFIX = "cloud-config.github.";

    public static GithubConfigProperties from(Environment env) {
        return new GithubConfigProperties(
            env.getProperty("cloud-config.enabled", Boolean.class, false),
            env.getProperty(PREFIX + "owner", ""),
            env.getProperty(PREFIX + "repo", ""),
            env.getProperty(PREFIX + "path", ""),
            env.getProperty(PREFIX + "ref", "main"),
            env.getProperty(PREFIX + "token", "")
        );
    }

    public boolean isComplete() {
        return enabled
            && !owner.isBlank()
            && !repo.isBlank()
            && !path.isBlank()
            && !token.isBlank();
    }

    public String describe() {
        return owner + "/" + repo + ":" + path + "@" + ref;
    }
}
