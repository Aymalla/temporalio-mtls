package com.temporal.samples.helloworld.workflows;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface HelloActivity {

    @ActivityMethod
    String hello(String name);
}
