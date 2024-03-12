package com.bsl.control;
import java.util.concurrent.ConcurrentHashMap;

public class MapTrackerThread extends Thread {
    private final ConcurrentHashMap<String, Boolean> map;
    private final ConcurrentHashMap<String, Boolean> previousMap;

    public MapTrackerThread(ConcurrentHashMap<String, Boolean> map) {
        this.map = map;
        this.previousMap = new ConcurrentHashMap<>(map);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            for (String key : map.keySet()) {
                Boolean previousValue = previousMap.get(key);
                Boolean currentValue = map.get(key);
                if (!previousValue.equals(currentValue)) {
                    System.out.println("出现了！");
                    System.out.println("Key: " + key + ", Previous Value: " + previousValue + ", Current Value: " + currentValue);
                }
            }
            previousMap.clear();
            previousMap.putAll(map);
        }
    }

    public static void main(String[] args) {
        ConcurrentHashMap<String, Boolean> myChoice = new ConcurrentHashMap<>();
        myChoice.put("A", true);
        myChoice.put("B", false);
        myChoice.put("C", true);

        MapTrackerThread trackerThread = new MapTrackerThread(myChoice);
        trackerThread.start();

        // 模拟对map的修改
        myChoice.put("B", true);
        myChoice.put("C", false);

        // 主线程睡眠一段时间
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 停止跟踪线程
        trackerThread.interrupt();
    }
}
