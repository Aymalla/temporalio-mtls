package com.temporal.samples.helloworld.workflows;


public class HelloActivityImpl implements HelloActivity {
    /**
     * send company approval request
     * @param activityContext
     * @return
     */
    @Override
    public String hello(String name) {
        var greeting = "Hello " + name + "!";
        return greeting;
    }

}
