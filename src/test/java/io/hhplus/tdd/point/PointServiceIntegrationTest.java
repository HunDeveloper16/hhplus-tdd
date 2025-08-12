package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

@ExtendWith(MockitoExtension.class)
public class PointServiceIntegrationTest {

    // 실제 Table내부에 있는 Map<>을 활용하기 위해, 실제 객체를 사용
    UserPointTable userPointTable = new UserPointTable();  // 실제 객체
    PointHistoryTable pointHistoryTable = new PointHistoryTable();  // 실제 객체
    PointService pointService = new PointService(userPointTable, pointHistoryTable);

    @Test
    @DisplayName("포인트 충전 및 사용 후 포인트 내역이 정상적으로 조회된다.")
    void getHistories_afterChargeAndUse_success() {
        // given
        long id = 1, firstAmount = 1000, secondAmount = 2000, useAmount = 2500;
        pointService.chargeUserPoint(id, firstAmount);
        pointService.chargeUserPoint(id, secondAmount);
        pointService.useUserPoint(id, useAmount);

        // when
        List<PointHistory> pointHistories = pointService.getUserPointHistories(1);

        // then
        assertThat(pointHistories).isNotEmpty();
        assertThat(pointHistories.size()).isEqualTo(3);
        assertThat(pointHistories)
                .extracting(PointHistory::type, PointHistory::amount)
                .containsExactly(
                        tuple(TransactionType.CHARGE, 1000L),
                        tuple(TransactionType.CHARGE, 3000L),
                        tuple(TransactionType.USE, 500L)
                );

    }


}
