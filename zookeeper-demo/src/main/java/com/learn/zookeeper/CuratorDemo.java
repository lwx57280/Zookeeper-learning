package com.learn.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

public class CuratorDemo {

    public static void main(String[] args) throws Exception {

        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString("192.168.1.103:2181,192.168.1.104:2181,192.168.1.106:2181")
                // 会话超时时间、重试策略
                .sessionTimeoutMs(4000).retryPolicy(new ExponentialBackoffRetry(1000,3))
                // 指定命名空间，一般会把当前某个业务放指定的命名空间下，不建议直接放在根节点上
                .namespace("curator")
                .build();

        curatorFramework.start();

        // 结果: /curator/api/node1
        // 原生api中，必须是逐层创建，也就是父节点必须存在，子节点才能创建
//        curatorFramework.create().creatingParentsIfNeeded()
//                .withMode(CreateMode.PERSISTENT)
//                .forPath("/api/node1","1".getBytes());


        // 删除节点
        //curatorFramework.delete().deletingChildrenIfNeeded().forPath("/api/node1");

        //
        Stat stat = new Stat();
        curatorFramework.getData().storingStatIn(stat).forPath("/api/node1");
        curatorFramework.setData().withVersion(stat.getVersion()).forPath("/api/node1", "xx123".getBytes());

        curatorFramework.close();
    }
}
