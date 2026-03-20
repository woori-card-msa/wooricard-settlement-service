package com.wooricard.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정산 엔티티
 * 일별 + 가맹점별 카드 승인 내역을 집계한 정산 결과 저장
 */
@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_date",
                columnNames = {"settlementDate", "merchantId"}  // 같은 날짜 + 같은 가맹점 중복 방지
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 정산 대상 날짜 (예: 2025-03-20)
     */
    @Column(nullable = false)
    private LocalDate settlementDate;

    /**
     * 가맹점 ID (Authorization.merchantId 기준으로 집계)
     */
    @Column(nullable = false, length = 50)
    private String merchantId;

    /**
     * 정산 대상 승인 건수 (APPROVED 상태인 거래만 집계)
     */
    @Column(nullable = false)
    private Integer totalCount;

    /**
     * 정산 대상 총 금액
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 정산 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    /**
     * 정산 완료 일시 (COMPLETED 상태일 때만 값 존재)
     */
    @Column
    private LocalDateTime completedAt;

    /**
     * 실패 사유 (FAILED 상태일 때만 값 존재)
     */
    @Column(length = 255)
    private String failureReason;

    /**
     * 생성 일시 (배치 시작 시각)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시 (상태 변경 시각 추적)
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // --- 상태 전이 편의 메서드 ---

    public void complete(int count, BigDecimal amount) {
        this.totalCount = count;
        this.totalAmount = amount;
        this.status = SettlementStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = SettlementStatus.FAILED;
        this.failureReason = reason;
    }
}