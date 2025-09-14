package kr.hhplus.be.server.order.infrastructure.adapter;

import kr.hhplus.be.server.order.domain.port.EventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventAdapter implements EventPort {

    private final ApplicationEventPublisher eventPublisher;

    public EventAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publishOrderCompletedEvent(Object event) {
        eventPublisher.publishEvent(event);
    }
}