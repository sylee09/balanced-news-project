package producer.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, Boolean> newsUrlCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofHours(1))
                .build();
    }

}
