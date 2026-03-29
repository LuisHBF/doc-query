package br.com.docquery.gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int GENERAL_CAPACITY = 60;
    private static final int CHAT_CAPACITY = 10;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> chatBuckets = new ConcurrentHashMap<>();

    public Bucket resolveGeneralBucket(String key) {
        return generalBuckets.computeIfAbsent(key, k -> buildBucket(GENERAL_CAPACITY));
    }

    public Bucket resolveChatBucket(String key) {
        return chatBuckets.computeIfAbsent(key, k -> buildBucket(CHAT_CAPACITY));
    }

    private Bucket buildBucket(int capacity) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, REFILL_PERIOD)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

}
