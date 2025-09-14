package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Queue;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.QueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueService 단위 테스트")
class QueueServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private QueueService queueService;

    private User testUser;
    private Queue testQueue;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .name("테스트유저")
                .balance(BigDecimal.valueOf(50000))
                .build();

        testQueue = new Queue(1L);
        testQueue.updatePosition(5); // 5번째 대기
    }

    @Test
    @DisplayName("대기열 진입 성공 - 신규 사용자")
    void enterQueue_NewUser_Success() {
        // given
        Long userId = 1L;

        given(userService.getUser(userId)).willReturn(testUser);
        given(queueRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(queueRepository.save(any(Queue.class))).willReturn(testQueue);
        given(queueRepository.countWaitingQueuesBefore(any(LocalDateTime.class))).willReturn(4L);

        // when
        Queue result = queueService.enterQueue(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(Queue.QueueStatus.WAITING);

        then(userService).should().getUser(userId);
        then(queueRepository).should().findByUserId(userId);
        then(queueRepository).should().save(any(Queue.class));
    }

    @Test
    @DisplayName("대기열 진입 - 이미 활성화된 사용자")
    void enterQueue_AlreadyActive_Success() {
        // given
        Long userId = 1L;
        Queue activeQueue = new Queue(userId);
        activeQueue.activate();

        given(userService.getUser(userId)).willReturn(testUser);
        given(queueRepository.findByUserId(userId)).willReturn(Optional.of(activeQueue));

        // when
        Queue result = queueService.enterQueue(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(Queue.QueueStatus.ACTIVE);

        // 새로운 큐를 저장하지 않아야 함
        then(queueRepository).should(never()).save(any(Queue.class));
    }

    @Test
    @DisplayName("대기열 진입 - 이미 대기중인 사용자")
    void enterQueue_AlreadyWaiting_Success() {
        // given
        Long userId = 1L;

        given(userService.getUser(userId)).willReturn(testUser);
        given(queueRepository.findByUserId(userId)).willReturn(Optional.of(testQueue));
        given(queueRepository.countWaitingQueuesBefore(any(LocalDateTime.class))).willReturn(4L);

        // when
        Queue result = queueService.enterQueue(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(Queue.QueueStatus.WAITING);

        // 새로운 큐를 저장하지 않아야 함
        then(queueRepository).should(never()).save(any(Queue.class));
    }

    @Test
    @DisplayName("대기열 상태 조회 성공")
    void getQueueStatus_Success() {
        // given
        String token = "test-token";
        testQueue.setToken(token);

        given(queueRepository.findByToken(token)).willReturn(Optional.of(testQueue));
        given(queueRepository.countWaitingQueuesBefore(any(LocalDateTime.class))).willReturn(4L);

        // when
        Queue result = queueService.getQueueStatus(token);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(token);
        assertThat(result.getStatus()).isEqualTo(Queue.QueueStatus.WAITING);

        then(queueRepository).should().findByToken(token);
    }

    @Test
    @DisplayName("대기열 상태 조회 실패 - 잘못된 토큰")
    void getQueueStatus_InvalidToken() {
        // given
        String invalidToken = "invalid-token";

        given(queueRepository.findByToken(invalidToken)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queueService.getQueueStatus(invalidToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_QUEUE_TOKEN);
    }

    @Test
    @DisplayName("대기열 상태 조회 실패 - 만료된 토큰")
    void getQueueStatus_ExpiredToken() {
        // given
        String token = "expired-token";
        Queue expiredQueue = new Queue(1L);
        expiredQueue.setToken(token);
        expiredQueue.expire();

        given(queueRepository.findByToken(token)).willReturn(Optional.of(expiredQueue));

        // when & then
        assertThatThrownBy(() -> queueService.getQueueStatus(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QUEUE_EXPIRED);
    }

    @Test
    @DisplayName("토큰 검증 성공 - 활성화된 토큰")
    void validateToken_ActiveToken_Success() {
        // given
        String token = "active-token";
        Queue activeQueue = new Queue(1L);
        activeQueue.setToken(token);
        activeQueue.activate();

        given(queueRepository.findByToken(token)).willReturn(Optional.of(activeQueue));

        // when & then - 예외가 발생하지 않아야 함
        assertThatNoException().isThrownBy(() -> queueService.validateToken(token));

        then(queueRepository).should().findByToken(token);
    }

    @Test
    @DisplayName("토큰 검증 실패 - 대기중인 토큰")
    void validateToken_WaitingToken_Fail() {
        // given
        String token = "waiting-token";
        testQueue.setToken(token);

        given(queueRepository.findByToken(token)).willReturn(Optional.of(testQueue));

        // when & then
        assertThatThrownBy(() -> queueService.validateToken(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("활성화되지 않은 토큰입니다");
    }

    @Test
    @DisplayName("대기열 나가기 성공")
    void exitQueue_Success() {
        // given
        String token = "test-token";
        testQueue.setToken(token);

        given(queueRepository.findByToken(token)).willReturn(Optional.of(testQueue));

        // when
        queueService.exitQueue(token);

        // then
        assertThat(testQueue.getStatus()).isEqualTo(Queue.QueueStatus.EXPIRED);
        assertThat(testQueue.getExpiredAt()).isNotNull();

        then(queueRepository).should().findByToken(token);
    }

    @Test
    @DisplayName("대기중인 사용자 활성화 프로세스")
    void activateWaitingUsers_Success() {
        // given
        List<Queue> waitingQueues = List.of(
                new Queue(1L),
                new Queue(2L),
                new Queue(3L)
        );

        given(queueRepository.expireQueues(any(LocalDateTime.class))).willReturn(2); // 2명 만료
        given(queueRepository.countActiveQueues()).willReturn(97L); // 현재 97명 활성
        given(queueRepository.findWaitingQueuesOrderByEnteredAt()).willReturn(waitingQueues);

        // when
        queueService.activateWaitingUsers();

        // then
        // 3명의 슬롯이 있으므로 모든 대기자가 활성화되어야 함
        for (Queue queue : waitingQueues) {
            assertThat(queue.getStatus()).isEqualTo(Queue.QueueStatus.ACTIVE);
            assertThat(queue.getActivatedAt()).isNotNull();
        }

        then(queueRepository).should().expireQueues(any(LocalDateTime.class));
        then(queueRepository).should().countActiveQueues();
        then(queueRepository).should().findWaitingQueuesOrderByEnteredAt();
    }

    @Test
    @DisplayName("대기열 통계 조회 성공")
    void getQueueStatistics_Success() {
        // given
        given(queueRepository.countActiveQueues()).willReturn(75L);
        given(queueRepository.findWaitingQueuesOrderByEnteredAt())
                .willReturn(List.of(new Queue(1L), new Queue(2L)));

        // when
        QueueService.QueueStatistics result = queueService.getQueueStatistics();

        // then
        assertThat(result.getActiveUsers()).isEqualTo(75);
        assertThat(result.getWaitingUsers()).isEqualTo(2);
        assertThat(result.getMaxActiveUsers()).isEqualTo(100);
        assertThat(result.getAvailableSlots()).isEqualTo(25);

        then(queueRepository).should().countActiveQueues();
        then(queueRepository).should().findWaitingQueuesOrderByEnteredAt();
    }
}