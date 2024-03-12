package com.bsl.p2p.client;

import com.bsl.config.AllNodeCommonMsg;
import com.bsl.control.StopWatch;
import com.bsl.dao.node.Node;
import com.bsl.dao.pbft.MsgCollection;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import com.bsl.p2p.P2PConnectionMsg;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import static com.bsl.util.MsgUtil.nodePrinter;

/**

 *
 * @author: kevin
 * @data: 2020/2/12 下午9:43
 * @description: 客户端监听器
 */
@Slf4j
public class P2PClientLinstener implements ClientAioListener {

    private ClientAction action = ClientAction.getInstance();

    private Node node = Node.getInstance();

    /**
     * 消息队列
     */
    private BlockingQueue<PbftMsg> msgQueue = MsgCollection.getInstance().getMsgQueue();


    /**
     * 建链后触发本方法，注：建链不一定成功，需要关注参数isConnected
     *
     * @param channelContext
     * @param isConnected    是否连接成功,true:表示连接成功，false:表示连接失败
     * @param isReconnect    是否是重连, true: 表示这是重新连接，false: 表示这是第一次连接
     * @throws Exception
     * @author: tanyaowu
     */
    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
        if (isReconnect) {
            log.warn(String.format("结点%重新连接服务端", channelContext));
        }
        if (isConnected) {
            log.info(String.format("结点%s连接服务端成功", channelContext));
        }else{
            log.warn(String.format("结点%s连接服务端%s失败", node.getIndex(),channelContext.getServerNode()));
        }
    }


    /**
     * 原方法名：onAfterDecoded
     * 解码成功后触发本方法
     *
     * @param channelContext
     * @param packet
     * @param packetSize
     * @throws Exception
     * @author: tanyaowu
     */
    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {

    }

    /**
     * 接收到TCP层传过来的数据后
     *
     * @param channelContext
     * @param receivedBytes  本次接收了多少字节
     * @throws Exception
     */
    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {

    }

    /**
     * 消息包发送之后触发本方法
     *
     * @param channelContext
     * @param packet
     * @param isSentSuccess  true:发送成功，false:发送失败
     * @throws Exception
     * @author tanyaowu
     */
    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {

    }

    /**
     * 当处理好消息是就行action
     * @param channelContext
     * @param packet
     * @param cost           本次处理消息耗时，单位：毫秒
     * @throws Exception
     */
    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {
        action.doAction(channelContext);
    }

    /**
     * 连接关闭前触发本方法
     *
     * @param channelContext the channelcontext
     * @param throwable      the throwable 有可能为空
     * @param remark         the remark 有可能为空
     * @param isRemove
     * @throws Exception
     * @author tanyaowu
     */
    @Override
    public void onBeforeClose(final ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
       log.warn(String.format("客户端%s连接关闭", channelContext));
        AllNodeCommonMsg.timer.start();
       // 假如连接中断则移除
        P2PConnectionMsg.CLIENTS.values().removeIf(v -> v.equals(channelContext));

        //掉线的节点实际编号ABCD
        int nodeId = channelContext.getServerNode().getPort()%18080;
        //是否在备份节点中
//        int backupIndex = AllNodeCommonMsg.backupNodeList.indexOf(nodeId);
        if(AllNodeCommonMsg.activeNodeList.contains(nodeId)){
            //掉线的节点逻辑编号0123
            int activeIndex = AllNodeCommonMsg.activeNodeList.indexOf(nodeId);
            if(activeIndex == AllNodeCommonMsg.view){
                AllNodeCommonMsg.increment();
                System.out.println("主节点掉线，视图更新："+ AllNodeCommonMsg.view);
            }
            // 是活跃节点
            if(AllNodeCommonMsg.backupNodeList.size() >0){
                // 从备份节点列表取出节点
                int backupNode = AllNodeCommonMsg.backupNodeList.remove(0);
                log.info(String.format("节点%s掉线，用备份节点%s替换", nodeId,backupNode));
                AllNodeCommonMsg.activeNodeList.set(activeIndex,backupNode);
            }else{
                AllNodeCommonMsg.activeNodeList.set(activeIndex,null);
                log.warn("节点数量不满足最低要求");
            }
        }else if(AllNodeCommonMsg.backupNodeList.contains(nodeId)){
            //节点在备份节点列表中
            AllNodeCommonMsg.backupNodeList.remove(Integer.valueOf(nodeId));
            log.info(String.format("节点%s掉线，从备份节点列表中删除", nodeId));
        }else {
            // 节点既不在活跃节点列表中，也不在备份节点列表中
            log.warn(String.format("警告：节点 %s掉线，但不属于活跃节点或备份节点", nodeId));
        }
//        log.info(String.format("当前活动节点列表：%s，备份节点列表：%s",
//                Arrays.toString(AllNodeCommonMsg.activeNodeList.toArray()),Arrays.toString(AllNodeCommonMsg.backupNodeList.toArray()) ));
        nodePrinter();
        //打印网络

        AllNodeCommonMsg.timer.stop();
        System.out.println(StopWatch.startTime+ "  到 为止  "  +StopWatch.endTime );
        log.info("总计用时："+ AllNodeCommonMsg.timer.getElapsedTime() + "毫秒");

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
}
