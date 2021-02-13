package com.learn.zookeeper.locks;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

/**
 * Curator框架实现ZooKeeper分布式锁
 * @Author:         cong zhi
 * @CreateDate:     2021/1/24 20:23
 * @UpdateUser:     cong zhi
 * @UpdateDate:     2021/1/24 20:23
 * @UpdateRemark:   修改内容
 * @Version:        1.0
 */
public class CuratorDemo {

    public static void main(String[] args) {
        Integer a = 1000, b = 1000;
        //1
        System.out.println(a == b);
        System.out.println(Integer.MAX_VALUE);
        Integer c = 100, d = 100;
        //2
        System.out.println(c == d);
//        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().build();
//
//        InterProcessMutex interProcessMutex = new InterProcessMutex(curatorFramework, "/locks");
//        try {
//            interProcessMutex.acquire();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
