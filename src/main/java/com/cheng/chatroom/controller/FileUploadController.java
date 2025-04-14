package com.cheng.chatroom.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-path}")
    private String accessPath;

    /**
     * 文件上传接口
     * @param file 上传的文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public UploadResult uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new UploadResult(false, "文件为空");
        }

        try {
            // 确保上传目录存在
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // 保存文件
            Path path = Paths.get(uploadDir + uniqueFileName);
            Files.write(path, file.getBytes());

            // 返回访问URL
            String fileUrl = accessPath + uniqueFileName;
            return new UploadResult(true, "上传成功", fileUrl, originalFilename, file.getSize());

        } catch (IOException e) {
            e.printStackTrace();
            return new UploadResult(false, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传结果封装类
     */
    private static class UploadResult {
        private boolean success;
        private String message;
        private String fileUrl;
        private String fileName;
        private long fileSize;

        public UploadResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public UploadResult(boolean success, String message, String fileUrl, String fileName, long fileSize) {
            this.success = success;
            this.message = message;
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.fileSize = fileSize;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public long getFileSize() {
            return fileSize;
        }
    }
}