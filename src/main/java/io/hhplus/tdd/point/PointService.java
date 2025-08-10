package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    /**
     * 유저 포인트를 조회합니다.
     *
     * @param id 유저 아이디
     */
    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    /**
     * 유저 포인트 내역을 조회합니다.
     *
     * @param id 유저 아이디
     */
    public List<PointHistory> getUserPointHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 유저 포인트를 충전합니다.
     *
     * @param id 유저 아이디
     * @param amount 충전 포인트
     *
     * @return 충전 이후 유저 포인트 정보
     */
    public UserPoint chargeUserPoint(long id, long amount) {
        long updatedPoints = amount;

        if(amount < 0) {
            log.error("음수의 포인트는 충전될 수 없습니다. 요청 포인트:{}", amount);
            throw new RuntimeException();
        }

        // 1. 유저 조회
        UserPoint userPoint = userPointTable.selectById(id);

        // 충전 이후 포인트 계산
        updatedPoints = userPoint.point() + amount;

        // 2. 충전
        try{
            userPoint = userPointTable.insertOrUpdate(id, updatedPoints);
        } catch (Exception e){
            log.error("포인트 충전 중 에러가 발생하였습니다.");
            throw new RuntimeException();
        }

        // 3. 내역 추가
        pointHistoryTable.insert(id, updatedPoints, TransactionType.CHARGE, System.currentTimeMillis());

        return userPoint;
    }


    /**
     * 유저 포인트를 사용합니다.
     *
     * @param id 유저 아이디
     * @param amount 사용 포인트
     * @return 사용 이후 유저 포인트 정보
     */
    public UserPoint useUserPoint(long id, long amount) {
        // 1.유저 조회
        UserPoint userPoint = userPointTable.selectById(id);

        // 사용 이후 포인트 계산
        long updatedPoints = userPoint.point() - amount;

        if(userPoint.point() - amount < 0){
            log.error("소유한 포인트보다 사용 포인트가 더 큽니다. 현재 계좌: {} , 사용 포인트: {}", userPoint.point(), amount);
            throw new RuntimeException();
        }

        // 2. 유저 포인트 갱신
        try{
            userPoint = userPointTable.insertOrUpdate(id, updatedPoints);
        }catch (Exception e){
            log.error("포인트 사용 중 에러가 발생하였습니다.");
            throw new RuntimeException();
        }

        // 3. 내역 추가
        pointHistoryTable.insert(id, updatedPoints, TransactionType.USE, System.currentTimeMillis());

        return userPoint;
    }


}
