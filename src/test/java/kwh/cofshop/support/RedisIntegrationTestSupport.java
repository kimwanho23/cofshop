package kwh.cofshop.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 운영과 같은 조합(StringRedisTemplate + Lettuce, Redisson)을 실제 Redis에 연결. Docker가 없으면 건너뜀
@Testcontainers(disabledWithoutDocker = true)
public abstract class RedisIntegrationTestSupport {

    @Container
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedissonClient redissonClient;

    protected static StringRedisTemplate redisTemplate() {
        if (redisTemplate == null) {
            connectionFactory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379))
            );
            connectionFactory.afterPropertiesSet();
            redisTemplate = new StringRedisTemplate(connectionFactory);
        }
        return redisTemplate;
    }

    protected static RedissonClient redissonClient() {
        if (redissonClient == null) {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379))
                    .setDatabase(1);
            redissonClient = Redisson.create(config);
        }
        return redissonClient;
    }

    @BeforeEach
    void flushRedis() {
        redisTemplate().execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @AfterAll
    static void closeClients() {
        if (redissonClient != null) {
            redissonClient.shutdown();
            redissonClient = null;
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
            connectionFactory = null;
            redisTemplate = null;
        }
    }
}
