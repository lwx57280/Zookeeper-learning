package com.learn.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class WatcherDemo {

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        // 192.168.1.102:2181


        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final ZooKeeper zooKeeper = new ZooKeeper("192.168.1.103:2181,192.168.1.104:2181,192.168.1.106:2181", 5000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("默认事件:"+event.getType());
                if (Event.KeeperState.SyncConnected == event.getState()) {
                    // 如果收到了服务器响应事件，连接成功
                    countDownLatch.countDown();
                }
            }
        });
        countDownLatch.await();

        zooKeeper.create("/zk-node01", "12".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        // 通过exists绑定事件
        Stat stat = zooKeeper.exists("/zk-node01", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println(event.getType() + "->" + event.getPath());

                try {
                    zooKeeper.exists(event.getPath(), true);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        // 通过修改的事务类型操作触发监听事件
        stat = zooKeeper.setData("/zk-node01", "11".getBytes(), stat.getVersion());

        Thread.sleep(1000);
        zooKeeper.delete("/zk-node01", stat.getVersion());
        System.in.read();

    }
}
