package com.bsl.control;

import lombok.Data;

/**
 * @ClassName EventData
 * @Description 事件和最终的裁决结果
 * @Author ty
 * @Date 2023/5/12 17:37
 * @Version 1.0
 **/
@Data
class EventData {
    private String eventId;
    private String decisionResult;
//    {"decisionResult":"0","eventId":"98cd0ac7-aff6-49ab-9044-1c5acebddd89"}
    public EventData(String eventId, String decisionResult) {
        this.eventId = eventId;
        this.decisionResult = decisionResult;
    }

    public EventData() {
    }
    public boolean getResult() {
        //如果文件中是0则代表不会拒止
        return decisionResult.equals("0");
    }

// Getters and setters...
}