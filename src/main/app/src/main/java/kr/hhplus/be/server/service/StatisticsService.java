package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.ProductSalesStatistics;
import kr.hhplus.be.server.dto.PopularProductDto;
import kr.hhplus.be.server.dto.PopularProductsResponse;
import kr.hhplus.be.server.repository.ProductSalesStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final ProductSalesStatisticsRepository statisticsRepository;

    public StatisticsService(ProductSalesStatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    public PopularProductsResponse getPopularProducts(Integer days, Integer limit) {
        if (days == null || days <= 0) {
            days = 3; // 기본값 3일
        }
        if (limit == null || limit <= 0) {
            limit = 5; // 기본값 5개
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<ProductSalesStatisticsRepository.PopularProductDto> results = 
            statisticsRepository.findTopProductsByPeriod(startDate, endDate);

        AtomicInteger rank = new AtomicInteger(1);
        List<PopularProductDto> popularProducts = results.stream()
            .limit(limit)
            .map(result -> {
                PopularProductDto dto = new PopularProductDto(
                    result.getProductId(),
                    result.getName(),
                    result.getPrice(),
                    result.getTotalQuantity(),
                    result.getTotalAmount()
                );
                dto.setRank(rank.getAndIncrement());
                return dto;
            })
            .collect(Collectors.toList());

        return new PopularProductsResponse(startDate, endDate, days, popularProducts);
    }

    @Transactional
    public void updateSalesStatistics(Long productId, Integer quantity, BigDecimal amount) {
        LocalDate salesDate = LocalDate.now();
        
        Optional<ProductSalesStatistics> existingStat = 
            statisticsRepository.findByProductIdAndSalesDate(productId, salesDate);

        if (existingStat.isPresent()) {
            ProductSalesStatistics stat = existingStat.get();
            stat.addSales(quantity, amount);
            statisticsRepository.save(stat);
        } else {
            ProductSalesStatistics newStat = ProductSalesStatistics.create(
                productId, salesDate, quantity, amount
            );
            statisticsRepository.save(newStat);
        }
    }

    @Transactional
    public void updateSalesStatisticsWithUpsert(Long productId, Integer quantity, BigDecimal amount) {
        LocalDate salesDate = LocalDate.now();
        statisticsRepository.upsertDailySales(productId, salesDate, quantity, amount);
    }
}