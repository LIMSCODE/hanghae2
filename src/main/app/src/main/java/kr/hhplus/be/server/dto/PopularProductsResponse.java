package kr.hhplus.be.server.dto;

import java.time.LocalDate;
import java.util.List;

public class PopularProductsResponse {

    private PeriodInfo period;
    private List<PopularProductDto> products;

    public PopularProductsResponse() {
    }

    public PopularProductsResponse(LocalDate startDate, LocalDate endDate, Integer days, List<PopularProductDto> products) {
        this.period = new PeriodInfo(startDate, endDate, days);
        this.products = products;
    }

    public PeriodInfo getPeriod() {
        return period;
    }

    public void setPeriod(PeriodInfo period) {
        this.period = period;
    }

    public List<PopularProductDto> getProducts() {
        return products;
    }

    public void setProducts(List<PopularProductDto> products) {
        this.products = products;
    }

    public static class PeriodInfo {
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;

        public PeriodInfo() {
        }

        public PeriodInfo(LocalDate startDate, LocalDate endDate, Integer days) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.days = days;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public Integer getDays() {
            return days;
        }

        public void setDays(Integer days) {
            this.days = days;
        }
    }
}