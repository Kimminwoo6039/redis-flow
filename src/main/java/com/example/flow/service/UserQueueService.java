package com.example.flow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static com.example.flow.exception.ErrorCode.QUEUE_ALREADY_REGISTERED_USER;

@Service
@RequiredArgsConstructor
public class UserQueueService {

    private final ReactiveRedisTemplate<String,String> reactiveRedisTemplate;

    // 대기열 등록
    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    
    // 허용인원 프로세스
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";
    

    // 대기열 등록 API
    public Mono<Long> registerWaitQueue(final String queue,final Long userId) {
        // redis sortedset
        // - key : userId
        // - value : unix timestamp ( 먼저 접속한사람이 높은 순서)
        // rank ( 몇번째 대기중이야 )
        var unixTimestamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString(), unixTimestamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(QUEUE_ALREADY_REGISTERED_USER.build()))
                .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString()))
                .map(i -> i >= 0 ? i + 1 : i);
    }


    // 진입 허용
    // count == 몇명의 사람을 허용할꺼냐 ?
    public Mono<Long> allowUser(final String queue,final Long count) {
        // 진입을 허용하는 단계
        // 1. wait queue 에 사용자를 제거
        // 2. proceed queue 사용자를 추가
        var unixTimestamp = Instant.now().getEpochSecond();
       return reactiveRedisTemplate.opsForZSet().popMin(USER_QUEUE_WAIT_KEY.formatted(queue),count)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_PROCEED_KEY.formatted(queue), member.getValue() , unixTimestamp))
               .count();
    }

    // 진입이 가능한 상태 인지 조회
    public Mono<Boolean> isAllowed(final String queue,final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_PROCEED_KEY.formatted(queue),userId.toString())
                .defaultIfEmpty(-1L) // 빈값일 경우 defalut -1L
                .map(rank -> rank >= 0 ? true : false);
    }


    // 대기번호 발급
    public Mono<Long> getRank(final String queue,final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue),userId.toString())
                .defaultIfEmpty(-1L) // 빈값일 경우 defalut -1L
                .map( rank -> rank >=0 ? rank +1 : rank); // 0번째부터 시작되니깐  +1 해줘야함
    }
}
