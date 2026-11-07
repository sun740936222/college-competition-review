package com.zhk.contest.training.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.service.FileStorageService;
import com.zhk.contest.training.entity.SrtpAttachment;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpAttachmentMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** SRTP 申报附件：上传/列表/下载/删除。存储于 uploads/srtp/{stage}/{targetId}/。 */
@RestController
@RequestMapping("/api/srtp/attachments")
public class SrtpAttachmentController {

    private final SrtpAttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;

    public SrtpAttachmentController(SrtpAttachmentMapper attachmentMapper, FileStorageService fileStorageService) {
        this.attachmentMapper = attachmentMapper;
        this.fileStorageService = fileStorageService;
    }

    private SrtpStage parseStage(String stage) {
        try {
            return SrtpStage.valueOf(stage.toUpperCase());
        } catch (Exception e) {
            throw new BusinessException("阶段参数错误");
        }
    }

    @PostMapping("/{stage}/{targetId}")
    public ApiResponse<SrtpAttachment> upload(@PathVariable String stage, @PathVariable Long targetId,
                                              @RequestParam("file") MultipartFile file, HttpServletRequest req) {
        if (req.getAttribute("userId") == null) throw new BusinessException(401, "请先登录");
        if (file == null || file.isEmpty()) throw new BusinessException("文件不能为空");
        SrtpStage s = parseStage(stage);
        String subPath = "srtp/" + s.name().toLowerCase() + "/" + targetId;
        String storedPath = fileStorageService.store(subPath, file);

        SrtpAttachment a = new SrtpAttachment();
        a.setStage(s);
        a.setTargetId(targetId);
        a.setOriginalFilename(file.getOriginalFilename());
        a.setStoredFilename(storedPath.substring(storedPath.lastIndexOf('/') + 1));
        a.setFilePath(storedPath);
        a.setFileSize(file.getSize());
        attachmentMapper.insert(a);
        return ApiResponse.success("上传成功", a);
    }

    @GetMapping("/{stage}/{targetId}")
    public ApiResponse<List<SrtpAttachment>> list(@PathVariable String stage, @PathVariable Long targetId) {
        SrtpStage s = parseStage(stage);
        return ApiResponse.success(attachmentMapper.selectList(new QueryWrapper<SrtpAttachment>().lambda()
                .eq(SrtpAttachment::getStage, s)
                .eq(SrtpAttachment::getTargetId, targetId)
                .orderByDesc(SrtpAttachment::getCreatedAt)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        SrtpAttachment a = attachmentMapper.selectById(id);
        if (a == null) throw new BusinessException("附件不存在");
        Resource resource = fileStorageService.loadAsResource(a.getFilePath());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(a.getOriginalFilename() != null ? a.getOriginalFilename() : "file", StandardCharsets.UTF_8)
                        .build().toString())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        if (req.getAttribute("userId") == null) throw new BusinessException(401, "请先登录");
        SrtpAttachment a = attachmentMapper.selectById(id);
        if (a != null) {
            fileStorageService.delete(a.getFilePath());
            attachmentMapper.deleteById(id);
        }
        return ApiResponse.success("已删除", null);
    }
}
