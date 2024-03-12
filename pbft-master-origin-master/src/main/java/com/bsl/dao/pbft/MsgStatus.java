package com.bsl.dao.pbft;

public enum MsgStatus {
    /**
     * 请求视图
     */
    GET_VIEW(-1),
    /**
     * 变更视图
     */
    CHANGE_VIEW(0),
    /**
     * 预准备阶段
     */
    PRE_PREPARE(1),
    /**
     * 准备阶段
     */
    PREPARE(2),
    /**
     * 提交阶段
     */
    COMMIT(3),
    /**
     * ip消息回复回复阶段
     */
    CLIENT_REPLAY(4),
    /**
     * 节点清洗
     */
    CLEANING(5);

    private int code;

    MsgStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MsgStatus fromCode(int code) {
        for (MsgStatus status : MsgStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
