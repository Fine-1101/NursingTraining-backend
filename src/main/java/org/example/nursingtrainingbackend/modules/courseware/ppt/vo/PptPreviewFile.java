package org.example.nursingtrainingbackend.modules.courseware.ppt.vo;

import java.io.IOException;
import java.io.InputStream;

public record PptPreviewFile(InputStream inputStream, long contentLength) implements AutoCloseable {
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
