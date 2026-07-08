package org.example.nursingtrainingbackend.modules.file;

import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.OssProperties;
import org.example.nursingtrainingbackend.config.properties.UploadProperties;
import org.example.nursingtrainingbackend.modules.file.dto.UploadPolicyRequest;
import org.example.nursingtrainingbackend.modules.file.service.impl.OssFileServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.time.Duration;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class OssFileServiceTests {
    private final UploadProperties upload = new UploadProperties(10, Set.of("image/png"));
    private final OssProperties missing = new OssProperties("", "", "", "", "", "nursing", Duration.ofMinutes(10));

    @Test
    void rejectsEmptyFileBeforeContactingOss() {
        var service = new OssFileServiceImpl(missing, upload);
        var file = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);
        assertCode(() -> service.upload(file, "images"), ErrorCode.FILE_EMPTY);
    }

    @Test
    void rejectsOversizedAndUnsupportedFiles() {
        var service = new OssFileServiceImpl(missing, upload);
        assertCode(() -> service.upload(new MockMultipartFile("file", "x.png", "image/png", new byte[11]), "images"), ErrorCode.FILE_TOO_LARGE);
        assertCode(() -> service.upload(new MockMultipartFile("file", "x.exe", "application/octet-stream", new byte[1]), "files"), ErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void reportsMissingOssConfigurationWithoutLeakingSecrets() {
        var service = new OssFileServiceImpl(missing, upload);
        assertCode(() -> service.createPolicy(new UploadPolicyRequest("x.png", "image/png", "images")), ErrorCode.OSS_NOT_CONFIGURED);
    }

    private void assertCode(ThrowingCallable callable, ErrorCode code) {
        assertThatThrownBy(callable::call).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }

    @FunctionalInterface interface ThrowingCallable { void call(); }
}
