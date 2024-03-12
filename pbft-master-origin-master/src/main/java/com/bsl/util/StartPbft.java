package com.bsl.util;

import com.bsl.config.AllNodeCommonMsg;
import com.bsl.config.StartConfig;
import com.sun.org.apache.bcel.internal.generic.ALOAD;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**

 *
 * @author: kevin
 * @data: 2020/2/19 下午7:56
 * @description: 启动
 */
@Slf4j
public class StartPbft {

    public static <T> T[] expandArray(Class<T> tClass, T[] originalArray) {
        int expand = AllNodeCommonMsg.quorumSize;
        T[] array  = (T[]) Array.newInstance(tClass, expand);;
        int origin = originalArray.length;
        for(int i=0;i<expand;i++){
            array[i]  = originalArray[i%origin] ;
        }
        return array;
    }

//    public static String[] expandArray(String[] originalArray) {
//        int expand = AllNodeCommonMsg.quorumSize;
//        String[] expandedArray = new String[expand];
//        int origin = originalArray.length;
//        if(origin >= expand){
//            //源数组更长
//            System.arraycopy(originalArray, 0, expandedArray, 0, expand);
//        }else{
//            //源数组更短
//            int round = expand / origin;
//            int start = 0;
//            int many ;
//            for(int i=0;i<=round;i++){
//                many = (start+origin)> (expand-1) ? (expand-1-start+1) : origin;
//                System.arraycopy(originalArray, 0, expandedArray, start, many);
//                start += origin;
//                System.out.println("expandedArray: "+ Arrays.toString(expandedArray));
//            }
//        }
//        return expandedArray;
//    }

    public static void main(String[] args) {
        String[] ori = new String[]{"1","2"};
        String[] res =  expandArray(String.class,ori);
        System.out.println(Arrays.toString(res));
    }

    public static boolean start() {
        StartConfig startConfig = new StartConfig();
        if (startConfig.startConfig()) {
            if (new Pbft().pubView()) {
                return true;
            }
        }
        return false;
    }
}
