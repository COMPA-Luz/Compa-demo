package com.bsl.util;

import com.alibaba.fastjson.JSON;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.bean.ReplayJson;
import com.bsl.dao.node.Node;
import com.bsl.dao.pbft.MsgCollection;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.MsgStatus;
import com.bsl.dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientTioConfig;
import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.core.Tio;
import com.bsl.p2p.P2PConnectionMsg;
import com.bsl.p2p.client.ClientAction;
import com.bsl.p2p.client.P2PClientLinstener;
import com.bsl.p2p.client.P2pClientAioHandler;
import com.bsl.p2p.common.Const;
import com.bsl.p2p.common.MsgPacket;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author: kevin
 * @data: 2020/2/15 上午1:12
 * @description: 服务端工具
 */
@Slf4j
public class ClientUtil {

    /**
     * client 的配置
     */
    private static ClientTioConfig clientTioConfig = new ClientTioConfig(
            new P2pClientAioHandler(),
            new P2PClientLinstener(),
            new ReconnConf(Const.TIMEOUT)
    );

    public static ClientChannelContext clientConnect(String ip, int port) {

        clientTioConfig.setHeartbeatTimeout(Const.TIMEOUT);
        ClientChannelContext context;
        try {
            TioClient client = new TioClient(clientTioConfig);
            context = client.connect(new org.tio.core.Node(ip, port), Const.TIMEOUT);
            return context;
        } catch (Exception e) {
            log.error("%s：%d连接错误" + e.getMessage());
            return null;
        }
    }

    /**
     * 添加client到 P2PConnectionMsg.CLIENTS中
     *
     * @param index  结点序号
     * @param client
     */
    public static void addClient(int index, ClientChannelContext client) {
        P2PConnectionMsg.CLIENTS.put(index, client);
    }

