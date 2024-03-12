package com.bsl.util;

import com.alibaba.fastjson.JSON;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.vote.VoteBasicMsg;
import org.tio.utils.json.Json;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void up(List<VoteBasicMsg> localResultList, List<VoteBasicMsg> receivedResultList,int from){

        //收到了eventId对应的表决结果List<VoteBasicMsg>
        boolean containsTargetNode = false;
        for (VoteBasicMsg receivedResult : receivedResultList) {
            //如果存在来源节点的记录则有效，记录
            boolean replaced = false;
            if(receivedResult.getNodeId() ==  from){
                //[<0,true>]
                containsTargetNode = true;
                // 3. 更新本地的 AllNodeCommonMsg.allNodeState
                //[<0,true>,<1,true>,<>,<>]
                //[<0,true>,<2,true>,<>,<>]
                //[<0,true>,<3,true>,<>,<>]
                for(int i = 0; i < localResultList.size(); i++){
                    //已经有了
                    if(localResultList.get(i).getNodeId()== receivedResult.getNodeId()){
                        localResultList.set(i,receivedResult);
                        replaced = true;
                        break;
                    }
                }
                if(!replaced){
                    localResultList.add(receivedResult);
                    break;
                }
                //本地[<1,true>,<0,true>,<2,true>,<>]
                //本地[<1,true>,<0,true>,<2,true>,<3,true>]
            }
        }
        if (!containsTargetNode) {
            //没有,则此消息无效
            return;
        }
    }
    public static void main(String[] args) {
        //更新接收到的节点的选择
        // 1. 查询本地的 AllNodeCommonMsg.allNodeState，找到与 msg 中事件ID对应的结果列表
        //[<1,true>,<>,<>,<>]
        List<VoteBasicMsg> localResultList = new ArrayList<>();
        localResultList.add(new VoteBasicMsg(1,true));
        localResultList.add(new VoteBasicMsg(0,true));
        localResultList.add(new VoteBasicMsg(3,true));
        System.out.println("更新前"+JSON.toJSON(localResultList));
        //本地[<1,true>,<0,true>,<>,<>]
        // 2. 将本地的结果与接收到的结果合并
        //[<0,true>,<>,<>,<>]
        List<VoteBasicMsg> receivedResultList = new ArrayList<>();
        receivedResultList.add(new VoteBasicMsg(0,false));
        receivedResultList.add(new VoteBasicMsg(2,true));
        up(localResultList,receivedResultList,2);
        System.out.println("[<0,false>,<2,true>],2");
        System.out.println("更新后"+JSON.toJSON(localResultList));

        receivedResultList = new ArrayList<>();
        receivedResultList.add(new VoteBasicMsg(0,true));
        receivedResultList.add(new VoteBasicMsg(3,true));
        up(localResultList,receivedResultList,3);
        System.out.println("[<0,true>,<3,true>],3");
        System.out.println("更新后"+JSON.toJSON(localResultList));

        receivedResultList = new ArrayList<>();
        receivedResultList.add(new VoteBasicMsg(0,true));
        receivedResultList.add(new VoteBasicMsg(2,false));
        up(localResultList,receivedResultList,2);
        System.out.println("[<0,true>,<2,false>],2");
        System.out.println("更新后"+JSON.toJSON(localResultList));
    }
}
