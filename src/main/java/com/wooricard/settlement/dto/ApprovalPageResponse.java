package com.wooricard.settlement.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 승인 서비스의 Page 응답을 받기 위한 래퍼 DTO
 *
 * 승인 서비스가 Page<ApprovalDto> 로 응답하면 JSON이 이렇게 옴:
 * {
 *   "content": [ {...}, {...} ],
 *   "totalPages": 1,
 *   "totalElements": 15,
 *   ...
 * }
 * content 필드만 꺼내서 사용
 */
@Data
@NoArgsConstructor
public class ApprovalPageResponse {

    private List<ApprovalDto> content;
    private int totalPages;
    private long totalElements;
    private boolean last;  // true면 마지막 페이지
}