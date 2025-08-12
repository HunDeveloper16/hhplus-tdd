package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final ReentrantLock lock = new ReentrantLock();

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
     * @param userId 유저 아이디
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
        long updatedPoints;

        if(amount < 0) {
            log.error("음수의 포인트는 충전될 수 없습니다. 요청 포인트:{}", amount);
            throw new CustomException("음수의 포인트는 충전될 수 없습니다.");
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
            throw new CustomException("포인트 충전 중 DB에러 발생.");
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
     *
     * @return 사용 이후 유저 포인트 정보
     *
     * 동시성 문제 :
     *
     * 유저가 두번의 포인트를 사용한다고 가정합니다.
     * 유저는 총 1,000포인트를 가지고 있고 첫 번째 시도에선 500포인트를, 다음 시도에선 1000포인트를 사용하고자 합니다.
     *
     * 1.첫번째 시도에서 유저는 포인트 검증을 완료하고 insertOrUpdate()에서 3초의 시간을 sleep합니다.
     * 2.바로 다음 두번째 시도에서 위 3초가 지나기전 포인트 검증을 완료하고, insertOrUpdate() 또 시도하려합니다.
     * 3.따라서 모두 검증을 완료한 뒤 유저의 최종 포인트는 -500 포인트가 되어있을 것입니다.
     *
     * 이에 따라, ReentrantLock을 사용하여 문제가 될 수 있는 로직에서 lock을 걸고자 합니다.
     * lock()내부에는 최소한의 문제가 될 수 있는 로직만 포함해야 하기 때문에 내역을 쌓는 로직은 제외하였습니다.
     *
     */
    public UserPoint useUserPoint(long id, long amount) {
        if(amount < 0) {
            log.error("음수의 포인트는 사용될 수 없습니다. 요청 포인트: {}", amount);
            throw new CustomException("음수의 포인트는 사용될 수 없습니다.");
        }

        UserPoint updatedUserPoint;

        lock.lock();
        try{
            // 1.유저 조회
            UserPoint userPoint = userPointTable.selectById(id);
            // 사용 이후 포인트 계산
            long updatedPoints = userPoint.point() - amount;

            if(userPoint.point() - amount < 0){
                log.error("소유한 포인트보다 사용 포인트가 더 큽니다. 현재 계좌: {} , 사용 포인트: {}", userPoint.point(), amount);
                throw new CustomException("소유한 포인트보다 사용 포인트가 더 큽니다.");
            }

            // 2. 유저 포인트 갱신
            try{
                updatedUserPoint = userPointTable.insertOrUpdate(id, updatedPoints);
            }catch (Exception e){
                log.error("포인트 사용 중 에러가 발생하였습니다.");
                throw new CustomException("포인트 충전 중 DB에러 발생.");
            }
        } finally {
            lock.unlock();
        }

        // 3. 내역 추가
        pointHistoryTable.insert(id, updatedUserPoint.point(), TransactionType.USE, System.currentTimeMillis());

        return updatedUserPoint;

    }


}
