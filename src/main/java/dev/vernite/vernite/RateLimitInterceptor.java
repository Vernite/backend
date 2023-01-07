package dev.vernite.vernite;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.servlet.HandlerInterceptor;

import dev.vernite.vernite.user.auth.AuthController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limit interceptor. Limits GET requests to 1000 per minute and
 * POST/PUT/DELETE to 100 per minute.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int GET_LIMIT = 1000;

    private static final int MODIFY_LIMIT = 100;

    private final Map<String, Bucket> getLimit = new ConcurrentHashMap<>();

    private final Map<String, Bucket> modifyLimit = new ConcurrentHashMap<>();

    private Bucket getGetBucket(String apiKey) {
        return getLimit.computeIfAbsent(apiKey, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(GET_LIMIT, Refill.greedy(GET_LIMIT, Duration.ofMinutes(1))))
                .build());
    }

    private Bucket getModifyBucket(String apiKey) {
        return modifyLimit.computeIfAbsent(apiKey, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(MODIFY_LIMIT, Refill.greedy(MODIFY_LIMIT, Duration.ofMinutes(1))))
                .build());
    }

    static private String getApiKey(HttpServletRequest request) {
        for (var cookies : request.getCookies()) {
            if (cookies.getName().equals(AuthController.COOKIE_NAME)) {
                return cookies.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String apiKey = getApiKey(request);
        if (apiKey == null) {
            apiKey = request.getHeader("X-Forwarded-For");
            if (apiKey == null) {
                apiKey = request.getRemoteAddr();
            }
        }

        Bucket tokenBucket;
        if (request.getMethod().equals("GET")) {
            tokenBucket = getGetBucket(apiKey);
        } else {
            tokenBucket = getModifyBucket(apiKey);
        }

        var probe = tokenBucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.sendError(429, "You have exhausted your API Request Quota");
            return false;
        }
    }
}
