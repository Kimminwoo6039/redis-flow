package com.example.flow.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;


@AllArgsConstructor
public enum ErrorCode {
    QUEUE_ALREADY_REGISTERED_USER(HttpStatus.CONFLICT,"UQ-0001","Already Registered in queue");
   // QUEUE_ALREADY_REGISTERED_US12ER2(HttpStatus.CONFLICT,"UQ-0001","Already Registered in queue in %s");

    private final HttpStatus httpStatus;
    private final String code;
    private String reason;

    public ApplicationException build() {
        return new ApplicationException(httpStatus,code,reason);
    }

    public ApplicationException build(Object ...args) {
        return new ApplicationException(httpStatus,code,reason.formatted(args));
    }
}
