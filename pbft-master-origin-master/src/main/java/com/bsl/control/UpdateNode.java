package com.bsl.control;

import com.alibaba.fastjson.JSON;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.bean.ReplayJson;
import com.bsl.dao.node.Node;
import com.bsl.dao.node.NodeAddress;
import com.bsl.dao.node.NodeBasicInfo;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.PbftMsg;
import com.bsl.dao.vote.VoteBasicMsg;
import com.bsl.p2p.P2PConnectionMsg;
import com.bsl.util.ClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.ClientChannelContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.bsl.util.MsgUtil.getResult;
import static com.bsl.util.MsgUtil.nodePrinter;

/**
 * @ClassName UpdateNode
 * @Description 备用节点队列初始化，恶意节点发现，节点清洗
 * @Author ty
 * @Date 2023/5/12 15:43
 * @Version 1.0
 **/
@Slf4j
public class UpdateNode {

    //初始化备用节点队列
    public static void initBackup(){
        String s = "{\"port\":18080,\"ip\":\"127.0.0.1\",\"index\":0,\"publicKey\":\"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCQR1AmHL3kqIWOdJa+BTL/4C+8ClcR6pgrgrEAkGvnfLWGhl4V363VsNBb8mB+OUDwocz7UNULnDvM2aRH109uas/tSebpz3P1GZhtIkLkAvB+aUd/COKRba5RMwcyNzJStTvQIxQS025oWI5Fm08/8jY1QT/Jjho7T9oCYmgQKwIDAQAB\"}";
        ReplayJson replayJson = JSON.parseObject(s, ReplayJson.class);
        ConcurrentHashMap<Integer, NodeBasicInfo> backUpNodesMap = new ConcurrentHashMap<>(2 << 10);
        /**
         * 所有节点公钥
         */
        Map<Integer,String> backUpNodepublicKeyMap = new ConcurrentHashMap<>(2<<10);
        NodeBasicInfo nodeBasicInfo = new NodeBasicInfo();
        NodeAddress nodeAddress = new NodeAddress();
        nodeAddress.setIp(replayJson.getIp());
        nodeAddress.setPort(replayJson.getPort());
        nodeBasicInfo.setAddress(nodeAddress);
        nodeBasicInfo.setIndex(replayJson.getIndex());
        backUpNodesMap.put(replayJson.getIndex(), nodeBasicInfo);
        backUpNodepublicKeyMap.put(replayJson.getIndex(), replayJson.getPublicKey());


    }
    /**
     * 发现恶意节点
     */
    public static void nodeFinder(PbftMsg msg){
        List<VoteBasicMsg> list= AllNodeCommonMsg.allNodeState.get(msg.getId());
//        log.info("对于容器进程: "+msg.getEventId()+"的表决结果: "+JSON.toJSONString(list));
        boolean result = msg.isPassOrNot();
        for (VoteBasicMsg vote : list) {
             if(vote.isVote() != result){
//                 log.info(String.format("节点[%s]判断结果[%s]不一致,执行清洗",vote.getNodeId(), vote.isVote()));
                 System.out.println("节点["+vote.getNodeId()+"]判断与最终结果不一致,识别为可疑节点,执行清洗");
                 nodeCleaner(vote.getNodeId(), msg.getId());
                 return;
             }
        }
//        log.info("所有节点功能正常");
        System.out.println("检查完毕：所有节点功能正常");

    }

    /**
     * 节点清洗
     */
    public static void nodeCleaner(int nodeId,String id){
//        log.info(String.format("节点[%s]判断结果不一致,开始清洗", nodeId));
        if(nodeId == 0){
            return;
        }
        //掉线的节点实际编号ABCD
        //是否在备份节点中
//        int backupIndex = AllNodeCommonMsg.backupNodeList.indexOf(nodeId);
        //掉线的节点逻辑编号0123
        int activeIndex = AllNodeCommonMsg.activeNodeList.indexOf(nodeId);
        // 是活跃节点
        if(AllNodeCommonMsg.backupNodeList.size() >0){
            // 从备份节点列表取出节点
            int backupNode = AllNodeCommonMsg.backupNodeList.remove(0);
//            log.info(String.format("节点[%s]离线，使用备份节点%s替换", nodeId,backupNode));
            System.out.println(String.format("节点[%s]离线，使用备份节点%s替换", nodeId,backupNode));
            AllNodeCommonMsg.timer.start();
            AllNodeCommonMsg.activeNodeList.set(activeIndex,backupNode);
        }else{
            AllNodeCommonMsg.activeNodeList.set(activeIndex,null);
//            log.warn("节点数量不满足最低要求");
            System.out.println("节点数量不满足最低要求");
        }
//        log.info(String.format("更新活动节点列表：%s，更新备份节点列表：%s",
//                Arrays.toString(AllNodeCommonMsg.activeNodeList.toArray()),Arrays.toString(AllNodeCommonMsg.backupNodeList.toArray()) ));
//        System.out.println("更新活动节点列表："+Arrays.toString(AllNodeCommonMsg.activeNodeList.toArray())
//                  +"，更新备份节点列表：" + Arrays.toString(AllNodeCommonMsg.backupNodeList.toArray()));
        nodePrinter();
        //打印网络


        //广播告诉节点5你被更新了
        Node node = Node.getInstance();
        if (node.getIndex() == AllNodeCommonMsg.getPriIndex()) {
            System.out.println("主节点开始转发被更新消息");
            PbftMsg msg = new PbftMsg(MsgType.CLEANING, 0);
            msg.setBody("clean done");
            msg.setNodeStates(AllNodeCommonMsg.allNodeState.get(id));
            msg.setActiveNodeList(AllNodeCommonMsg.activeNodeList);
            msg.setBackupNodeList(AllNodeCommonMsg.backupNodeList);
            ClientUtil.cleanNode(msg);
        }

        AllNodeCommonMsg.timer.stop();
        System.out.println(StopWatch.startTime+ "  到 为止  "  +StopWatch.endTime );
        log.info("总计用时："+ AllNodeCommonMsg.timer.getElapsedTime() + "毫秒");

//

        /**
         * 假如中断的是主结点
         */
//        if (channelContext.equals(P2PConnectionMsg.CLIENTS.get(AllNodeCommonMsg.getPriIndex()))){
//            log.warn("主节点链接失败，决定发起视图选举");
//            node.setViewOK(false);
//            PbftMsg msg = new PbftMsg(MsgType.CHANGE_VIEW,node.getIndex());
//            msgQueue.put(msg);
//            action.doAction(channelContext);
//        }
    }
    //替换恶意节点
    public static void replaceNode(int index) {
        String ip = "";
        int port = 100;
        int replaced = 100;
        log.info(String.format("节点%d尝试链接%s", ip, port));


        ClientChannelContext context = ClientUtil.clientConnect(ip, port);
        P2PConnectionMsg.CLIENTS.put(replaced, context);

        if (context != null) {
            // 将通道进行保存
            ClientUtil.addClient(replaced, context);
        } else {
            log.warn(String.format("节点%d-->%s：%d连接失败", replaced, ip, port));
        }

        log.info("client链接服务端的数量" + P2PConnectionMsg.CLIENTS.size());
    }

    public static void main(String[] args) {
        initBackup();
    }


}
