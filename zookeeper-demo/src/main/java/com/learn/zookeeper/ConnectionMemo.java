package com.learn.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * zookeeper  调用API 创建节点、修改节点、删除节点
 *
 */
public class ConnectionMemo {
    public static void main(String[] args) {


        // 192.168.1.102:2181
        ZooKeeper zooKeeper = null;
        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);

            zooKeeper = new ZooKeeper("192.168.1.103:2181,192.168.1.104:2181,192.168.1.106:2181", 5000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (Event.KeeperState.SyncConnected == event.getState()) {
                        // 如果收到了服务器响应事件，连接成功
                        countDownLatch.countDown();
                    }
                }
            });
            countDownLatch.await();
            System.out.println("zooKeeper state=:" + zooKeeper.getState());
            // 添加节点
            zooKeeper.create("/zk-node01", "2345".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            Thread.sleep(1000);

            Stat stat = new Stat();
            // 得到当前节点的值
            byte[] bytes = zooKeeper.getData("/zk-node01", null, stat);
            System.out.println(new String(bytes));

            // 修改节点值
            zooKeeper.setData("/zk-node01", "1243".getBytes(), stat.getVersion());

            byte[] bytes1 = zooKeeper.getData("/zk-node01", null, stat);
            System.out.println(new String(bytes1));

            zooKeeper.delete("/zk-node01", stat.getVersion());
            zooKeeper.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }


    }
}
