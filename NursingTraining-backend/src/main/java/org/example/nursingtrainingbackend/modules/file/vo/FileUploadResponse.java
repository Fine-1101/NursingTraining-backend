package org.example.nursingtrainingbackend.modules.file.vo;

public record FileUploadResponse(String objectKey, String url, String originalFileName,
                                 long size, String contentType) {}
