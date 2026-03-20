package com.wooricard.settlement.batch;

import com.wooricard.settlement.entity.Settlement;
import com.wooricard.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * 정산 ItemWriter
 *
 * Processor에서 넘어온 Settlement 엔티티를 settlement_db에 저장
 * chunk 단위로 묶어서 한 번에 saveAll() 호출 → DB 왕복 최소화
 */
@Slf4j
@RequiredArgsConstructor
public class SettlementItemWriter implements ItemWriter<Settlement> {

    private final SettlementRepository settlementRepository;

    @Override
    public void write(Chunk<? extends Settlement> chunk) {
        log.info("정산 저장 - {}건", chunk.size());
        settlementRepository.saveAll(chunk.getItems());
        log.info("정산 저장 완료");
    }
}