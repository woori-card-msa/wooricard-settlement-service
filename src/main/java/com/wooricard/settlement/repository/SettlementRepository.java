package com.wooricard.settlement.repository;

import com.wooricard.settlement.entity.Settlement;
import com.wooricard.settlement.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * 날짜 + 가맹점으로 단건 조회
     * (중복 정산 방지 체크 / 재실행 여부 확인)
     */
    Optional<Settlement> findBySettlementDateAndMerchantId(LocalDate settlementDate, String merchantId);

    /**
     * 특정 날짜의 전체 가맹점 정산 내역 조회
     * (하루 정산 결과 전체 확인)
     */
    List<Settlement> findBySettlementDate(LocalDate settlementDate);

    /**
     * 특정 가맹점의 기간별 정산 내역 조회
     * (가맹점 입장에서 본인 정산 내역 조회)
     */
    List<Settlement> findByMerchantIdAndSettlementDateBetween(
            String merchantId, LocalDate from, LocalDate to);

    /**
     * 특정 상태의 정산 내역 조회
     * (FAILED 건 재처리할 때 사용)
     */
    List<Settlement> findByStatus(SettlementStatus status);

    /**
     * 중복 정산 존재 여부 확인
     */
    boolean existsBySettlementDateAndMerchantId(LocalDate settlementDate, String merchantId);
}