package com.example.flow.controller;

import com.example.flow.dto.AllowUserResponse;
import com.example.flow.dto.AllowedUserResponse;
import com.example.flow.dto.RankNumberResponse;
import com.example.flow.dto.RegisterUserResponse;
import com.example.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {


    private final UserQueueService userQueueService;

    // 등록할수 있는 API path
    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(
            @RequestParam(name = "queue" , defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.registerWaitQueue(queue,userId)
                .map(RegisterUserResponse::new);
    }

    // 진입 허용
    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUser(
            @RequestParam(name = "queue" , defaultValue = "default") String queue,
            @RequestParam(name = "count") Long count) {
        return userQueueService.allowUser(queue,count)
                .map(allowed -> new AllowUserResponse(count,allowed));
    }

    // 진입가능한지 확인
    @GetMapping("/allowed")
    public Mono<AllowedUserResponse> isAllowedUser(
            @RequestParam(name = "queue" , defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.isAllowed(queue,userId)
                .map(allowed -> new AllowedUserResponse(allowed));
    }

    // 대기표 확인
    @GetMapping("/rank")
    public Mono<RankNumberResponse> getRank(
            @RequestParam(name = "queue" , defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.getRank(queue,userId)
                .map(RankNumberResponse::new);
    }

}
