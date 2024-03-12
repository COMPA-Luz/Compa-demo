package com.bsl.p2p.server;

import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.bean.ReplayJson;
import com.bsl.dao.node.Node;
import com.bsl.dao.node.NodeAddress;
import com.bsl.dao.node.NodeBasicInfo;
import com.bsl.dao.pbft.MsgCollection;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.MsgStatus;
import com.bsl.dao.pbft.PbftMsg;
import com.bsl.p2p.client.ClientAction;
import com.bsl.p2p.common.MsgPacket;
import com.bsl.util.ClientUtil;
import com.bsl.util.MsgUtil;
import com.bsl.util.PbftUtil;
import com.alibaba.fastjson.JSON;
import com.bsl.util.ProcessUtil;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.ClientChannelContext;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.bsl.control.UpdateNode.nodeFinder;
import static com.bsl.util.MsgUtil.*;


/**

 *
 * @author: kevin
 * @data: 2020/2/14 下午10:07
 * @description: 服务端的Action
 */
@Slf4j
public class ServerAction {
    private Node node = Node.getInstance();
    private MsgCollection msgCollection = MsgCollection.getInstance();
    /**
     * 单例模式构建action
     */
    private static ServerAction action = new ServerAction();

    private MsgCollection collection = MsgCollection.getInstance();

    public static ServerAction getInstance() {
        return action;
    }

    private ServerAction() {
    }


    /**
     * 对PBFT消息做出回应
     *
     * @param channelContext 谁发送的请求
     * @param msg            消息内容
     */
    public void doAction(ChannelContext channelContext, PbftMsg msg) {
        switch (msg.getMsgType()) {
            case MsgType.CLEANING:
                cleaning(msg);
                break;
            case MsgType.GET_VIEW:
                onGetView(channelContext, msg);
                break;
            case MsgType.CHANGE_VIEW:
                changeView(msg);
                break;
            case MsgType.PRE_PREPARE:
                prePrepare(msg);
                break;
            case MsgType.PREPARE:
                prepare(msg);
                break;
            case MsgType.COMMIT:
                commit(msg);
            case MsgType.CLIENT_REPLAY:
                addClient(msg);
                break;
            default:
                break;
        }
    }

