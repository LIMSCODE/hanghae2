package kr.hhplus.be.server.infrastructure.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RedisDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

    private final RedissonClient redissonClient;

    public RedisDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 분산락을 이용한 작업 실행
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간 (초)
     * @param leaseTime 락 보유 시간 (초)
     * @param supplier 실행할 작업
     * @return 작업 결과
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLockAcquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!isLockAcquired) {
                log.warn("Failed to acquire lock for key: {}", lockKey);
                throw new IllegalStateException("Could not acquire lock for key: " + lockKey);
            }

            log.debug("Lock acquired for key: {}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            log.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition was interrupted", e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released for key: {}", lockKey);
            }
        }
    }

    /**
     * 기본 설정으로 분산락 실행 (대기시간: 5초, 보유시간: 10초)
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 5, 10, supplier);
    }

    /**
     * void 반환 작업을 위한 분산락 실행
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * void 반환 작업을 위한 기본 분산락 실행
     */
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, 5, 10, runnable);
    }
}