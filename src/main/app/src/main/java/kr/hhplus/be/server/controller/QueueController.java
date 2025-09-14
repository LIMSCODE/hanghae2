package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.domain.Queue;
import kr.hhplus.be.server.dto.ApiResponse;
import kr.hhplus.be.server.dto.QueueResponse;
import kr.hhplus.be.server.dto.QueueStatisticsResponse;
import kr.hhplus.be.server.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    /**
     * 대기열 진입 (토큰 발급)
     */
    @PostMapping("/enter")
    public ResponseEntity<ApiResponse<QueueResponse>> enterQueue(@RequestParam Long userId) {
        Queue queue = queueService.enterQueue(userId);
        QueueResponse response = new QueueResponse(queue);

        return ResponseEntity.ok(
            ApiResponse.success("대기열에 진입하였습니다", response)
        );
    }

    /**
     * 대기열 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QueueResponse>> getQueueStatus(@RequestParam String token) {
        Queue queue = queueService.getQueueStatus(token);
        QueueResponse response = new QueueResponse(queue);

        return ResponseEntity.ok(
            ApiResponse.success("대기열 상태 조회가 완료되었습니다", response)
        );
    }

    /**
     * 대기열에서 나가기
     */
    @DeleteMapping("/exit")
    public ResponseEntity<ApiResponse<String>> exitQueue(@RequestParam String token) {
        queueService.exitQueue(token);

        return ResponseEntity.ok(
            ApiResponse.success("대기열에서 나갔습니다", "SUCCESS")
        );
    }

    /**
     * 대기열 통계 조회 (관리용)
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<QueueStatisticsResponse>> getQueueStatistics() {
        QueueService.QueueStatistics statistics = queueService.getQueueStatistics();
        QueueStatisticsResponse response = new QueueStatisticsResponse(statistics);

        return ResponseEntity.ok(
            ApiResponse.success("대기열 통계 조회가 완료되었습니다", response)
        );
    }
}