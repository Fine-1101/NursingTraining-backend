package org.example.nursingtrainingbackend.common;

import org.example.nursingtrainingbackend.common.utils.FileNameUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FileNameUtilsTests {
    @Test
    void objectKeyRemovesPathTraversalAndKeepsSafeExtension() {
        String key = FileNameUtils.objectKey("nursing-training", "../course//images", "../../病例.PnG");
        assertThat(key).startsWith("nursing-training/course/images/").endsWith(".png").doesNotContain("..");
    }

    @Test
    void extensionRejectsSuspiciousCharacters() {
        assertThat(FileNameUtils.extension("report.pdf.exe$cmd")).isEqualTo(".execmd");
        assertThat(FileNameUtils.extension("no-extension")).isEmpty();
    }
}
