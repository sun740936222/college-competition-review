package com.zhk.contest.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(Long projectId, MultipartFile file);
    String store(String subPath, MultipartFile file);
    Resource loadAsResource(String storedFilename);
    void delete(String filePath);
}
