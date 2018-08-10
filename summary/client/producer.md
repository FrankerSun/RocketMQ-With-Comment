# 生产者分析



与消息发送紧密相关的几行代码：
```
   1. new DefaultMQProducer("ProducerGroupName");
   2. defaultMQProducer.start();
   3. new Message(...var);
   4. producer.send(message);
   5. producer.shutdown();
```


ProducerTable:
    Key:Group1 Value:MqProducerInner_1

TopicPublishInfoTable:路由信息表
    MixAll.DEFAULT_TOPIC
    topicPublishInfo1
    
工厂模式与单例模式





### 发送Message的过程：

根据topic，从topicPublishInfoTable中获取相应的topicPublishInfo，如果没有则更新路由信息，
从nameserver端拉取最新路由信息。

### 从nameserver端拉取最新路由信息的过程：

首先getTopicRouteInfoFromNameServer，然后topicRouteData2TopicPublishInfo。 

