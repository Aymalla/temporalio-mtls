package com.temporal.samples.approval.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public class Result {
    private Boolean success;
    private String content;
    private String error;

    public Result(){}

    public Result(Boolean success, String content){
        this.success = success;
        this.content = content;
    }

    public Result(Boolean success, String content, String error){
        this.success = success;
        this.content = content;
        this.error = error;
    }
    public static Result Succeeded(String content)
    {
        return new Result(true, content);
    }

    public static Result Failed(String content, String error)
    {
        return new Result(false, content);
    }
}
