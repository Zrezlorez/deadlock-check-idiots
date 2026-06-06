package com.litovskiy.config.cloud;

import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class GithubConfigPropertySource extends MapPropertySource {

    public static final String NAME = "github-cloud-config";

    public GithubConfigPropertySource(Map<String, Object> data) {
        super(NAME, data);
    }
}
