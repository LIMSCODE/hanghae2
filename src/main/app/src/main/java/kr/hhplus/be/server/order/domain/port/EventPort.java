package kr.hhplus.be.server.order.domain.port;

public interface EventPort {
    void publishOrderCompletedEvent(Object event);
}