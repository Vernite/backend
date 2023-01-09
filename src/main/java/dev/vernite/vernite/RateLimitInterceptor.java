package dev.vernite.vernite;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Rate limit interceptor. Limits GET requests to 1000 per minute and
 * POST/PUT/DELETE to 100 per minute.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long TIME_LIMIT = TimeUnit.MINUTES.toMillis(1);
    private static final int READ_LIMIT = 1000;
    private static final int WRITE_LIMIT = 100;

    private static final Map<Long, ArrayDeque<Long>> userReadLimit = new ConcurrentHashMap<>();
    private static final Map<Long, ArrayDeque<Long>> userWriteLimit = new ConcurrentHashMap<>();
    private static final Map<String, ArrayDeque<Long>> ipReadLimit = new ConcurrentHashMap<>();
    private static final Map<String, ArrayDeque<Long>> ipWriteLimit = new ConcurrentHashMap<>();

    private static boolean isWriteMethod(String method) {
        switch (method) {
            case "GET":
            case "HEAD":
            case "OPTIONS":
                return false;
            case "PUT":
            case "PATCH":
            case "DELETE":
            case "POST":
                return true;
            default:
                return true;
        }
    }

    private static <T> int increment(T key, Map<T, ArrayDeque<Long>> map, long time, int maxSize)
            throws TooManyRequestsException {
        ArrayDeque<Long> deque = map.get(key);
        if (deque == null) {
            // do not add empty ArrayDeque to map - thread safety
            deque = new ArrayDeque<>();
            deque.addLast(time);
            map.put(key, deque);
            return maxSize - 1;
        } else {
            synchronized (deque) {
                while (!deque.isEmpty() && time - deque.getFirst() > TIME_LIMIT) {
                    deque.removeFirst();
                }
                if (deque.size() >= maxSize) {
                    throw new TooManyRequestsException(TIME_LIMIT + deque.getFirst() - time);
                }
                deque.addLast(time);
                map.putIfAbsent(key, deque);
                return maxSize - deque.size();
            }
        }
    }

    private static String getIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // this interceptor is called before and after UserResolver
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        boolean writeMethod = isWriteMethod(request.getMethod());
        long now = System.currentTimeMillis();
        try {
            int remaining;

            if (request.getAttribute("ratelimit") != null) {
                // after user resolver:
                Long userID = (Long) request.getAttribute("userID");
                if (userID == null) {
                    return true;
                }
                remaining = writeMethod ? increment(userID, userWriteLimit, now, WRITE_LIMIT)
                        : increment(userID, userReadLimit, now, READ_LIMIT);
                remaining = Math.min((int) request.getAttribute("ratelimit"), remaining);
            } else {
                // before user resolver:
                String ip = getIP(request);
                remaining = writeMethod ? increment(ip, ipWriteLimit, now, WRITE_LIMIT)
                        : increment(ip, ipReadLimit, now, READ_LIMIT);
                request.setAttribute("ratelimit", remaining);
            }
            response.setHeader("X-Rate-Limit-Remaining", Integer.toString(remaining));
            return true;
        } catch (TooManyRequestsException e) {
            // round up
            long seconds = (e.retryAfter + 999L) / 1000L;
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", Long.toString(seconds));
            response.sendError(429, "You have exhausted your API Request Quota");
            return false;
        }
    }

    private static <T> void clearMap(Map<T, ArrayDeque<Long>> map, long now) {
        Iterator<ArrayDeque<Long>> it = map.values().iterator();
        while (it.hasNext()) {
            ArrayDeque<Long> deque = it.next();
            while (!deque.isEmpty() && now - deque.getFirst() > TIME_LIMIT) {
                deque.removeFirst();
            }
            if (deque.isEmpty()) {
                it.remove();
            }
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void cleanDeques() {
        long now = System.currentTimeMillis();
        clearMap(ipWriteLimit, now);
        clearMap(ipReadLimit, now);
        clearMap(userWriteLimit, now);
        clearMap(userReadLimit, now);
    }

    @AllArgsConstructor
    private static class TooManyRequestsException extends Exception {
        @Getter
        private long retryAfter;
    }
}
