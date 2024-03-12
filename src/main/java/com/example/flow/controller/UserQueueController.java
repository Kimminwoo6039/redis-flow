package com.example.flow.controller;

import com.example.flow.dto.AllowUserResponse;
import com.example.flow.dto.AllowedUserResponse;
import com.example.flow.dto.RankNumberResponse;
import com.example.flow.dto.RegisterUserResponse;
import com.example.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {


    private final UserQueueService userQueueService;

    // 등록할수 있는 API path
    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.registerWaitQueue(queue, userId)
                .map(RegisterUserResponse::new);
    }

    // 진입 허용
    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUser(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "count") Long count) {
        return userQueueService.allowUser(queue, count)
                .map(allowed -> new AllowUserResponse(count, allowed));
    }

    // 진입가능한지 확인
    @GetMapping("/allowed")
    public Mono<AllowedUserResponse> isAllowedUser(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId,
            @RequestParam(name = "token") String token
            ) throws NoSuchAlgorithmException {
        return userQueueService.isAllowedByToken(queue, userId,token)
                .map(allowed -> new AllowedUserResponse(allowed));
    }

    // 대기표 확인
    @GetMapping("/rank")
    public Mono<RankNumberResponse> getRank(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.getRank(queue, userId)
                .map(RankNumberResponse::new);
    }

    // 대기열 이탈
    @GetMapping("/touch")
    Mono<?> touch(@RequestParam(name = "queue", defaultValue = "default") String queue,
                  @RequestParam(name = "user_id") Long userId,
                  ServerWebExchange exchange
    ) throws NoSuchAlgorithmException {
        return userQueueService.generateToken(queue, userId).map(token -> {
             exchange.getResponse().addCookie(
                    ResponseCookie
                            .from("user-queue-%s-token".formatted(queue), token)
                            .maxAge(Duration.ofSeconds(300))
                            .path("/")
                            .build()
            );

            return token;
        });
    }

}
