package com.wooricard.settlement.client;

import com.wooricard.settlement.dto.ApprovalDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 승인/결제 서비스 클라이언트 인터페이스
 */
public interface ApprovalClient {

    /**
     * 특정 날짜의 승인 완료 내역 조회 (페이지 단위)
     * Batch ItemReader에서 청크 단위로 호출
     *
     * @param date     조회 대상 날짜
     * @param page     페이지 번호 (0-based)
     * @param size     페이지 크기
     * @return 승인 내역 목록 (빈 리스트면 마지막 페이지)
     */
    List<ApprovalDto> getApprovedByDate(LocalDate date, int page, int size);
}