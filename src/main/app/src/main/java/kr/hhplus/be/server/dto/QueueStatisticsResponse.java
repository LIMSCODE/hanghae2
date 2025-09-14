package kr.hhplus.be.server.dto;

import kr.hhplus.be.server.service.QueueService;

public class QueueStatisticsResponse {

    private int activeUsers;
    private int waitingUsers;
    private int maxActiveUsers;
    private int availableSlots;

    public QueueStatisticsResponse(QueueService.QueueStatistics statistics) {
        this.activeUsers = statistics.getActiveUsers();
        this.waitingUsers = statistics.getWaitingUsers();
        this.maxActiveUsers = statistics.getMaxActiveUsers();
        this.availableSlots = statistics.getAvailableSlots();
    }

    // Getters
    public int getActiveUsers() { return activeUsers; }
    public int getWaitingUsers() { return waitingUsers; }
    public int getMaxActiveUsers() { return maxActiveUsers; }
    public int getAvailableSlots() { return availableSlots; }
}