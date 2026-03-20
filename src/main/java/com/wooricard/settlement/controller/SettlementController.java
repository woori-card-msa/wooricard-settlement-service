package com.wooricard.settlement.controller;

import com.wooricard.settlement.dto.SettlementResponse;
import com.wooricard.settlement.repository.SettlementRepository;
import com.wooricard.settlement.scheduler.SettlementScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Settlement", description = "정산 API")
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementScheduler settlementScheduler;
    private final SettlementRepository settlementRepository;

    /**
     * 정산 수동 실행 (Swagger에서 테스트용)
     * POST /api/settlements/trigger?date=2025-03-19
     */
    @Operation(summary = "정산 수동 실행", description = "특정 날짜의 정산 배치를 수동으로 실행합니다.")
    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            settlementScheduler.triggerSettlement(date);
            return ResponseEntity.ok(date + " 정산 배치 실행 완료");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(date + " 정산 배치 실행 실패: " + e.getMessage());
        }
    }

    /**
     * 특정 날짜의 전체 가맹점 정산 내역 조회
     * GET /api/settlements?date=2025-03-19
     */
    @Operation(summary = "날짜별 정산 내역 조회")
    @GetMapping
    public ResponseEntity<List<SettlementResponse>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SettlementResponse> result = settlementRepository.findBySettlementDate(date)
                .stream()
                .map(SettlementResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 가맹점의 기간별 정산 내역 조회
     * GET /api/settlements/merchant/M001?from=2025-03-01&to=2025-03-20
     */
    @Operation(summary = "가맹점별 정산 내역 조회")
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<SettlementResponse>> getByMerchant(
            @PathVariable String merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<SettlementResponse> result = settlementRepository
                .findByMerchantIdAndSettlementDateBetween(merchantId, from, to)
                .stream()
                .map(SettlementResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}