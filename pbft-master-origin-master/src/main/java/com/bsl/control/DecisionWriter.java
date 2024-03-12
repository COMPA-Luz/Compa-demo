package com.bsl.control;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import com.alibaba.fastjson.JSON;
import lombok.Data;

/**
 * @ClassName TypeWrite
 * @Description 事件、节点的判决结果写入对应文件
 * @Author ty
 * @Date 2023/5/10 23:27
 * @Version 1.0
 **/


public class DecisionWriter {
    private static final String[] filePaths = {
            "node-1.txt",
            "node-2.txt",
            "node-3.txt",
            "node-4.txt"
    };
    static String abs = "D:\\temp_workstation_Luz\\pbft-master-origin-master\\pbft-master-origin-master\\";
    public static String getFilePath(int nodeIndex) {
        if (nodeIndex > 0 && nodeIndex <= filePaths.length) {
            return filePaths[nodeIndex - 1];
        } else {
            throw new IllegalArgumentException("Invalid node index: " + nodeIndex);
        }
    }
    public static void writeDecisionResult(UUID eventId,int nodeIndex, String decisionResult) {
        try {
            // 创建 JSON 对象，将事件ID和判决结果放入
            EventData eventData = new EventData(eventId.toString(), decisionResult);

            // 将 JSON 对象转换为字符串
            String json = JSON.toJSONString(eventData);

            // 写入文件
            FileWriter writer = new FileWriter(abs+filePaths[nodeIndex-1]);
            writer.write(eventData.decisionResult);
//            writer.write(System.lineSeparator()); // 可选：写入换行符
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 内部类，表示事件数据
    @Data
    static class EventData {
        private String eventId;
        private String decisionResult;

        public EventData(String eventId, String decisionResult) {
            this.eventId = eventId;
            this.decisionResult = decisionResult;
        }

        // Getters and setters...
    }

    public static void main(String[] args) {
        // 生成唯一的事件ID
        UUID eventId = UUID.randomUUID();
        writeDecisionResult(eventId,1,"1");
        writeDecisionResult(eventId,2,"1");
        writeDecisionResult(eventId,3,"1");
        writeDecisionResult(eventId,4,"1");
        System.out.println("新事件完成");
    }
}