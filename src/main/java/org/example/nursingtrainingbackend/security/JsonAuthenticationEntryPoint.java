package org.example.nursingtrainingbackend.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.common.result.Result;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            .withZone(ZoneId.of("GMT+8"));

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        try {
            objectMapper.writeValue(writer, Result.failure(ErrorCode.UNAUTHORIZED));
        } catch (Exception e) {
            log.warn("Jackson serialization failed in EntryPoint, using fallback JSON", e);
            writer.write(String.format(
                    "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                    ErrorCode.UNAUTHORIZED.getMessage(),
                    FORMATTER.format(Instant.now())
            ));
        }
        writer.flush();
    }
}
