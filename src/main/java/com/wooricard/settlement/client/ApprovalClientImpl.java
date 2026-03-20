package com.wooricard.settlement.client;

import com.wooricard.settlement.dto.ApprovalDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 승인/결제 서비스 클라이언트 구현체
 * BankClientImpl 구조를 참고하여 RestTemplate 기반으로 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalClientImpl implements ApprovalClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${approval.service.url:http://localhost:8081}")
    private String approvalServiceUrl;

    /**
     * TODO
     * 승인/결제 서비스에서 특정 날짜의 승인 완료 내역을 페이지 단위로 조회
     *
     * 호출 예시:
     * GET http://localhost:8081/api/authorizations?date=2025-03-20&page=0&size=100&status=APPROVED
     */
    @Override
    public List<ApprovalDto> getApprovedByDate(LocalDate date, int page, int size) {
        String url = UriComponentsBuilder
                .fromHttpUrl(approvalServiceUrl + "/api/authorizations")
                .queryParam("date", date)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("status", "APPROVED")
                .toUriString();

        log.info("승인 내역 조회 요청 - date: {}, page: {}, size: {}", date, page, size);

        try {
            ResponseEntity<List<ApprovalDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<ApprovalDto> result = response.getBody();
            if (result == null) {
                return Collections.emptyList();
            }

            log.info("승인 내역 조회 완료 - date: {}, page: {}, 조회 건수: {}", date, page, result.size());
            return result;

        } catch (Exception e) {
            log.error("승인 내역 조회 실패 - date: {}, page: {}, error: {}", date, page, e.getMessage());
            throw new RuntimeException("승인 서비스 호출 실패: " + e.getMessage(), e);
        }
    }
}