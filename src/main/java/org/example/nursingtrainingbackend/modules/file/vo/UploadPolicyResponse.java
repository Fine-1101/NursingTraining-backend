package org.example.nursingtrainingbackend.modules.file.vo;

public record UploadPolicyResponse(String host, String key, String policy, String signature,
                                   String accessKeyId, String contentType, long expireAt) {}