    /**
     * commit阶段
     *
     * @param msg
     */
    private void commit(PbftMsg msg) {
//        System.out.println("来自节点"+ msg.getNode()+"的commit");
        List<Integer> nodeList =  AllNodeCommonMsg.commitMap.getOrDefault(msg.getId(),new ArrayList<>());
        if (!nodeList.contains(msg.getNode())) {
            nodeList.add(msg.getNode());
        } else {
            System.out.println("Duplicate node number: " + msg.getNode() + " for event ID: " + msg.getId());
            return;
        }
        long count = collection.getAgreeCommit().incrementAndGet(msg.getId());
//        long count = collection.getDoCommit().incrementAndGet(msg.getEventId());
//        System.out.println(nodeList.toString());
//        log.info(String.format("接受%s事件数量count：%s", msg.getId(),count));
//        if (count >= AllNodeCommonMsg.getAgreeNum()) {
//        if (count >= AllNodeCommonMsg.quorumSize) {
        int hasDone = AllNodeCommonMsg.eventHasDone.getOrDefault(msg.getId(), 0);
        if (AllNodeCommonMsg.allNodeState.get(msg.getId()).size() >= AllNodeCommonMsg.quorumSize
                && count >= (AllNodeCommonMsg.quorumSize- 1) && hasDone== 1 ) {
            AllNodeCommonMsg.eventHasDone.put(msg.getId(), 0);
            System.out.println("表决结果同步完成,进行放行/拒止裁决");
            System.out.println("阶段四：REPLY");
            //收到三个节点的消息
            MsgUtil.decryptMsg(msg);
            AllNodeCommonMsg.timer.stop();
            boolean result = getResult(msg);

            AllNodeCommonMsg.eventHasDone.put(msg.getId(),2);
//            log.info("最终表决结果为："+ result );
            System.out.println("事件编号"+msg.getId());
            tablePrinter(AllNodeCommonMsg.allNodeState.get(msg.getId()));
            if(!result){
                //如果大家认为应该拒止，则kill
//                log.warn("最终表决结果为：拒止");
                System.out.println("最终表决结果为：拒止");

                if (node.getIndex() == AllNodeCommonMsg.getPriIndex()) {
                    System.out.println("主节点执行裁决------>");
                    switch (AllNodeCommonMsg.test){
                        //测试用
                        case 0:
                            System.out.println("假装拒止完毕");
                            break;

                        case 1:
                            ProcessUtil.terminateProcess(Integer.parseInt(AllNodeCommonMsg.targetPid));
                            break;

                        default:
                            ProcessUtil.COMPAfeedbackProcess(result);
                            break;
                    }
                }
//                ProcessUtil.testTerminateProcess(Integer.parseInt(AllNodeCommonMsg.targetPid));

            }else {
//                log.info("最终表决结果为：放行");
                if(node.getIndex() == AllNodeCommonMsg.getPriIndex() && AllNodeCommonMsg.test == -1) {
                    ProcessUtil.COMPAfeedbackProcess(result);
                }
                System.out.println("最终表决结果为：放行");
            }
//            System.out.println(StopWatch.startTime+ "  到 为止  "  +StopWatch.endTime);
//            log.info("总计用时："+ AllNodeCommonMsg.timer.getElapsedTime() + "毫秒");
            System.out.println("总计用时："+ AllNodeCommonMsg.timer.getElapsedTime() + "毫秒");
            //开始进行节点清洗
            if(AllNodeCommonMsg.onOrOFF){
                System.out.println("[互锁保护]: 开启 ---> 运行恶意节点发现与清洗模块");
                nodeFinder(msg);
            }else {
                System.out.println("[互锁保护]: 关闭，事件结束");
//                log.info("表决情况: "+ JSON.toJSONString(AllNodeCommonMsg.allNodeState.get(msg.getId())));
            }

//            log.info(JSON.toJSONString(AllNodeCommonMsg.allNodeState.get(msg.getId())));


            //更新试图，每次操作完毕view+1；
//            AllNodeCommonMsg.increment();
//            AllNodeCommonMsg.view = msg.getViewNum();
//            System.out.println("最新主节点："+ AllNodeCommonMsg.view);
            if (node.getIndex() == AllNodeCommonMsg.getPriIndex()) {
                System.out.println("当前主节点["+ node.getIndex() + "]进行视图更新");
                PbftMsg viewMsg = new PbftMsg(MsgType.CHANGE_VIEW, 0);
                viewMsg.setBody("view update");
                ClientUtil.changeView(viewMsg);
            }else{
                log.error("该节点非主节点，不能发送更新视图消息");
            }

            collection.getAgreeCommit().remove(msg.getId());
//            collection.getDoCommit().remove(msg.getEventId());
            PbftUtil.save(msg);
        }
    }

