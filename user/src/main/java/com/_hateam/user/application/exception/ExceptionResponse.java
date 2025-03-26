package com._hateam.user.application.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionResponse {

    private String error;
    private String msg;
    private int status;
}
