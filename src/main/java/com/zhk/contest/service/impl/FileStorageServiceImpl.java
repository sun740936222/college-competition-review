package com.zhk.contest.service.impl;

import com.zhk.contest.config.FileStorageConfig;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path uploadRoot;

    public FileStorageServiceImpl(FileStorageConfig config) {
        this.uploadRoot = Paths.get(config.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录", e);
        }
    }

    @Override
    public String store(Long projectId, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + extension;
        Path projectDir = this.uploadRoot.resolve("projects").resolve(projectId.toString());
        Path targetPath = projectDir.resolve(storedName);

        try {
            Files.createDirectories(projectDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "projects/" + projectId + "/" + storedName;
        } catch (IOException e) {
            throw new BusinessException("文件存储失败: " + e.getMessage());
        }
    }

    @Override
    public String store(String subPath, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + extension;
        Path dir = this.uploadRoot.resolve(subPath);
        Path targetPath = dir.resolve(storedName);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return subPath + "/" + storedName;
        } catch (IOException e) {
            throw new BusinessException("文件存储失败: " + e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String storedFilename) {
        try {
            Path filePath = this.uploadRoot.resolve(storedFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            // Verify both existence and ability to open the stream before returning,
            // so that streaming failures don't happen after the response Content-Type is committed.
            if (resource.exists() && resource.isReadable()) {
                try (InputStream is = resource.getInputStream()) {
                    // stream opened successfully, resource is valid
                }
                return resource;
            }
            throw new BusinessException("文件不存在或不可读");
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("文件不可读: " + e.getMessage());
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path target = this.uploadRoot.resolve(filePath).normalize();
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Log but don't throw - file may already be deleted
        }
    }
}
