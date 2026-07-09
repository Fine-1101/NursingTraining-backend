package org.example.nursingtrainingbackend.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public final class FileNameUtils {
    private FileNameUtils() {}

    public static String extension(String fileName) {
        if (fileName == null) return "";
        String normalized = fileName.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1);
        int dot = baseName.lastIndexOf('.');
        if (dot <= 0 || dot == baseName.length() - 1) return "";
        return baseName.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^.a-z0-9]", "");
    }

    public static String objectKey(String baseDirectory, String businessDirectory, String fileName) {
        String base = cleanDirectory(baseDirectory);
        String business = cleanDirectory(businessDirectory);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.join("/", base, business, date, UUID.randomUUID() + extension(fileName));
    }

    private static String cleanDirectory(String value) {
        String cleaned = value == null ? "" : value.replace('\\', '/').replaceAll("[^a-zA-Z0-9/_-]", "");
        cleaned = cleaned.replaceAll("/{2,}", "/").replaceAll("^/+|/+$", "");
        return cleaned.isBlank() ? "files" : cleaned;
    }
}
