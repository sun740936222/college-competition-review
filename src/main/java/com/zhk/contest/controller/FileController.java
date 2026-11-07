package com.zhk.contest.controller;

import com.zhk.contest.entity.ProjectMaterial;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.ProjectMaterialMapper;
import com.zhk.contest.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final ProjectMaterialMapper materialMapper;

    public FileController(FileStorageService fileStorageService, ProjectMaterialMapper materialMapper) {
        this.fileStorageService = fileStorageService;
        this.materialMapper = materialMapper;
    }

    @GetMapping("/{storedFilename}")
    public ResponseEntity<Resource> serve(@PathVariable String storedFilename) {
        ProjectMaterial material = materialMapper.findByStoredFilename(storedFilename)
                .orElseThrow(() -> new BusinessException("文件不存在"));

        Resource resource = fileStorageService.loadAsResource(material.getFilePath());

        MediaType mediaType;
        if (storedFilename.toLowerCase().endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
        } else if (storedFilename.toLowerCase().endsWith(".mp4")) {
            mediaType = MediaType.valueOf("video/mp4");
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }
}
