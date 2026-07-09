package org.example.nursingtrainingbackend.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 绑定yml中前缀为 app.oss 的配置
@ConfigurationProperties(prefix = "app.oss")
public class OssConfig {
    // 和yml配置一一对应
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String baseDirectory;
    private String policyExpiration;

    @Bean
    public OSS ossClient () {
        // 仅打印地址、桶名，不打印密钥
        System.out.println ("=== OSS 配置加载完成 ===");
        System.out.println ("endpoint:" + endpoint);
        System.out.println ("bucketName:" + bucketName);
        // 密钥不再打印，避免日志泄露
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    // getter & setter 必须齐全，否则配置无法注入
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getPolicyExpiration() {
        return policyExpiration;
    }

    public void setPolicyExpiration(String policyExpiration) {
        this.policyExpiration = policyExpiration;
    }
}