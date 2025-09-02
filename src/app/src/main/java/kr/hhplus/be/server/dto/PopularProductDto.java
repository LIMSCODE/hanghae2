package kr.hhplus.be.server.dto;

import java.math.BigDecimal;

public class PopularProductDto {

    private Integer rank;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Long totalSalesQuantity;
    private BigDecimal totalSalesAmount;

    public PopularProductDto() {
    }

    public PopularProductDto(Long productId, String productName, BigDecimal price, 
                           Long totalSalesQuantity, BigDecimal totalSalesAmount) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.totalSalesQuantity = totalSalesQuantity;
        this.totalSalesAmount = totalSalesAmount;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getTotalSalesQuantity() {
        return totalSalesQuantity;
    }

    public void setTotalSalesQuantity(Long totalSalesQuantity) {
        this.totalSalesQuantity = totalSalesQuantity;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }
}