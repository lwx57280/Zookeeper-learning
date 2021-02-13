package com.learn.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * zk分布式实现
 */
public class DistributedLock implements Lock, Watcher {


    /**
     * 定义zookeeper的连接
     */
    private ZooKeeper zk = null;
    /**
     * 定义一个根节点
     */
    private String ROOT_LOCK = "/locks";
    /**
     * 等待前一个锁
     */
    private String WAIT_LOCK;
    /**
     * 表示当前锁
     */
    private String CURRENT_LOCK;


    private CountDownLatch countDownLatch;

    public DistributedLock() {
        try {
            this.zk = new ZooKeeper("192.168.1.101:2181", 4000, this);
            // 判断根节点是否存在
            Stat stat = zk.exists(ROOT_LOCK, false);
            if (null == stat) {
                zk.create(ROOT_LOCK, "0".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lock() {
        // 如果获得锁成功
        if (this.tryLock()) {
            System.out.println(Thread.currentThread().getName() + "->" + CURRENT_LOCK + "-> 获得锁成功!");
            return;
        }
        try {
            waitForLock(WAIT_LOCK); // 没有获得锁，则继续等待获得锁
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean waitForLock(String prev) throws KeeperException, InterruptedException {
        // 监听当前节点的上一个节点
        Stat stat = zk.exists(prev, true);
        if (null != stat) {
            System.out.println(Thread.currentThread().getName() + "->等待" + ROOT_LOCK + "/" + prev + "释放");
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await();
            System.out.println(Thread.currentThread().getName() + "->获得锁成功");
        }
        return true;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        try {
            // 创建临时有序节点
            CURRENT_LOCK = zk.create(ROOT_LOCK + "/", "0".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            System.out.println(Thread.currentThread().getName() + "->" +
                    CURRENT_LOCK + "尝试竞争锁");
            // 获取根节点下的所有子节点
            List<String> childrens = zk.getChildren(ROOT_LOCK, false);
            // 定义一个集合对节点进行排序
            SortedSet<String> sortedSet = new TreeSet<>();
            for (String children : childrens) {
                sortedSet.add(ROOT_LOCK + "/" + children);
            }
            // 获取当前所有子节点中最小的值
            String firstNode = sortedSet.first();
            SortedSet<String> lessThenMe = ((TreeSet<String>) sortedSet).headSet(CURRENT_LOCK);
            // 通过当前节点和子节点中最小的节点进行比较，如果相等，表示获得锁成功
            if (CURRENT_LOCK.equals(firstNode)) {
                return true;
            }
            if (!lessThenMe.isEmpty()) {
                // 获得比当前节点更小的最后一个节点，设置给WAIT_LOCK
                WAIT_LOCK = lessThenMe.last();
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        System.out.println(Thread.currentThread().getName() + "->释放锁" + CURRENT_LOCK);
        try {
            zk.delete(CURRENT_LOCK, -1);
            CURRENT_LOCK = null;
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (this.countDownLatch != null) {
            this.countDownLatch.countDown();
        }
    }
}
