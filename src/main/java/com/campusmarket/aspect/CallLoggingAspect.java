package com.campusmarket.aspect;

import com.campusmarket.security.LoginUser;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

@Aspect
@Component
public class CallLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(CallLoggingAspect.class);

    @Around("execution(public * com.campusmarket.controller..*(..)) "
            + "|| execution(public * com.campusmarket.service..*(..))")
    public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String target = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        String arguments = summarizeArguments(signature.getParameterNames(), joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            log.info("{} args={} result=success elapsedMs={}", target, arguments, elapsedMillis(startNanos));
            return result;
        } catch (Throwable ex) {
            log.warn("{} args={} result=exception type={} message={} elapsedMs={}",
                    target, arguments, ex.getClass().getSimpleName(), safeMessage(ex),
                    elapsedMillis(startNanos));
            throw ex;
        }
    }

    private String summarizeArguments(String[] parameterNames, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return "[]";
        }

        StringBuilder summary = new StringBuilder("[");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                summary.append(", ");
            }
            String parameterName = parameterNames != null && i < parameterNames.length
                    ? parameterNames[i]
                    : "arg" + i;
            summary.append(parameterName).append('=').append(summarizeValue(arguments[i]));
        }
        return summary.append(']').toString();
    }

    private String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof LoginUser loginUser) {
            return "LoginUser(id=" + loginUser.id() + ")";
        }
        if (value instanceof CharSequence text) {
            return "String(length=" + text.length() + ")";
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return value.getClass().getSimpleName() + "(" + value + ")";
        }
        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }
        if (value instanceof Map<?, ?> map) {
            return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }
        if (value.getClass().isArray()) {
            return value.getClass().getSimpleName() + "(length=" + Array.getLength(value) + ")";
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse) {
            return value.getClass().getSimpleName();
        }
        return value.getClass().getSimpleName();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "-" : message;
    }
}
