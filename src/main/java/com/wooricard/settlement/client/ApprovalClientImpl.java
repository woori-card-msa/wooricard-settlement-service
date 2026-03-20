package com.wooricard.settlement.client;

import com.wooricard.settlement.dto.ApprovalDto;
import com.wooricard.settlement.dto.ApprovalPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ApprovalClientImpl implements ApprovalClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${approval.service.url:http://localhost:8081}")
    private String approvalServiceUrl;

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
            ResponseEntity<ApprovalPageResponse> response = restTemplate.getForEntity(
                    url, ApprovalPageResponse.class);

            ApprovalPageResponse body = response.getBody();
            if (body == null || body.getContent() == null) {
                return Collections.emptyList();
            }

            log.info("승인 내역 조회 완료 - date: {}, page: {}, 조회 건수: {}, 마지막 페이지: {}",
                    date, page, body.getContent().size(), body.isLast());

            return body.getContent();

        } catch (Exception e) {
            log.error("승인 내역 조회 실패 - date: {}, page: {}, error: {}", date, page, e.getMessage());
            throw new RuntimeException("승인 서비스 호출 실패: " + e.getMessage(), e);
        }
    }
}