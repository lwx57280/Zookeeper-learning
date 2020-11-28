# Zookeeper-learning
分布式协调服务


学习目标

1、数据存储

2、基于Java API初探zookeeper

3、深入分析Watcher机制的实现原理

4、Curator客户端的使用，简单高效



zoo.cfg文件中，datadir

事务日志

快照日志

运行时日志 bin/zookeepe.out



## 基于Java API初探zookeeper使用



### 引入zookeeper相关依赖jar

```xml
     <dependency>
         <groupId>org.apache.zookeeper</groupId>
         <artifactId>zookeeper</artifactId>
         <version>3.4.8</version>
     </dependency>
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-framework</artifactId>
        <version>4.0.1</version>
    </dependency>
    
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-recipes</artifactId>
        <version>4.0.1</version>
    </dependency>
```



* 建立连接

```java
 		// 192.168.1.102:2181
        ZooKeeper zooKeeper = null;
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);

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
            zooKeeper.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```



## zookeeper 会话生命周期

- ⼀个会话从NOT_CONNECTED状态开始，当ZooKeeper客户端初始化后转换到CONNECTING状态（箭头①）
- 正常情况下，成功与 ZooKeeper服务器建⽴连接后，会话转换到CONNECTED状态（箭头②）
- 当客户端与ZooKeeper服务器断开连接或者⽆法收到服务器的响应时，它就会转换回CONNECTING状态（箭头③）并尝试发现其他ZooKeeper服务器：
  - 如果可以发现另⼀个服务器或重连到原来的服务器，当服务器确认会话有效后，状态又会转换回CONNECTED状态（箭头②）
  - 否则，它将会声明会话过期，然后转换到CLOSED状态（箭头④）
- 应⽤也可以显式地关闭会话（箭头④和箭头⑤）



![20201106](https://gitee.com/li_VillageHead/note-image/raw/master/img-folder/20201106104127.png)



### 通过java api 操作节点数据的增、删、改、查


```
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
        // 删除节点
        zooKeeper.delete("/zk-node01",stat.getVersion());
        zooKeeper.close();
```



## 事件机制

watcher特性：当数据发生变化的时候，zookeeper会产生一个watcher事件，并且会发送到客户端，但是客户端只会收到一次通知。如果后续这个节点再次发生变化，那么之前设置watcher的客户端不会再次收到消息。（watcher是一次性的操作）。可以通过循环监听达到永久监听效果。



### 如何注册事件机制

通过这个三个操作来绑定事件：getData、Exists、getChilden

如何触发事件？凡是事务类型的操作，都会触发事件。create、delete、setData



事件监听操作

```java
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
```

### watcher事件机制

```java
None(-1),				// 客户端连接状态发生变化的时候，会收到node事件
NodeCreated(1),			// 创建节点的事件
NodeDeleted(2),			// 删除节点的事件
NodeDataChanged(3),		// 节点数据发生变更
NodeChildrenChanged(4);	 // 子节点被创建、删除、会发生事件触发
```



什么样的操作产生什么类型事件



|                               | zk-node01(监听事件) | zk-node01/child(监听事件) |
| ----------------------------- | ------------------- | ------------------------- |
| create("/zk-node01")          | NodeCreated         | 无                        |
| delete("/zk-node01")          | NodeDeleted         | 无                        |
| setData("/zk-node01")         | NodeDataChanged     | 无                        |
| create("/zk-node01/children") | NodeChildrenChanged | NodeCreated               |
| delete("/zk-node01/children") | NodeChildrenChanged | NodeDeleted               |
|                               |                     | NodeDataChanged           |

## 事件的原理



![20201101152004](https://gitee.com/li_VillageHead/note-image/raw/master/img-folder/20201115200432.png)

```java
ClientCnxnSocketNetty
	doTransport()
```



## zookeeper网络通信架构

![20201101152003](https://gitee.com/li_VillageHead/note-image/raw/master/img-folder/20201115200346.png)

- Stats： 表示ServerCnxn上的统计数据。
- Watcher：表示时间处理器。
- ServerCnxn：表示服务器连接，表示一个从客户端到服务器的连接。
- NettyServerCnxn：基于Netty的连接的具体实现。
- NIOServerCnxn：基于NIO的连接的具体实现（默认）。



## zookeeper源码请求处理链



https://www.cnblogs.com/leesf456/p/6472496.html







## Curator的操作

```java
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

        /**
         *
         */
        Stat stat = new Stat();
        curatorFramework.getData().storingStatIn(stat).forPath("/api/node1");
        curatorFramework.setData().withVersion(stat.getVersion()).forPath("/api/node1", "xx123".getBytes());

        curatorFramework.close();
    }
```





