package com.learn.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * 事件监听
 */
public class CuratorWatcherDemo {
    public static void main(String[] args) throws Exception {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString("192.168.1.103:2181,192.168.1.104:2181,192.168.1.106:2181")
                .sessionTimeoutMs(4000).retryPolicy(new ExponentialBackoffRetry(1000,3))
                // 指定命名空间，一般会把当前某个业务放指定的命名空间下，不建议直接放在根节点上
                .namespace("curator")
                .build();

        curatorFramework.start();
        // 当前节点创建和删除事件监听
//        addListenerWatcherNodeCache(curatorFramework, "/api");
        // 子节点的创建、修改、删除事件监听
//        addListenerWithPathChileCache(curatorFramework, "/api");

        addListenerWithTreeCache(curatorFramework, "/api");
        // 让进程停留
        System.in.read();
    }

    /**
     * PathChildCache 监听一个节点下子节点的创建、删除、更新
     * NodeCache 监听一个节点的更新h和创建事件
     * TreeNode  综合PathChildCache和NodeCache事件
     */
    public static void addListenerWatcherNodeCache(CuratorFramework curatorFramework, String path) throws Exception {
        final NodeCache nodeCache = new NodeCache(curatorFramework, path, false);
        NodeCacheListener nodeCacheListener = new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {

                System.out.println("Receive Event:"+nodeCache.getCurrentData().getPath());
            }
        };
        nodeCache.getListenable().addListener(nodeCacheListener);
        nodeCache.start();
    }

    /**
     * 监听api 节点的字节的变化
     * @param curatorFramework
     * @param path
     * @throws Exception
     */
    public static void addListenerWithPathChileCache(CuratorFramework curatorFramework, String path) throws Exception {
        PathChildrenCache pathChildrenCache = new PathChildrenCache(curatorFramework,path, true);

        PathChildrenCacheListener pathChildrenCacheListener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
                System.out.println("Receive Event:"+event.getType());
            }
        };
        // 将当前事件当前到节点上
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        // 设置对应Model
        pathChildrenCache.start(PathChildrenCache.StartMode.NORMAL);
    }

    /**
     * 综合节点事件监听
     */
    public static void addListenerWithTreeCache(CuratorFramework curatorFramework, String path) throws Exception {

        TreeCache treeCache = new TreeCache(curatorFramework,path);
        TreeCacheListener treeCacheListener = new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent event) throws Exception {
                System.out.println(event.getType()+"-"+event.getData().getPath());
            }
        };
        treeCache.getListenable().addListener(treeCacheListener);
        treeCache.start();
    }
}
