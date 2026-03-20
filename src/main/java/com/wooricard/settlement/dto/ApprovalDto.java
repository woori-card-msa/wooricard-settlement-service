package com.wooricard.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 승인/결제 서비스에서 받아오는 승인 내역 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDto {

    private Long id;

    private String transactionId;

    private String merchantId;

    private BigDecimal amount;

    private String responseCode;    // "00" = 승인 건만 필터링해서 받음

    private LocalDateTime authorizationDate;
}