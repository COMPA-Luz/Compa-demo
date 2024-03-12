package com.bsl.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.bean.DbDao;
import com.bsl.dao.bean.ReplayJson;
import com.bsl.dao.pbft.MsgCollection;
import com.bsl.dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import java.io.*;
import java.util.*;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/**

 *
 * @author: kevin
 * @data: 2020/3/1 上午12:13
 * @description: levelDB操作
 */
@Slf4j
public class DbUtil {
    public static String dbFilePath = "E:\\pbft\\test.db";

    private static DB db = null;
    private static Options options = new Options();
    private static boolean flag = true;

    private static boolean init() {
        options = new Options();
        options.createIfMissing(true);
        try {
            db = factory.open(new File(dbFilePath), options);
        } catch (IOException e) {
            log.warn(String.format("数据库获取失败%s", e.getMessage()));
            return false;
        }
        return true;
    }


    /**
     * 插入一个dao
     *
     * @param dao
     */
    private static void insert(DbDao dao) {
        try {
            db.put(String.valueOf(dao.getNode()).getBytes(), daoToBytes(dao));
        } catch (IOException e) {
            log.warn(String.format("对象序列化失败%s", e.getMessage()));
        }
    }

    synchronized public static void save() {
        if (!flag) {
            return;
        }
        flag = false;
        log.info(String.format("保存的大小%s", MsgCollection.getInstance().getDbDaos().size()));
        if (init()) {
            for (DbDao dao :
                    MsgCollection.getInstance().getDbDaos()) {
                insert(dao);
            }
            try {
                db.close();
            } catch (IOException e) {
                log.warn(String.format("levelDB关闭失败%s", e.getMessage()));
            }
        }

    }

    /**
     * 进行遍历
     */
    private static void get() {
        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;
        try {
            db = factory.open(new File(dbFilePath), options);
        } catch (IOException e) {
            log.warn(String.format("数据库获取失败%s", e.getMessage()));
            return;
        }
        DBIterator iterator = db.iterator();
        List<byte[]> list = new ArrayList<byte[]>();
        while (iterator.hasNext()) {
            Map.Entry<byte[], byte[]> next = iterator.next();
            byte[] value = next.getValue();
            list.add(value);
        }
        System.out.println(list.size());
        for (byte[] bytes : list) {
            try {
                DbDao dbDao = (DbDao) bytesToDao(bytes);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

//    public static void main(String[] args) {
//        dbFilePath = "/home/xiaohui/桌面/test/29.db";
//        get();
//    }

    /**
     * @param node 谁发送的信息
     * @param msg
     */
    public static void addDaotoList(int node, PbftMsg msg) {
        DbDao dbDao = new DbDao();
        dbDao.setNode(node);
        dbDao.setPublicKey(AllNodeCommonMsg.publicKeyMap.get(node));
        dbDao.setTime(msg.getTime());
        dbDao.setViewNum(msg.getViewNum());
        MsgCollection.getInstance().getDbDaos().add(dbDao);
    }

    /**
     * 将对象序列化
     *
     * @param dao
     * @return
     * @throws IOException
     */
    private static byte[] daoToBytes(DbDao dao) throws IOException {
        //创建内存输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(dao);
        oos.writeObject(null);
        oos.close();
        baos.close();
        return baos.toByteArray();
    }

    /**
     * 将对象反序列化
     *
     * @param bytes
     * @return
     * @throws IOException
     */
    private static Object bytesToDao(byte[] bytes) throws IOException, ClassNotFoundException {
        //创建内存输出流
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);
        Object obj = null;
        while ((obj = inputStream.readObject()) != null) {
            DbDao dbDao = (DbDao) obj;
            System.out.println(dbDao);
        }
        inputStream.close();
        byteArrayInputStream.close();
        return obj;

    }


    private static List<ReplayJson> readReplayJsonFromFile(String filePath) {
        List<ReplayJson> replayJsonList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                ReplayJson replayJson = JSON.parseObject(line, ReplayJson.class);
                replayJsonList.add(replayJson);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return replayJsonList;
    }
    private static void writeReplayJsonToFile(List<ReplayJson> replayJsonList, String filePath) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (ReplayJson replayJson : replayJsonList) {
                String jsonStr = JSON.toJSONString(replayJson);
                bw.write(jsonStr);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void deleteNodes(int targetIndex,String filePath){
        List<ReplayJson> replayJsonList = readReplayJsonFromFile(filePath);

        Iterator<ReplayJson> iterator = replayJsonList.iterator();
        while (iterator.hasNext()) {
            ReplayJson replayJson = iterator.next();
            if (replayJson.getIndex() == targetIndex) {
                iterator.remove();
                log.info("删除信息");
                break;
            }
        }
        writeReplayJsonToFile(replayJsonList, filePath);
    }
    public static void main(String[] args) {
        int targetIndex = 1; // 目标行的index

        String filePath = "D:\\temp_workstation_Luz\\pbft-master-origin-master\\pbft-master-origin-master\\oldIp.json"; // 替换为实际的文件路径
        deleteNodes(1,filePath);
    }

}
