package com.wooricard.settlement.batch;

import com.wooricard.settlement.client.ApprovalClient;
import com.wooricard.settlement.dto.ApprovalDto;
import com.wooricard.settlement.dto.MerchantApprovalSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 승인 내역 ItemReader
 *
 * 동작 방식:
 * 1. 첫 read() 호출 시 → 승인 서비스 API를 페이지 단위로 전부 읽어서 merchantId별로 그룹핑
 * 2. 이후 read() 호출마다 → 가맹점 하나씩 MerchantApprovalSummary 반환
 * 3. 더 이상 없으면 → null 반환 (Batch가 Step 종료 신호로 인식)
 */
@Slf4j
public class ApprovalItemReader implements ItemReader<MerchantApprovalSummary> {

    private final ApprovalClient approvalClient;
    private final LocalDate targetDate;
    private final int pageSize;

    // 가맹점별 집계 결과를 순서 있게 꺼내기 위한 Iterator
    private Iterator<MerchantApprovalSummary> iterator;

    public ApprovalItemReader(ApprovalClient approvalClient, LocalDate targetDate, int pageSize) {
        this.approvalClient = approvalClient;
        this.targetDate = targetDate;
        this.pageSize = pageSize;
    }

    @Override
    public MerchantApprovalSummary read() {
        // 첫 호출 시 초기화
        if (iterator == null) {
            iterator = loadAndGroup().iterator();
        }

        // 남은 가맹점 있으면 반환, 없으면 null (Step 종료)
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * 승인 서비스에서 전체 데이터를 페이징으로 읽어서 merchantId별로 그룹핑
     */
    private List<MerchantApprovalSummary> loadAndGroup() {
        List<ApprovalDto> allApprovals = new ArrayList<>();
        int page = 0;

        // 빈 리스트 올 때까지 페이지 단위로 호출
        while (true) {
            List<ApprovalDto> pageResult = approvalClient.getApprovedByDate(targetDate, page, pageSize);
            if (pageResult.isEmpty()) break;

            allApprovals.addAll(pageResult);
            log.info("승인 내역 로드 - page: {}, 건수: {}, 누적: {}", page, pageResult.size(), allApprovals.size());
            page++;
        }

        log.info("전체 승인 내역 로드 완료 - date: {}, 총 {}건", targetDate, allApprovals.size());

        // merchantId별로 그룹핑 → MerchantApprovalSummary 리스트로 변환
        return allApprovals.stream()
                .collect(Collectors.groupingBy(ApprovalDto::getMerchantId))
                .entrySet().stream()
                .map(entry -> MerchantApprovalSummary.builder()
                        .merchantId(entry.getKey())
                        .settlementDate(targetDate)
                        .approvals(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}