    /**
     * 判断该结点是否保存在CLIENTS中
     *
     * @param index
     * @return
     */
    public static boolean haveClient(int index) {
        if (P2PConnectionMsg.CLIENTS.containsKey(index)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * client对所有的server广播
     *
     * @param msg
     */
    public static void clientPublish(PbftMsg msg) {
        // 设置目标消息发送方
        msg.setNode(Node.getInstance().getIndex());
        // 设置消息的目标方 -1 代表all
        msg.setToNode(-1);
//        System.out.println("当前网络内节点"+P2PConnectionMsg.CLIENTS.keySet().toString());
        for (int index : P2PConnectionMsg.CLIENTS.keySet()) {
            //仅向所选网络内节点广播
            //实际编号
            if(msg.getMsgType() == 1 || msg.getMsgType() == 2 ||msg.getMsgType() ==  3 ){
                if(!AllNodeCommonMsg.activeNodeList.contains(index) ){
//                    System.out.println("编号不存在列表:"+ index);
                    continue;
                }
//                System.out.println("编号存在列表:"+ index);
            }
            ClientChannelContext client = P2PConnectionMsg.CLIENTS.get(index);
            // 如果不是CLIENT_REPLAY的消息类型，就进行加密和签名操作
            if (msg.getMsgType() != MsgType.CLIENT_REPLAY && msg.getMsgType() != MsgType.GET_VIEW) {
                if (!MsgUtil.preMsg(index, msg)) {
                    log.error("消息加密失败");
                    return;
                }
            }
            String json = JSON.toJSONString(msg);
            MsgPacket msgPacket = new MsgPacket();
            try {
//                log.info("开始向 index:{}, 发送消息: {{}}", index, msg.getBody());
//                log.info("向node[{}]发送{}消息", index, MsgStatus.fromCode(msg.getMsgType()));
//                System.out.println("向节点["+ index + "]发送"+ MsgStatus.fromCode(msg.getMsgType()) + "类型消息");
//                System.out.println("消息内容: " + msg.getNodeStates().toString());
                msgPacket.setBody(json.getBytes(MsgPacket.CHARSET));
                Tio.send(client, msgPacket);
            } catch (UnsupportedEncodingException e) {
                log.error("数据utf-8编码错误" + e.getMessage());
            }
        }

    }

    /**
     * 将自己的节点ip和公钥消息广播出去
     *
     * @param index
     * @param ip
     * @param port
     */
    public static void publishIpPort(int index, String ip, int port) {
        PbftMsg replayMsg = new PbftMsg(MsgType.CLIENT_REPLAY, index);
        replayMsg.setViewNum(AllNodeCommonMsg.view);
        // 将节点消息数据发送过去
        ReplayJson replayJson = new ReplayJson();
        replayJson.setIp(ip);
        replayJson.setPort(port);
        // 公钥部分
        replayJson.setPublicKey(Node.getInstance().getPublicKey());

        replayMsg.setBody(JSON.toJSONString(replayJson));
        ClientUtil.clientPublish(replayMsg);
//        log.info(String.format("广播%s消息：", MsgStatus.fromCode(replayMsg.getMsgType())));
        System.out.println("广播地址,接入P2P网络");
    }

    /**
     * 主节点进行广播
     *
     * @param msg
     */
    public static void prePrepare(PbftMsg msg) {
        msg.setNode(Node.getInstance().getIndex());
        msg.setToNode(-1);
        msg.setViewNum(AllNodeCommonMsg.view);
        Node node = Node.getInstance();
        if (node.getIndex() != AllNodeCommonMsg.getPriIndex()) {
            log.error("该节点非主节点，不能发送preprepare消息");
            return;
        }
        System.out.println("阶段一：PrePrepare");
        //如果我是主节点，可以发送新消息，整体版本号+1
        //如果不是，版本号不能更新，还处于上一个阶段
        AllNodeCommonMsg.updateGlobalSequence();
        msg.setGlobalSequence(AllNodeCommonMsg.getGlobalSequence());

        msg.setMsgType(MsgType.PRE_PREPARE);
        ClientUtil.clientPublish(msg);
        MsgCollection msgCollection = MsgCollection.getInstance();
        msg.setMsgType(MsgType.PRE_PREPARE);
        msgCollection.getVotePrePrepare().add(msg.getId());
        if (!PbftUtil.checkMsg(msg)) {
            log.error("msg check 消息不通过");
            return;
        }
        try {
            msgCollection.getMsgQueue().put(msg);
            ClientAction.getInstance().doAction(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 主节点进行广播
     *
     * @param msg
     */
    public static void cleanNode(PbftMsg msg) {
        msg.setNode(Node.getInstance().getIndex());
        msg.setToNode(-1);
        msg.setViewNum(AllNodeCommonMsg.view);
        Node node = Node.getInstance();
        if (node.getIndex() != AllNodeCommonMsg.getPriIndex()) {
            log.error("该节点非主节点，不能发送cleanNode消息");
            return;
        }
        msg.setMsgType(MsgType.CLEANING);
        ClientUtil.clientPublish(msg);
        MsgCollection msgCollection = MsgCollection.getInstance();
        msg.setMsgType(MsgType.CLEANING);
        msgCollection.getVotePrePrepare().add(msg.getId());
        if (!PbftUtil.checkMsg(msg)) {
            log.error("msg check 消息不通过");
            return;
        }
        try {
//            log.error("发送更新消息");
            msgCollection.getMsgQueue().put(msg);
            ClientAction.getInstance().doAction(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

    public static void changeView(PbftMsg msg) {
        //更新试图，每次操作完毕view+1；
        AllNodeCommonMsg.increment();
        int id = Node.getInstance().getIndex();
        msg.setNode(id);
        msg.setToNode(-1);
        msg.setViewNum(AllNodeCommonMsg.view);
        ClientUtil.clientPublish(msg);
        System.out.println("更新后主节点："+ AllNodeCommonMsg.activeNodeList.get(msg.getViewNum()));
        MsgCollection msgCollection = MsgCollection.getInstance();
        msg.setMsgType(MsgType.CHANGE_VIEW);
        if (!PbftUtil.checkMsg(msg)) {
            log.error("msg check 消息不通过");
            return;
        }
        try {
//            log.error("发送更新消息");
            msgCollection.getMsgQueue().put(msg);
            ClientAction.getInstance().doAction(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

}
