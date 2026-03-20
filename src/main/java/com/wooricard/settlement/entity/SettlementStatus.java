package com.wooricard.settlement.entity;

/**
 * 정산 상태
 */
public enum SettlementStatus {

    /**
     * 정산 대기 (Job 시작 전 초기 상태)
     */
    PENDING,

    /**
     * 정산 처리 중 (Batch Job 실행 중)
     */
    IN_PROGRESS,

    /**
     * 정산 완료
     */
    COMPLETED,

    /**
     * 정산 실패 (failureReason 필드에 원인 저장)
     */
    FAILED
}