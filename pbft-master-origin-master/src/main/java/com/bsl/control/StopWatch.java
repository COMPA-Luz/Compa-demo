package com.bsl.control;

/**
 * @ClassName StopWatch
 * @Description 耗时统计
 * @Author ty
 * @Date 2023/5/12 23:30
 * @Version 1.0
 **/
public class StopWatch {
    public static long startTime;
    public static long endTime;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return endTime - startTime;
    }
}
