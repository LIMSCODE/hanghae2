package kr.hhplus.be.server.domain.concert.repository;

import kr.hhplus.be.server.domain.concert.Concert;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository {
    Concert save(Concert concert);
    Optional<Concert> findById(Long concertId);
    List<Concert> findAll();
}