package org.example.nursingtrainingbackend.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.common.result.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            .withZone(ZoneId.of("GMT+8"));

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        try {
            objectMapper.writeValue(writer, Result.failure(ErrorCode.FORBIDDEN));
        } catch (Exception e) {
            log.warn("Jackson serialization failed in AccessDeniedHandler, using fallback JSON", e);
            writer.write(String.format(
                    "{\"code\":403,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                    ErrorCode.FORBIDDEN.getMessage(),
                    FORMATTER.format(Instant.now())
            ));
        }
        writer.flush();
    }
}
