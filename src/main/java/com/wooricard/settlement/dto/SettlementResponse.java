package com.wooricard.settlement.dto;

import com.wooricard.settlement.entity.Settlement;
import com.wooricard.settlement.entity.SettlementStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 조회 응답 DTO
 * 엔티티를 직접 노출하지 않고 필요한 필드만 반환
 */
@Getter
@Builder
public class SettlementResponse {

    private final Long id;
    private final LocalDate settlementDate;
    private final String merchantId;
    private final Integer totalCount;
    private final BigDecimal totalAmount;
    private final SettlementStatus status;
    private final LocalDateTime completedAt;
    private final String failureReason;

    /**
     * Settlement 엔티티 → SettlementResponse 변환
     */
    public static SettlementResponse from(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .settlementDate(settlement.getSettlementDate())
                .merchantId(settlement.getMerchantId())
                .totalCount(settlement.getTotalCount())
                .totalAmount(settlement.getTotalAmount())
                .status(settlement.getStatus())
                .completedAt(settlement.getCompletedAt())
                .failureReason(settlement.getFailureReason())
                .build();
    }
}