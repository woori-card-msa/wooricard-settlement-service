package com.wooricard.settlement.client;

import com.wooricard.settlement.dto.ApprovalPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * 승인/결제 서비스 Feign 클라이언트
 * name = Eureka에 등록된 서비스 이름
 */
@FeignClient(name = "wooricard-approval-service")
public interface ApprovalClient {

    @GetMapping("/api/authorizations")
    ApprovalPageResponse getApprovedByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String status
    );
}