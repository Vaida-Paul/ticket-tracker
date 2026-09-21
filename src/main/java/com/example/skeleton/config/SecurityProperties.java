package com.example.skeleton.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> pathsToSkip = List.of();
    private Map<String, List<String>> rolePaths = Map.of();
    private boolean devMode = false;
}
