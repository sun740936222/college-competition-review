package com.zhk.contest.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageConfig {
    private String uploadDir = "./uploads";
    private Map<String, Long> maxSizes;

    public long getMaxSize(String materialType) {
        if (maxSizes == null) return 50 * 1024 * 1024;
        return maxSizes.getOrDefault(materialType, 50 * 1024 * 1024L);
    }
}
