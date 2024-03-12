package com.bsl.dao.pbft;

import cn.hutool.core.util.IdUtil;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.vote.VoteBasicMsg;
import lombok.Data;

import java.util.*;

/**

 *
 * @author: kevin
 * @data: 2020/1/22 下午3:56
 * @description: 进行Pbft发送的消息、
 */
@Data
public class PbftMsg {
    /**
     * 消息类型
     */
    private int msgType;

    /**
     * 消息发起的结点编号
     */
    private int node;

    /**
     * 消息发送的目的地
     */
    private int toNode;

    /**
     * 消息时间戳
     */
    private long time;
    /**
     * 检测是否通过
     */
    private boolean passOrNot;
    /**
     * 消息体
     */
    private String body;
    /**
     * 节点版本号
     */
    private long globalSequence;
    /**
     * 检测是否通过
     */
    private boolean isOk;

    /**
     * 结点视图
     */
    private int viewNum;

    /**
     * 使用UUID进行生成
     */
    private String id;
    /**
     * 存储表决结果
     */
//    private Map<String, List<VoteBasicMsg>> allNodeState ;
    private List<VoteBasicMsg> nodeStates ;

    private List<Integer> activeNodeList;
    //备份节点列表（BackupNodeList）：这个数据结构用于存储备份节点的信息。
    //存储实际编号 A B C D
    private List<Integer> backupNodeList;
    private String eventId;
    /**
     * 消息的签名
     */
    private String sign;

    private PbftMsg() {
        this.nodeStates = new ArrayList<>();
    }

    public PbftMsg(int msgType, int node) {
        this.msgType = msgType;
        this.node = node;
        this.time = System.currentTimeMillis();
        this.id = IdUtil.randomUUID();
        this.viewNum = AllNodeCommonMsg.view;
        this.nodeStates = new ArrayList<>();
        this.globalSequence = AllNodeCommonMsg.getGlobalSequence();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PbftMsg)) {
            return false;
        }
        PbftMsg msg = (PbftMsg) o;
        return getMsgType() == msg.getMsgType() &&
                getNode() == msg.getNode() &&
                getToNode() == msg.getToNode() &&
                getTime() == msg.getTime() &&
                isOk() == msg.isOk() &&
                getViewNum() == msg.getViewNum() &&
                Objects.equals(getBody(), msg.getBody()) &&
                Objects.equals(getId(), msg.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMsgType(), getBody(), getNode(), getToNode(), getTime(),  getViewNum(), getId());
    }
}
