package com.example.flow.controller;

import com.example.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class WaitingRoomController {

    private final UserQueueService userQueueService;

    @GetMapping("/waiting-room")
    Mono<Rendering> waitRoomPage(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId,
            @RequestParam(name = "redirect_url") String redirect_url
    ) {

        // 대기 등록
        // 웹페이지 필요한 데이터를 전달


        // 1. 입장이 허용되어 page redirect(이동)이 가능한 상태인가 ?
        // 2. 어디로 이동해야 하는가 ?
        return userQueueService.isAllowed(queue, userId) // 순번을 다 기다린 상태
                .filter(allowed -> allowed) //진입가능상태
                .flatMap(allowed -> Mono.just(Rendering.redirectTo(redirect_url).build())) // 메인홈페이지로 이동
                .switchIfEmpty( // 진입이 불가능한상태는 , > 대기열에 등록이 안되어있는상태
                        userQueueService.registerWaitQueue(queue, userId) // 대기열에 등록해준다
                                .onErrorResume(ex -> userQueueService.getRank(queue, userId)) // 이미 대기열에 등록되서 대기표 번호를 받는다
                                .map(rank -> Rendering.view("/waiting-room.html") // 다시 페이지로 이동한다.
                                        .modelAttribute("number", rank)
                                        .modelAttribute("userId", userId)
                                        .modelAttribute("queue", queue)
                                        .build()
                                )
                );

    }
}
