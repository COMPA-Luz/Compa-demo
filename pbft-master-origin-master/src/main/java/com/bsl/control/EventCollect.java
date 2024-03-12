package com.bsl.control;

import lombok.Data;

import java.util.ArrayList;

/**
 * @ClassName EventCollect
 * @Description 事件，裁决列表，裁决结果，同意计数，反对计数
 * @Author ty
 * @Date 2023/5/12 17:31
 * @Version 1.0
 **/
@Data
public class EventCollect {
    private String eventId;
    private ArrayList<String> decisions;
    private String myDecision;
    private int AgreeCount;
    private int DisAgreeCount;

    public EventCollect() {
        this.decisions = new ArrayList<>();
    }

    public int getAgreeCount() {
        int countOne = 0;
        for (String s : decisions) {
            if (s.equals("1")) {
                countOne++;
            }
        }
        return countOne;
    }

    public int getDisAgreeCount() {
        int countZero = 0;
        for (String s : decisions) {
            if (s.equals("0")) {
                countZero++;
            }
        }
        return countZero;
    }
}
