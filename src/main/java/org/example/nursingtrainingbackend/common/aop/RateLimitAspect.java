// 文件路径: src/main/java/org/example/nursingtrainingbackend/common/aop/RateLimitAspect.java
package org.example.nursingtrainingbackend.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.nursingtrainingbackend.common.annotation.RateLimit;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private org.example.nursingtrainingbackend.common.aop.RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = getClientIpAddress(request);
        String key = getCacheKey(rateLimit, request, ip);

        if (rateLimitService.tryAcquire(key, rateLimit.count(), rateLimit.time())) {
            return point.proceed();
        } else {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    private String getCacheKey(RateLimit rateLimit, HttpServletRequest request, String ip) {
        String key = rateLimit.key();
        if (key.isEmpty()) {
            key = request.getRequestURI();
        }

        switch (rateLimit.limitType()) {
            case IP:
                return "nursing:limit:" + key + ":ip:" + ip;
            case USER:
                // 获取当前用户名，这里可以根据实际的安全框架获取
                String username = getCurrentUsername();
                return "nursing:limit:" + key + ":user:" + username;
            case DEFAULT:
            default:
                return "nursing:limit:" + key + ":" + ip;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 处理多级代理的情况，取第一个IP
            int index = xForwardedFor.indexOf(",");
            if (index != -1) {
                return xForwardedFor.substring(0, index);
            } else {
                return xForwardedFor;
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String getCurrentUsername() {
        // 这里可以根据实际情况从SecurityContext获取当前用户名
        // 示例：return SecurityContextHolder.getContext().getAuthentication().getName();
        return "anonymous";
    }
}
