package org.example.nursingtrainingbackend.modules.courseware.ppt.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.example.nursingtrainingbackend.config.properties.OssProperties;
import org.example.nursingtrainingbackend.config.properties.PptConversionProperties;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.mapper.PptMapper;
import org.example.nursingtrainingbackend.modules.courseware.ppt.service.PptConversionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PptConversionServiceImpl implements PptConversionService {

    private static final Set<Long> CONVERTING_IDS = ConcurrentHashMap.newKeySet();

    private final ObjectProvider<OSS> ossClientProvider;
    private final OssProperties ossProperties;
    private final PptConversionProperties conversionProperties;
    private final PptMapper pptMapper;
    /** 异步将PPT转换为PDF，并回写预览地址和页数。 */

    @Override
    @Async("pptConversionExecutor")
    public void convertAsync(Long pptId, String originalUrl, String originalName) {
        if (!conversionProperties.isEnabled()) {
            log.info("PPT conversion is disabled, pptId={}", pptId);
            return;
        }
        if (!CONVERTING_IDS.add(pptId)) {
            log.info("PPT conversion is already running, pptId={}", pptId);
            return;
        }

        OSS ossClient = ossClientProvider.getIfAvailable();
        if (ossClient == null || !ossProperties.configured()) {
            log.error("PPT conversion skipped because OSS is not configured, pptId={}", pptId);
            CONVERTING_IDS.remove(pptId);
            return;
        }

        Path workDirectory = null;
        try {
            workDirectory = Files.createTempDirectory("ppt-conversion-" + pptId + "-");
            Path inputDirectory = Files.createDirectories(workDirectory.resolve("input"));
            Path outputDirectory = Files.createDirectories(workDirectory.resolve("output"));
            Path profileDirectory = Files.createDirectories(workDirectory.resolve("lo-profile"));
            Path inputFile = inputDirectory.resolve(safeInputFileName(originalName));

            downloadOriginal(ossClient, originalUrl, inputFile);
            runLibreOffice(inputFile, outputDirectory, profileDirectory);

            Path pdfFile = findGeneratedPdf(outputDirectory);
            int pageCount = readPageCount(pdfFile);
            String objectKey = buildPreviewObjectKey(pptId);
            uploadPdf(ossClient, objectKey, pdfFile);

            Ppt update = new Ppt();
            update.setId(pptId);
            update.setFileUrl(publicUrl(objectKey));
            update.setPageCount(pageCount);
            int updated = pptMapper.updateById(update);
            if (updated == 0) {
                ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
                log.warn("PPT record no longer exists; converted file removed, pptId={}", pptId);
                return;
            }
            log.info("PPT converted to PDF successfully, pptId={}, pageCount={}, objectKey={}",
                    pptId, pageCount, objectKey);
        } catch (Exception exception) {
            log.error("PPT conversion failed, pptId={}, originalUrl={}", pptId, originalUrl, exception);
        } finally {
            deleteRecursively(workDirectory);
            CONVERTING_IDS.remove(pptId);
        }
    }

    private void downloadOriginal(OSS ossClient, String originalUrl, Path target) throws IOException {
        String objectKey = extractObjectKey(originalUrl);
        try (OSSObject object = ossClient.getObject(ossProperties.getBucketName(), objectKey);
             InputStream inputStream = object.getObjectContent()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void runLibreOffice(Path inputFile, Path outputDirectory, Path profileDirectory)
            throws IOException, InterruptedException {
        Path logFile = outputDirectory.resolve("libreoffice.log");
        Process process = new ProcessBuilder(
                conversionProperties.getLibreOfficePath(),
                "--headless",
                "--nologo",
                "--nodefault",
                "--nolockcheck",
                "-env:UserInstallation=" + profileDirectory.toUri(),
                "--convert-to", "pdf:impress_pdf_Export",
                "--outdir", outputDirectory.toString(),
                inputFile.toString())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();

        Duration timeout = conversionProperties.getTimeout();
        boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("LibreOffice conversion timed out after " + timeout);
        }
        if (process.exitValue() != 0) {
            String output = Files.exists(logFile) ? Files.readString(logFile) : "";
            throw new IOException("LibreOffice exited with code " + process.exitValue() + ": " + output);
        }
    }

    private Path findGeneratedPdf(Path outputDirectory) throws IOException {
        try (Stream<Path> files = Files.list(outputDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("LibreOffice did not generate a PDF file"));
        }
    }

    private int readPageCount(Path pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
            int pageCount = document.getNumberOfPages();
            if (pageCount <= 0) {
                throw new IOException("Converted PDF contains no pages");
            }
            return pageCount;
        }
    }

    private void uploadPdf(OSS ossClient, String objectKey, Path pdfFile) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/pdf");
        metadata.setContentDisposition("inline");
        metadata.setContentLength(Files.size(pdfFile));
        try (InputStream inputStream = Files.newInputStream(pdfFile)) {
            ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream, metadata);
        }
    }

    private String buildPreviewObjectKey(Long pptId) {
        String directory = conversionProperties.getPreviewDirectory().replaceAll("^/+|/+$", "");
        return directory + "/" + pptId + "/" + UUID.randomUUID() + ".pdf";
    }

    private String publicUrl(String objectKey) {
        if (ossProperties.getPublicDomain() != null && !ossProperties.getPublicDomain().isBlank()) {
            return stripTrailingSlash(ossProperties.getPublicDomain()) + "/" + objectKey;
        }
        String endpoint = ossProperties.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + ossProperties.getBucketName() + "." + stripTrailingSlash(endpoint)
                + "/" + objectKey;
    }

    private String extractObjectKey(String url) {
        String path = URI.create(url).getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            throw new IllegalArgumentException("Invalid PPT OSS URL");
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String safeInputFileName(String originalName) {
        String extension = originalName != null && originalName.toLowerCase(Locale.ROOT).endsWith(".ppt")
                ? ".ppt" : ".pptx";
        return "source" + extension;
    }

    private String stripTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    log.warn("Failed to delete PPT conversion temp file: {}", path, exception);
                }
            });
        } catch (IOException exception) {
            log.warn("Failed to clean PPT conversion temp directory: {}", directory, exception);
        }
    }
}
