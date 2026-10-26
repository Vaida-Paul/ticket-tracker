package com.example.skeleton.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private static final Set<String> SENSITIVE_PATH_PREFIXES = Set.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    private final ObjectMapper objectMapper;

    @Around("execution(* com.example.skeleton.controller..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();

        String method = request.getMethod();
        String path = request.getRequestURI();
        String controllerMethod = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName() + "(..)";

        log.info("START HTTP {} {} | controller_method={}", method, path, controllerMethod);

        if (log.isDebugEnabled()) {
            String requestBody = isSensitivePath(path)
                    ? "[MASKED]"
                    : extractRequestBody(joinPoint.getArgs());

            if (requestBody != null) {
                log.debug("REQUEST BODY {} {} | body={}", method, path, requestBody);
            }
        }

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        Integer status = extractStatus(result);
        log.info("END HTTP {} {} | status={} | durationMs={}", method, path, status, duration);

        return result;
    }

    private boolean isSensitivePath(String path) {
        return SENSITIVE_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String extractRequestBody(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) continue;
            try {
                return objectMapper.writeValueAsString(arg);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Integer extractStatus(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            return response.getStatusCode().value();
        }
        return 200;
    }
}