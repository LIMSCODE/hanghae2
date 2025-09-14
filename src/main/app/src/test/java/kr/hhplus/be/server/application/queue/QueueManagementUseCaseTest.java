package kr.hhplus.be.server.application.queue;

import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.repository.QueueTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueManagementUseCaseTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    private QueueManagementUseCase queueManagementUseCase;

    @BeforeEach
    void setUp() {
        queueManagementUseCase = new QueueManagementUseCase(queueTokenRepository);
    }

    @Test
    @DisplayName("토큰 발급 성공 - 새로운 사용자")
    void issueToken_NewUser_Success() {
        // Given
        Long userId = 1L;
        Long nextPosition = 5L;

        when(queueTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(queueTokenRepository.getNextQueuePosition()).thenReturn(nextPosition);
        when(queueTokenRepository.save(any(QueueToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        QueueManagementUseCase.QueueTokenResult result = queueManagementUseCase.issueToken(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("WAITING");
        assertThat(result.getQueuePosition()).isEqualTo(nextPosition);
        assertThat(result.getEstimatedWaitTimeMinutes()).isGreaterThanOrEqualTo(0);

        verify(queueTokenRepository).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("토큰 발급 - 이미 활성화된 토큰이 있는 경우 기존 토큰 반환")
    void issueToken_ExistingActiveToken_ReturnExisting() {
        // Given
        Long userId = 1L;
        QueueToken activeToken = new QueueToken(userId, 1L);
        activeToken.activate(10);

        when(queueTokenRepository.findByUserId(userId)).thenReturn(Optional.of(activeToken));

        // When
        QueueManagementUseCase.QueueTokenResult result = queueManagementUseCase.issueToken(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getQueuePosition()).isEqualTo(1L);
        assertThat(result.getEstimatedWaitTimeMinutes()).isEqualTo(0);

        verify(queueTokenRepository, never()).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("대기열 상태 조회 성공")
    void getQueueStatus_Success() {
        // Given
        String tokenUuid = "test-token-uuid";
        QueueToken waitingToken = new QueueToken(1L, 3L);

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(waitingToken));

        // When
        QueueManagementUseCase.QueueStatusResult result = queueManagementUseCase.getQueueStatus(tokenUuid);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("WAITING");
        assertThat(result.getQueuePosition()).isEqualTo(3L);
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("대기열 상태 조회 실패 - 존재하지 않는 토큰")
    void getQueueStatus_TokenNotFound_ShouldThrowException() {
        // Given
        String invalidTokenUuid = "invalid-token";

        when(queueTokenRepository.findByTokenUuid(invalidTokenUuid)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> queueManagementUseCase.getQueueStatus(invalidTokenUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token not found");
    }

    @Test
    @DisplayName("대기열 처리 - 만료된 토큰들 정리하고 새로운 토큰들 활성화")
    void processQueue_ActivateWaitingTokens() {
        // Given
        QueueToken expiredToken = new QueueToken(1L, 1L);
        expiredToken.activate(10);
        // 만료 시간을 과거로 설정하여 만료된 상태로 만듦

        QueueToken waitingToken1 = new QueueToken(2L, 1L);
        QueueToken waitingToken2 = new QueueToken(3L, 2L);

        List<QueueToken> expiredTokens = Arrays.asList(expiredToken);
        List<QueueToken> activeTokens = Arrays.asList(); // 현재 활성화된 토큰 없음
        List<QueueToken> waitingTokens = Arrays.asList(waitingToken1, waitingToken2);

        when(queueTokenRepository.findExpiredTokens()).thenReturn(expiredTokens);
        when(queueTokenRepository.findActiveTokens()).thenReturn(activeTokens);
        when(queueTokenRepository.findWaitingTokens()).thenReturn(waitingTokens);
        when(queueTokenRepository.save(any(QueueToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        queueManagementUseCase.processQueue();

        // Then
        verify(queueTokenRepository, atLeastOnce()).save(any(QueueToken.class));
        verify(queueTokenRepository).findExpiredTokens();
        verify(queueTokenRepository).findActiveTokens();
        verify(queueTokenRepository).findWaitingTokens();
    }

    @Test
    @DisplayName("토큰 완료 처리 성공")
    void completeToken_Success() {
        // Given
        String tokenUuid = "test-token-uuid";
        QueueToken activeToken = new QueueToken(1L, 1L);
        activeToken.activate(10);

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(activeToken));
        when(queueTokenRepository.save(any(QueueToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        queueManagementUseCase.completeToken(tokenUuid);

        // Then
        verify(queueTokenRepository).save(activeToken);
    }

    @Test
    @DisplayName("토큰 완료 처리 실패 - 존재하지 않는 토큰")
    void completeToken_TokenNotFound_ShouldThrowException() {
        // Given
        String invalidTokenUuid = "invalid-token";

        when(queueTokenRepository.findByTokenUuid(invalidTokenUuid)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> queueManagementUseCase.completeToken(invalidTokenUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token not found");

        verify(queueTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("대기열 통계 조회 성공")
    void getQueueStatistics_Success() {
        // Given
        Long waitingCount = 15L;
        List<QueueToken> activeTokens = Arrays.asList(
                new QueueToken(1L, 1L),
                new QueueToken(2L, 2L)
        );

        when(queueTokenRepository.countWaitingTokens()).thenReturn(waitingCount);
        when(queueTokenRepository.findActiveTokens()).thenReturn(activeTokens);

        // When
        QueueManagementUseCase.QueueStatistics result = queueManagementUseCase.getQueueStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getWaitingCount()).isEqualTo(15L);
        assertThat(result.getActiveCount()).isEqualTo(2L);
        assertThat(result.getMaxActiveCount()).isEqualTo(100L);
    }
}