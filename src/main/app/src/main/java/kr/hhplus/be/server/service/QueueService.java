package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Queue;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.QueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class QueueService {

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);
    private static final int MAX_ACTIVE_USERS = 100; // 최대 동시 활성 사용자 수

    private final QueueRepository queueRepository;
    private final UserService userService;

    public QueueService(QueueRepository queueRepository, UserService userService) {
        this.queueRepository = queueRepository;
        this.userService = userService;
    }

    /**
     * 대기열 진입 (토큰 발급)
     */
    @Transactional
    public Queue enterQueue(Long userId) {
        // 1. 사용자 존재 확인
        User user = userService.getUser(userId);

        // 2. 이미 대기열에 있는지 확인
        Optional<Queue> existingQueue = queueRepository.findByUserId(userId);
        if (existingQueue.isPresent()) {
            Queue queue = existingQueue.get();
            if (queue.isActive()) {
                return queue; // 이미 활성화된 사용자
            } else if (queue.isWaiting()) {
                updateQueuePosition(queue); // 대기 중인 사용자의 포지션 업데이트
                return queue;
            }
            // 만료된 경우 새로 생성
        }

        // 3. 새로운 대기열 엔트리 생성
        Queue queue = new Queue(userId);
        Queue savedQueue = queueRepository.save(queue);

        // 4. 대기 순번 계산
        updateQueuePosition(savedQueue);

        logger.info("User entered queue. UserId: {}, Token: {}, Position: {}",
                   userId, savedQueue.getToken(), savedQueue.getPosition());

        return savedQueue;
    }

    /**
     * 대기열 상태 조회
     */
    public Queue getQueueStatus(String token) {
        Queue queue = queueRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_QUEUE_TOKEN));

        if (queue.isExpired()) {
            throw new BusinessException(ErrorCode.QUEUE_EXPIRED);
        }

        // 대기 중인 경우 최신 포지션으로 업데이트
        if (queue.isWaiting()) {
            updateQueuePosition(queue);
        }

        return queue;
    }

    /**
     * 토큰 검증 (API 접근 시 사용)
     */
    public void validateToken(String token) {
        Queue queue = queueRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_QUEUE_TOKEN));

        if (queue.isExpired()) {
            throw new BusinessException(ErrorCode.QUEUE_EXPIRED);
        }

        if (!queue.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_QUEUE_TOKEN, "활성화되지 않은 토큰입니다"));
        }
    }

    /**
     * 대기열에서 나가기 (토큰 만료 처리)
     */
    @Transactional
    public void exitQueue(String token) {
        Queue queue = queueRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_QUEUE_TOKEN));

        queue.expire();
        logger.info("User exited queue. Token: {}, UserId: {}", token, queue.getUserId());
    }

    /**
     * 대기열 포지션 업데이트
     */
    private void updateQueuePosition(Queue queue) {
        if (queue.isWaiting()) {
            Long position = queueRepository.countWaitingQueuesBefore(queue.getEnteredAt()) + 1;
            queue.updatePosition(position.intValue());
        }
    }

    /**
     * 스케줄러: 대기중인 사용자를 활성화
     */
    @Scheduled(fixedRate = 30000) // 30초마다 실행
    @Transactional
    public void activateWaitingUsers() {
        try {
            // 1. 만료된 활성 사용자 정리
            expireOldActiveQueues();

            // 2. 현재 활성 사용자 수 확인
            Long activeCount = queueRepository.countActiveQueues();
            int slotsAvailable = MAX_ACTIVE_USERS - activeCount.intValue();

            if (slotsAvailable <= 0) {
                return; // 활성화할 슬롯이 없음
            }

            // 3. 대기 중인 사용자를 활성화
            List<Queue> waitingQueues = queueRepository.findWaitingQueuesOrderByEnteredAt();
            int activated = 0;

            for (Queue queue : waitingQueues) {
                if (activated >= slotsAvailable) {
                    break;
                }

                queue.activate();
                activated++;

                logger.info("Queue activated. UserId: {}, Token: {}",
                           queue.getUserId(), queue.getToken());
            }

            if (activated > 0) {
                logger.info("Activated {} users from queue. Active count: {}",
                           activated, activeCount + activated);
            }

        } catch (Exception e) {
            logger.error("Error during queue activation process", e);
        }
    }

    /**
     * 만료된 활성 대기열 정리
     */
    @Transactional
    public void expireOldActiveQueues() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = queueRepository.expireQueues(now);

        if (expiredCount > 0) {
            logger.info("Expired {} old active queues", expiredCount);
        }
    }

    /**
     * 대기열 통계 조회
     */
    public QueueStatistics getQueueStatistics() {
        Long activeCount = queueRepository.countActiveQueues();
        List<Queue> waitingQueues = queueRepository.findWaitingQueuesOrderByEnteredAt();

        return new QueueStatistics(
            activeCount.intValue(),
            waitingQueues.size(),
            MAX_ACTIVE_USERS
        );
    }

    /**
     * 대기열 통계 정보
     */
    public static class QueueStatistics {
        private final int activeUsers;
        private final int waitingUsers;
        private final int maxActiveUsers;

        public QueueStatistics(int activeUsers, int waitingUsers, int maxActiveUsers) {
            this.activeUsers = activeUsers;
            this.waitingUsers = waitingUsers;
            this.maxActiveUsers = maxActiveUsers;
        }

        public int getActiveUsers() { return activeUsers; }
        public int getWaitingUsers() { return waitingUsers; }
        public int getMaxActiveUsers() { return maxActiveUsers; }
        public int getAvailableSlots() { return maxActiveUsers - activeUsers; }
    }
}