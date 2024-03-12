package com.bsl.dao.vote;

import lombok.Data;

@Data
public class VoteBasicMsg {
    /**
     * 节点的实际编号
     */
    private int nodeId;
    /**
     * 这个代表了节点的决定
     */
    private boolean vote;

    public VoteBasicMsg() {
        this.nodeId = -1;
        this.vote = false;
    }

    public VoteBasicMsg(int nodeId, boolean vote) {
        this.nodeId = nodeId;
        this.vote = vote;
    }
}
