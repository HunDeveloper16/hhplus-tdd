package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    UserPointTable userPointTable;

    @Mock
    PointHistoryTable pointHistoryTable;

    @InjectMocks
    PointService pointService;

    @Test
    @DisplayName("존재하지 않는 유저 포인트 조회시 기본 객체가 반환된다.")
    void getUserPoint_whenUserDoesNotExist_returnsDefaultObject() {
        // given: Stub 객체 생성 및 상태 설정
        long id = 1, point = 0;
        UserPoint expectedUserPoint = new UserPoint(id,point,System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(expectedUserPoint);

        // when
        UserPoint actualUserPoint = pointService.getUserPoint(id);

        // then
        assertEquals(expectedUserPoint, actualUserPoint);
    }

    @Test
    @DisplayName("존재하지 않는 유저 포인트 내역 조회시 빈 리스트가 반환된다.")
    void getUserPointHistories_whenUserDoesNotExist_returnsEmptyList() {
        // given
        long userId = 1;
        List<PointHistory> expectedPointHisries = Collections.emptyList();

        // when
        List<PointHistory> actualPointHistories = pointService.getUserPointHistories(userId);

        // then
        assertEquals(expectedPointHisries, actualPointHistories);
        assertThat(actualPointHistories).isEmpty();
    }

    @Test
    @DisplayName("포인트 테이블 에러 발생시 정상적으로 에러를 반환한다.")
    void chargeUserPoint_whenPointTableThrowsException_throwsRuntimeException() {
        // given: Stub 객체 생성 및 상태 설정
        long id = 1, amount = 1;
        UserPointTable tempUserPointTable = mock(UserPointTable.class);
        PointHistoryTable tempPointHistoryTable = mock(PointHistoryTable.class);
        PointService tempPointService = new PointService(tempUserPointTable, tempPointHistoryTable);

        // 포인트 테이블에서 충전시 null을 반환할 것이라고 가정
        when(tempUserPointTable.selectById(id)).thenReturn(UserPoint.empty(id)); // mock 객체는 아무 동작을 하지않고 기본값을 반환하기 때문에 미리 설정 필요
        when(tempUserPointTable.insertOrUpdate(id, amount)).thenThrow(new CustomException("포인트 충전 중 DB 에러 발생"));

        // when & then
        assertThrows(CustomException.class, () -> tempPointService.chargeUserPoint(id, amount));
    }

    @Test
    @DisplayName("음수의 포인트 충전 시 에러가 발생한다.")
    void chargeUserPoint_whenAmountIsNegative_throwsRuntimeException() {
        // given
        long id = 1, amount = -1000;

        // when & then
        assertThrows(CustomException.class, () -> pointService.chargeUserPoint(id, amount));
    }

    @Test
    @DisplayName("음수의 포인트 사용 시 에러가 발생한다.")
    void useUserPoint_whenAmountIsNegative_throwsRuntimeException() {
        // given
        long id = 1, amount = -1000;

        // when & then
        assertThrows(CustomException.class, () -> pointService.useUserPoint(id, amount));
    }

    @Test
    @DisplayName("계좌보다 높은 포인트 사용 시 사용이 실패한다.")
    void useUserPoint_whenAmountExceedsBalance_throwsRuntimeException() {
        // given
        long id = 1, amount = 1000, useAmount = 2000;

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));

        // when & then
        assertThrows(CustomException.class, () -> pointService.useUserPoint(id, useAmount));
    }

}
