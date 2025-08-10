package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {


    // 실제 Table내부에 있는 Map<>을 활용하기 위해, 실제 객체를 사용
    UserPointTable userPointTable = new UserPointTable();  // 실제 객체
    PointHistoryTable pointHistoryTable = new PointHistoryTable();  // 실제 객체
    PointService pointService = new PointService(userPointTable, pointHistoryTable);

    @Test
    @DisplayName("존재하지 않는 유저 포인트 조회시 기본 객체가 반환된다.")
    void userPoint_get_fail() {
        // given: Stub 객체 생성 및 상태 설정
        long id = 1;

        // when
        UserPoint userPoint = pointService.getUserPoint(id);

        // then
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(1);
        assertThat(userPoint.point()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 유저 포인트 내역 조회시 빈 리스트가 반환된다.")
    void get_notExist_userPoint() {
        // given
        long userId = 1;

        // when
        List<PointHistory> pointHistories = pointService.getUserPointHistories(userId);

        // then
        assertThat(pointHistories).isEmpty();
    }

    @Test
    @DisplayName("포인트 테이블 에러 발생시 정상적으로 에러를 반환한다.")
    void userPoint_charge_fail_whenPointTableReturnNull() {
        // given: Stub 객체 생성 및 상태 설정
        long id = 1, amount = 1;
        UserPointTable tempUserPointTable = mock(UserPointTable.class);
        PointHistoryTable tempPointHistoryTable = mock(PointHistoryTable.class);
        PointService tempPointService = new PointService(tempUserPointTable, tempPointHistoryTable);

        // 포인트 테이블에서 충전시 null을 반환할 것이라고 가정
        when(tempUserPointTable.selectById(id)).thenReturn(UserPoint.empty(id)); // mock 객체는 아무 동작을 하지않고 기본값을 반환하기 때문에 미리 설정 필요
        when(tempUserPointTable.insertOrUpdate(id, amount)).thenThrow(new RuntimeException("DB 오류 발생"));

        // when & then
        assertThrows(RuntimeException.class, () -> tempPointService.chargeUserPoint(id, amount));
    }

    @Test
    @DisplayName("음수의 포인트 충전 시 에러가 발생한다.")
    void userPoint_charge_match() {
        // given
        long id = 1, amount = -1000;

        // when
        UserPoint userPoint = pointService.chargeUserPoint(id, amount);

        // then
        assertThrows(RuntimeException.class, () -> pointService.chargeUserPoint(id, amount));
    }

    @Test
    @DisplayName("계좌보다 높은 포인트 사용 시 사용이 실패한다.")
    void userPoint_use_fail_over_usePoint() {
        // given
        long id = 1, amount = 1000;

        // when & then
        assertThrows(RuntimeException.class, () -> pointService.useUserPoint(id, amount));
    }

}
