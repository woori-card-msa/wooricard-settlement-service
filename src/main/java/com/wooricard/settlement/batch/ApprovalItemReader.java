package com.wooricard.settlement.batch;

import com.wooricard.settlement.client.ApprovalClient;
import com.wooricard.settlement.dto.ApprovalDto;
import com.wooricard.settlement.dto.ApprovalPageResponse;
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

    private Iterator<MerchantApprovalSummary> iterator;

    public ApprovalItemReader(ApprovalClient approvalClient, LocalDate targetDate, int pageSize) {
        this.approvalClient = approvalClient;
        this.targetDate = targetDate;
        this.pageSize = pageSize;
    }

    @Override
    public MerchantApprovalSummary read() {
        if (iterator == null) {
            iterator = loadAndGroup().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<MerchantApprovalSummary> loadAndGroup() {
        List<ApprovalDto> allApprovals = new ArrayList<>();
        int page = 0;

        while (true) {
            ApprovalPageResponse response = approvalClient.getApprovedByDate(
                    targetDate, page, pageSize, "APPROVED");

            List<ApprovalDto> content = response.getContent();
            if (content == null || content.isEmpty()) break;

            allApprovals.addAll(content);
            log.info("승인 내역 로드 - page: {}, 건수: {}, 누적: {}", page, content.size(), allApprovals.size());

            if (response.isLast()) break;
            page++;
        }

        log.info("전체 승인 내역 로드 완료 - date: {}, 총 {}건", targetDate, allApprovals.size());

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