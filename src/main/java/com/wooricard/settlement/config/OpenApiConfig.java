package com.wooricard.settlement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger 설정
 * Card Settlement Service의 API 문서를 자동 생성합니다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI settlementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Card Settlement Service API")
                        .description("카드 정산 서비스 API 문서\n\n" +
                                "## 주요 기능\n" +
                                "- 정산 처리: 승인/결제 서비스에서 승인 내역을 조회하여 정산 집계\n" +
                                "- 정산 내역 조회: 날짜/상태 기준 정산 결과 조회\n" +
                                "- 정산 상태 조회: 특정 정산 건의 처리 상태 확인\n\n" +
                                "## 정산 상태 코드\n" +
                                "- `PENDING`: 정산 대기\n" +
                                "- `IN_PROGRESS`: 정산 처리 중\n" +
                                "- `COMPLETED`: 정산 완료\n" +
                                "- `FAILED`: 정산 실패\n\n" +
                                "## 처리 흐름\n" +
                                "1. 정산 트리거 (배치 스케줄러 또는 수동 API 호출)\n" +
                                "2. 승인/결제 서비스에서 대상 승인 내역 조회\n" +
                                "3. 정산 집계 후 settlement_db 저장\n" +
                                "4. 정산 결과 조회 API로 확인")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Card Payment System")
                                .email("support@cardpayment.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("정산 서버")
                ));
    }
}