    /**
     * 节点将prepare消息进行广播然后被接收到
     *
     * @param msg
     */
    private void prepare(PbftMsg msg) {
        if (!msgCollection.getVotePrePrepare().contains(msg.getId()) || !PbftUtil.checkMsg(msg)) {
//            log.info(msgCollection.getVotePrePrepare().contains(msg.getId()) + ">>>>");
            return;
        }

//        long count = collection.getAgreePrepare().incrementAndGet(msg.getId());
//        log.info(String.format("server接受到节点[%s]的%s消息 ", msg.getNode(),MsgStatus.fromCode(msg.getMsgType())));
//        System.out.println("接收到来自节点["+ msg.getNode() + "]的"+ MsgStatus.fromCode(msg.getMsgType()) + "类型消息");

        //本地[<1,true>,<0,true>,<>,<>]
        //收到
        //收到[<0,true>,<3,true>,<>,<>]
        updateVoteMsg(msg);
        //本地[<1,true>,<0,true>,<2,true>,<>]
        //本地[<1,true>,<0,true>,<2,true>,<3,true>]
        //满了再广播广播[<1,true>,<0,true>,<2,true>,<>]
//        if (count >= AllNodeCommonMsg.getAgreeNum()) {
//        System.out.println(msg.getNode()+ "节点到此一游");


        System.out.println( "当前列表:"+JSON.toJSON(msg.getNodeStates()));


//        System.out.println( "当前手中的列表大小为"+AllNodeCommonMsg.getSizeOfAllNodeState(msg.getEventId() ));
        int hasDone = AllNodeCommonMsg.eventHasDone.getOrDefault(msg.getId(), 0);
        if (AllNodeCommonMsg.getSizeOfAllNodeState(msg.getId()) >= AllNodeCommonMsg.quorumSize && hasDone == 0 ) {
            AllNodeCommonMsg.haveUpdate = false;
            // [<0,true>,<1,true>,<2,true>,<3,true>]
//            log.info("数据符合，发送commit操作");
            System.out.println("结果统计完毕,数据符合要求");
            //更新事件状态
            AllNodeCommonMsg.eventHasDone.put(msg.getId(), 1);
            countTrueFalse(msg);
            //passOrNot中存储本节点的统计结果
            collection.getVotePrePrepare().remove(msg.getId());
            collection.getAgreePrepare().remove(msg.getId());

            // 进入Commit阶段f
            System.out.println("阶段三：COMMIT");

            msg.setMsgType(MsgType.COMMIT);
            try {
                collection.getMsgQueue().put(msg);
                ClientAction.getInstance().doAction(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    /**
     * 通知节点列表更新
     */
    private void cleaning(PbftMsg msg) {
        log.info(String.format("server接受到node[%s]%s消息 ", msg.getNode(), MsgStatus.fromCode(msg.getMsgType())));
        AllNodeCommonMsg.activeNodeList = msg.getActiveNodeList();
        AllNodeCommonMsg.backupNodeList = msg.getBackupNodeList();
        AllNodeCommonMsg.allNodeState.put(msg.getId(),msg.getNodeStates());
//        log.info(String.format("当前活动节点列表：%s，备份节点列表：%s",
//                Arrays.toString(AllNodeCommonMsg.activeNodeList.toArray()),Arrays.toString(AllNodeCommonMsg.backupNodeList.toArray()) ));
        nodePrinter();
    }
    /**
     * 主节点发送过来的pre_prepare消息
     *
     * @param msg
     */
    private void prePrepare(PbftMsg msg) {

//        log.info(String.format("server接受到node[%s]%s消息 ", msg.getNode(), MsgStatus.fromCode(msg.getMsgType())));
//        System.out.println("接收到来自节点["+ msg.getNode() + "]的"+ MsgStatus.fromCode(msg.getMsgType()) + "类型消息");
        //初始化一个事件
        AllNodeCommonMsg.eventHasDone.put(msg.getId(), 0);
//        System.out.println("消息内容: " + msg.getNodeStates().toString());
        AllNodeCommonMsg.timer.start();
        msgCollection.getVotePrePrepare().add(msg.getId());
        if (!PbftUtil.checkMsg(msg)) {
            return;
        }
        System.out.println("阶段二：PREPARE");
        msg.setMsgType(MsgType.PREPARE);
        try {
            //本地[<1,true>,<>,<>,<>]
            //收到[<0,true>,<>,<>,<>]
//            initUpdateVoteMsg(msg);
            updateVoteMsg(msg);
            //本地[<1,true>,<0,true>,<>,<>]
            //广播[<1,true>,<0,true>,<>,<>]
            msgCollection.getMsgQueue().put(msg);
//            System.out.println(node.getIndex()+ "节点，要广播的是"+JSON.toJSON(msg.getNodeStates()));
            ClientAction.getInstance().doAction(null);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重新设置view
     *
     * @param msg
     */
    private void changeView(PbftMsg msg) {
        int hasDone = AllNodeCommonMsg.eventHasDone.getOrDefault(msg.getId(), 3);
        if(hasDone != 3){
            return;
        }
        AllNodeCommonMsg.eventHasDone.put(msg.getId(),4);
        node.setViewOK(true);
        AllNodeCommonMsg.view = msg.getViewNum();
        System.out.println("更新后主节点："+ AllNodeCommonMsg.activeNodeList.get(msg.getViewNum()));
//            log.info("视图变更:" + AllNodeCommonMsg.view);
//        if (node.isViewOK()) {
//            return;
//        }
//        long count = collection.getViewNumCount().incrementAndGet(msg.getViewNum());
//
//        if (count >= AllNodeCommonMsg.getAgreeNum() && !node.isViewOK()) {
//            collection.getViewNumCount().clear();
//            node.setViewOK(true);
//            AllNodeCommonMsg.view = msg.getViewNum();
//            log.info("视图变更完成OK");
//        }
    }

    /**
     * 添加未连接的结点
     *
     * @param msg
     */
    private void addClient(PbftMsg msg) {
        if (!ClientUtil.haveClient(msg.getNode())) {
            String ipStr = msg.getBody();
            ReplayJson replayJson = JSON.parseObject(ipStr, ReplayJson.class);
            ClientChannelContext context = ClientUtil.clientConnect(replayJson.getIp(), replayJson.getPort());

            NodeAddress address = new NodeAddress();
            address.setIp(replayJson.getIp());
            address.setPort(replayJson.getPort());
            NodeBasicInfo info = new NodeBasicInfo();
            info.setIndex(msg.getNode());
            info.setAddress(address);
            // 添加ip地址
            AllNodeCommonMsg.allNodeAddressMap.put(msg.getNode(), info);
            AllNodeCommonMsg.publicKeyMap.put(msg.getNode(), replayJson.getPublicKey());

            if(AllNodeCommonMsg.activeNodeList.contains(null)){
                // 如果列表中有 null，替换最前面的那个 null
                //活动节点列表（ActiveNodeList）：这个数据结构用于跟踪当前活动的节点。
                //逻辑编号就是list中的位置 0 1 2 3
                //    存储对应实际节点编号 A B C D
                int index = AllNodeCommonMsg.activeNodeList.indexOf(null);
                AllNodeCommonMsg.activeNodeList.set(index, msg.getNode());
            }else if(AllNodeCommonMsg.activeNodeList.size() < AllNodeCommonMsg.quorumSize){
                AllNodeCommonMsg.activeNodeList.add(msg.getNode());
            }else{
                //备份节点列表（BackupNodeList）：这个数据结构用于存储备份节点的信息。
                //存储实际编号 A B C D
                AllNodeCommonMsg.backupNodeList.add(msg.getNode());
            }

            log.info(String.format("节点%s添加ip地址：%s:%s", msg.getNode(), info.getAddress().getIp(),info.getAddress().getPort()));
//            log.info(String.format("有新节点加入网络，当前活动节点列表：%s，备份节点列表：%s",
//                    Arrays.toString(AllNodeCommonMsg.activeNodeList.toArray()),Arrays.toString(AllNodeCommonMsg.backupNodeList.toArray()) ));
            nodePrinter();
            //打印网络


            if (context != null) {
                // 添加client
                ClientUtil.addClient(msg.getNode(), context);
            }
        }
    }


    /**
     * 将自己的view发送给client
     *
     * @param channelContext
     * @param msg
     */
    private void onGetView(ChannelContext channelContext, PbftMsg msg) {
        int fromNode = msg.getNode();
        // 设置消息的发送方
        msg.setNode(node.getIndex());
        // 设置消息的目的地
        msg.setToNode(fromNode);
//        log.info(String.format("回复节点[%s]的视图请求", msg.getNode()));
        msg.setOk(true);
        msg.setViewNum(AllNodeCommonMsg.view);
        MsgUtil.signMsg(msg);
        String jsonView = JSON.toJSONString(msg);
        MsgPacket msgPacket = new MsgPacket();
        try {
            msgPacket.setBody(jsonView.getBytes(MsgPacket.CHARSET));
            Tio.send(channelContext, msgPacket);
        } catch (UnsupportedEncodingException e) {
            log.error(String.format("server结点发送view消息失败%s", e.getMessage()));
        }
    }
}
