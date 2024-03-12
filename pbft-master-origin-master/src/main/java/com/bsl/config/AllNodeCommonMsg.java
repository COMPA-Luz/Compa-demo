package com.bsl.config;

import com.bsl.control.StopWatch;
import com.bsl.dao.node.NodeBasicInfo;
import com.bsl.dao.vote.VoteBasicMsg;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**

 *
 * @author: kevin
 * @data: 2020/1/22 下午3:27
 * @description: 所有的结点的需要相同的信息，（假如不相同，则需要同步）
 */
@Slf4j
public class AllNodeCommonMsg {
    public static int voteFinished = -1;
    public static int test = -1;
    public static boolean onOrOFF = true;
    //多模表决数量
    public static int quorumSize = 4;
    public static String filePath;
    //目标容器进程Pid
    public static String targetPid;
    public static Map<String, Boolean> myChoice = new HashMap<>();
    public static boolean haveUpdate = false;
    //监控进程的Pid
    public static long monitorPid;
    //计算时间
    public static StopWatch timer = new StopWatch();
    //存储序号列表（SerialNumberList）：这个数据结构用于存储当前活动节点的序号信息。
    public static List<Integer> serialNumberList = new ArrayList<>();
    //PbftUtil.replaceNode
    //活动节点列表（ActiveNodeList）：这个数据结构用于跟踪当前活动的节点。
    //逻辑编号就是list中的位置 0 1 2 3
    //       对应实际节点编号 A B C D
    public static List<Integer> activeNodeList = new ArrayList<>(Arrays.asList(new Integer[quorumSize]));
    //备份节点列表（BackupNodeList）：这个数据结构用于存储备份节点的信息。
    //存储实际编号 A B C D
    public static List<Integer> backupNodeList = new ArrayList<>();
    //可疑节点列表（BackupNodeList）：这个数据结构用于存储备份节点的信息。
    public static List<Integer> replacedNodeList = new ArrayList<>();
    //某个事件对应的List
    public static Map<String, List<VoteBasicMsg>> allNodeState = new HashMap<>();
    public static Map<String, List<Integer>> commitMap = new HashMap<>();
    public static boolean getVoteForNode(String event, int nodeId) {
        // 检查事件是否存在
        if (allNodeState.containsKey(event)) {
            List<VoteBasicMsg> votes = allNodeState.get(event);
            // 遍历列表，查找指定节点的投票值
            for (VoteBasicMsg voteMsg : votes) {
                if (voteMsg.getNodeId() == nodeId) {
                    return voteMsg.isVote();
                }
            }
        }
        // 如果未找到匹配的投票值，则返回默认值（这里可以根据需求进行修改）
        return false;
    }
    public static ConcurrentHashMap<String, Integer> eventHasDone = new ConcurrentHashMap<>();

    //当前节点版本号
    private static long globalSequence = 0;
    public static long getGlobalSequence() {
        return globalSequence;
    }
    public static void setGlobalSequence(long globalSequence) {
        AllNodeCommonMsg.globalSequence = globalSequence;
    }
    public static synchronized void updateGlobalSequence() {
        AllNodeCommonMsg.globalSequence ++ ;
    }

    /**
     * 获得已收集表决结果数量
     */
    public static int getSizeOfAllNodeState(String eventId){
        Set<Integer> distinctNodes = new HashSet<>();

        for (VoteBasicMsg vote : AllNodeCommonMsg.allNodeState.get(eventId)) {
            distinctNodes.add(vote.getNodeId());
        }

        return distinctNodes.size();
    }
    /**
     * 获得最大失效结点的数量
     *
     * @return
     */
    public static int getMaxf() {
        return (getSize() - 1) / 3;
    }

    public static int getAgreeNum(){
        return 2 * AllNodeCommonMsg.getMaxf() + 1;
    }

    /**
     * 获得主节点的index序号
     *
     * @return
     */
    public static int getPriIndex() {
//        return  0;
        return activeNodeList.get(view);
    }

    /**
     * 保存结点对应的ip地址和端口号
     */
    public static ConcurrentHashMap<Integer, NodeBasicInfo> allNodeAddressMap = new ConcurrentHashMap<>(2 << 10);

    /**
     * 所有节点公钥
     */
    public static Map<Integer,String> publicKeyMap = new ConcurrentHashMap<>(2<<10);


    /**
     * view的值，0代表view未被初始化
     * 当前视图的编号，通过这个编号可以算出主节点的序号
     */
    public volatile static int view = 0;
    public static synchronized int increment() {
        view= (view+1)%quorumSize ;
        return view;
    }
    /**
     * @return 区块链中结点的总结点数
     */
    public static int getSize() {
        return allNodeAddressMap.size() + 1;
    }

    /**
     * 节点的状态
     */
    public static NodeStatus nodeStatus = NodeStatus.NORMAL;
    public enum NodeStatus {
        NORMAL("正常节点"),
        DAMAGED("受损节点"),
        MALICIOUS("恶意节点"),
        DOWN("宕机节点");

        private String description;

        NodeStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
    public static NodeStatus getStatusFromArg(String arg) {
        // 根据参数值返回相应的状态枚举实例
        switch (arg) {
            case "normal":
                return NodeStatus.NORMAL;
            case "damaged":
                return NodeStatus.DAMAGED;
            case "malicious":
                return NodeStatus.MALICIOUS;
            case "down":
                return NodeStatus.DOWN;
            default:
                return null;
        }
    }

    public static void performNormalOperation() {
        System.out.println("执行正常操作");
    }

    public static void performDamagedOperation() {
        System.out.println("执行受损操作");
    }

    public static void performMaliciousOperation() {
        System.out.println("执行恶意操作");
    }

}
