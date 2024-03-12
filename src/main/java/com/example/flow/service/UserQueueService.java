package com.example.flow.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import static com.example.flow.exception.ErrorCode.QUEUE_ALREADY_REGISTERED_USER;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserQueueService {

    private final ReactiveRedisTemplate<String,String> reactiveRedisTemplate;

    // 대기열 등록
    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    
    // 대기열 인원 스캔 ( 스케쥴링에서 * 짜를거임 )
    private final String USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait";
    
    // 허용인원 프로세스
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";

    @Value("${scheduler.enabled}")
    private Boolean scheudling = false;
    

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

    // 토큰 조회
    public Mono<Boolean> isAllowedByToken(final String queue,final Long userId,String token) throws NoSuchAlgorithmException {
        return this.generateToken(queue,userId)
                .filter(gen -> gen.equalsIgnoreCase(token))
                .map(i -> true)
                .defaultIfEmpty(false);

    }


    // 대기번호 발급
    public Mono<Long> getRank(final String queue,final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue),userId.toString())
                .defaultIfEmpty(-1L) // 빈값일 경우 defalut -1L
                .map( rank -> rank >=0 ? rank +1 : rank); // 0번째부터 시작되니깐  +1 해줘야함
    }

    // 대기열 이탈 토큰생성
    public Mono<String> generateToken(final String queue,final Long userId) throws NoSuchAlgorithmException {
        // sha256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        var input = "user-queue-%s-%d".formatted(queue,userId);
        byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte aByte : encodedHash) {
            hexString.append(String.format("%02x",aByte));
        }

        return Mono.just(hexString.toString());
    }

    @Scheduled(fixedDelay = 10000 , initialDelay = 5000) //서버시작하고 5초 쉬었다가  3초마다 스케쥴링
    public void scheduleAllowUser() {
        if (!scheudling) {
            log.info("passed schedule...");
            return ;
        }
        log.info("called schedule...");

        var maxAllowUserCount = 100L;

        // 사용자를 허용하는 코드 작성
        reactiveRedisTemplate.scan(ScanOptions.scanOptions().match(USER_QUEUE_WAIT_KEY_FOR_SCAN)
                .count(100) // 100명까지 조회
                .build())
                .map(key -> key.split(":")[2])
                .flatMap(queue -> allowUser(queue,maxAllowUserCount).map(allowed -> Tuples.of(queue,allowed)))
                .doOnNext(tuple -> log.info("Tried %d and allowed %d members of %s queue".formatted(maxAllowUserCount,tuple.getT2(),tuple.getT1())))
                .subscribe();

    }
}
