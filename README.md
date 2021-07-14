# RabbitmqDemo

全文可以去CSDN查看我写的博客[RabbitMQ 快速入门](https://blog.csdn.net/qq_43781399/article/details/118654194)

## 2.0 六种MQ的运行模式

### 2.1 基本消息模型（Hello World）

![image-20210711083354816](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711083354816.png)

对于上图模型中：

- P：生产者，消息的发出者（生产消息）

- C： 消费者，消息的接收者（处理消息）

- queue： 消息队列，即上图中红色的部分；队列，生产者将消息入队，消费者将队列按顺序出队再进行处理

模型介绍：

​	这个模型是最简单的基本模型，官网将其称为“Hello World”，就是生产者简单的将消息放置到队列中，消费者也是单个，顺序的将其消息从队列中取出。

#### **pom文件**以及ConnectionUtil编写

接下来的步骤中有一些重复的步骤，就如下方的pom文件导入依赖，只是简单的将日志和前文提到的**AMQP**依赖导入

```xml
    <dependencies>
            <dependency>
                <groupId>com.rabbitmq</groupId>
                <artifactId>amqp-client</artifactId>
                <version>5.7.1</version>
            </dependency>
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
                <version>1.8.0-alpha2</version>
            </dependency>
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-log4j12</artifactId>
                <version>1.8.0-alpha2</version>
            </dependency>
    </dependencies>
```

**ConnectionUtil**连接工具类

```java
public class ConnectionUtil {

    /**
     * Description : 用于获取MQ的连接
     * @author HuZhiYong
     * @return com.rabbitmq.client.Connection
     * @date 2021/07/11 09:19
     * @since 0.0.1
     */
    public static Connection getConnection() throws Exception {
        //定义连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        //设置服务地址
        factory.setHost("192.168.1.2");
        //端口
        factory.setPort(5672);
        //设置账号信息，用户名、密码、vhost
        factory.setVirtualHost("/");//设置虚拟机，一个mq服务可以设置多个虚拟机，每个虚拟机就相当于一个独立的mq
        factory.setUsername("admin");
        factory.setPassword("admin");
        // 通过工厂获取连接
        Connection connection = factory.newConnection();
        return connection;
    }
}
```

关于其中的一些配置下面我放一下查看方式

虚拟机列表：`rabbitmqctl list_vhosts`（我这里没有创建虚拟机，只有默认的一个‘/’）

![image-20210711092743757](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711092743757.png)

用户列表：`rabbitmqctl list_users`

![image-20210711092833770](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711092833770.png)

更改用户密码：`rabbitmqctl change_pssword <username> <UserNewPassword>`

![image-20210711093008203](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711093008203.png)

服务器地址和接口名字看个人安装的地方以及端口号的配置。

注：因为是第一个demo，这里加入的pom文件和ConnectionUtil工具类，后边的Demo将不再提及上述文件。

#### Send

1. 获取连接
2. 创建通道
3. 声明队列
4. 发起消息

```java
/**
 * @author huzy
 * @date 2021/07/1109:48
 */
public class Send {
    // 基本消息类型测试
    private final static String QUEUE_NAME = "simple_queue";

    public static void main(String[] argv) throws Exception {
        // 1、获取到连接
        Connection connection = ConnectionUtil.getConnection();
        // 2、从连接中创建通道，使用通道才能完成消息相关的操作
        Channel channel = connection.createChannel();
        // 3、声明（创建）队列
        //参数：String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
        /**
         * 参数明细
         * 1、queue 队列名称
         * 2、durable 是否持久化，如果持久化，mq重启后队列还在
         * 3、exclusive 是否独占连接，队列只允许在该连接中访问，如果connection连接关闭队列则自动删除,如果将此参数设置true可用于临时队列的创建
         * 4、autoDelete 自动删除，队列不再使用时是否自动删除此队列，如果将此参数和exclusive参数设置为true就可以实现临时队列（队列不用了就自动删除）
         * 5、arguments 参数，可以设置一个队列的扩展参数，比如：可设置存活时间
         */
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 4、消息内容
        String message = "Hello World!";
        // 向指定的队列中发送消息
        // 参数：String exchange, String routingKey, BasicProperties props, byte[] body
        /**
         * 参数明细：
         * 1、exchange，交换机，如果不指定将使用mq的默认交换机（设置为""）
         * 2、routingKey，路由key，交换机根据路由key来将消息转发到指定的队列，如果使用默认交换机，routingKey设置为队列的名称
         * 3、props，消息的属性
         * 4、body，消息内容
         */
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");

        //关闭通道和连接(资源关闭最好用try-catch-finally语句处理)
        channel.close();
        connection.close();
    }
}

```

控制台：

![image-20210711135639583](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711135639583.png)

MQ management：

![image-20210711142904992](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711142904992.png)

#### Recv

1. 获取连接
2. 创建通道
3. 声明队列
4. 实现消费方法（拿到消息后进行的处理方法）
5. 对队列进行监听

```java
/**
 * @author huzy
 * @date 2021/07/1109:49
 */
public class Recv {
    private final static String QUEUE_NAME = "simple_queue";

    public static void main(String[] argv) throws Exception {
        // 获取到连接
        Connection connection = ConnectionUtil.getConnection();
        //创建会话通道,生产者和mq服务所有通信都在channel通道中完成
        Channel channel = connection.createChannel();
        // 声明队列
        //参数：String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
        /**
         * 参数明细
         * 1、queue 队列名称
         * 2、durable 是否持久化，如果持久化，mq重启后队列还在
         * 3、exclusive 是否独占连接，队列只允许在该连接中访问，如果connection连接关闭队列则自动删除,如果将此参数设置true可用于临时队列的创建
         * 4、autoDelete 自动删除，队列不再使用时是否自动删除此队列，如果将此参数和exclusive参数设置为true就可以实现临时队列（队列不用了就自动删除）
         * 5、arguments 参数，可以设置一个队列的扩展参数，比如：可设置存活时间
         */
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //实现消费方法
        DefaultConsumer consumer = new DefaultConsumer(channel){
            // 获取消息，并且处理，这个方法类似事件监听，如果有消息的时候，会被自动调用
            /**
             * 当接收到消息后此方法将被调用
             * @param consumerTag  消费者标签，用来标识消费者的，在监听队列时设置channel.basicConsume
             * @param envelope 信封，通过envelope
             * @param properties 消息属性
             * @param body 消息内容
             * @throws IOException
             */
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException, UnsupportedEncodingException {
                //交换机
                String exchange = envelope.getExchange();
                System.out.println("envelope = " + envelope);
                System.out.println("properties = " + properties);
                //消息id，mq在channel中用来标识消息的id，可用于确认消息已接收
                long deliveryTag = envelope.getDeliveryTag();
                // body 即消息体
                System.out.println();
                System.out.println();
                String msg = new String(body,"utf-8");
                System.out.println(" [x] received : " + msg + "!");
            }
        };

        // 监听队列，第二个参数：是否自动进行消息确认。
        //参数：String queue, boolean autoAck, Consumer callback
        /**
         * 参数明细：
         * 1、queue 队列名称
         * 2、autoAck 自动回复，当消费者接收到消息后要告诉mq消息已接收，如果将此参数设置为tru表示会自动回复mq，如果设置为false要通过编程实现回复
         * 3、callback，消费方法，当消费者接收到消息要执行的方法
         */
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}
```

控制台：

![image-20210711142951134](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711142951134.png)

MQ Management：

![image-20210711143004629](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711143004629.png)

可以看到的是待处理数目已经归零。

这时候，接收信息的程序并没有停止，而是一直处于运转状态，等待被MQ服务器调用其消费方法。这个时候，可能有人关于**Unacked**字段数量就有点疑问了，关于这个字段的解释，这里要穿插一下**ACK**。

### 消息确认机制（ACK）

消息确认机制，其实很多的地方都有用到，例如计算机网络中消息的传输就有，ACK在消息传输中是很经典的处理方法。

rabbitMQ的消息确认机制用的ACK，就是消费者接收过信息过后，需要返回给MQ服务器一条信息，表示**已经将这条信息处理过了**。具体到RabbitMQ中，分为自动ACK和手动ACK两种

- 自动ACK：消息一旦被接收，消费者自动发送ACK
- 手动ACK：消息接收后，需要消费者主动调用方法，进行ACK的回复

对于二者，我们一般会采用下边策略

- 如果消息不太重要，丢失也没有影响，那么自动ACK会比较方便
- 如果消息非常重要，不容丢失。那么最好在消费完成后手动ACK，否则接收消息后就自动ACK，RabbitMQ就会把消息从队列中删除。如果此时消费者宕机，那么消息就丢失了。

#### 使用手动ACK策略

对于Recv，只需要简单的调整，就可以改为手动ACK了，

1. 将第五步 添加队列监听时，`basicConsume()`方法的第二个参数改为false
2. 在重写的消息处理方法中添加手动调用ACK的`basicAck()`方法。

```JAVA
/**
 * @author huzy
 * @date 2021/07/1109:49
 *  与Recv的区别： 采用手动ACK策略
 */
public class Recv2 {

    private final static String QUEUE_NAME = "simple_queue";

    public static void main(String[] argv) throws Exception {
        // 获取到连接
        Connection connection = ConnectionUtil.getConnection();
        //创建会话通道,生产者和mq服务所有通信都在channel通道中完成
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //实现消费方法
        DefaultConsumer consumer = new DefaultConsumer(channel){
            // 获取消息，并且处理，这个方法类似事件监听，如果有消息的时候，会被自动调用
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException, UnsupportedEncodingException {
                //交换机
                String exchange = envelope.getExchange();
                System.out.println("envelope = " + envelope);
                System.out.println("properties = " + properties);
                
                //消息id，mq在channel中用来标识消息的id，可用于确认消息已接收
                long deliveryTag = envelope.getDeliveryTag();
                // body 即消息体
                String msg = new String(body,"utf-8");
                System.out.println(" [x] received : " + msg + "!");
                
                 // 手动进行ACK
                /*
                 *  void basicAck(long deliveryTag, boolean multiple) throws IOException;
                 *  deliveryTag:用来标识消息的id
                 *  multiple：是否批量.true:将一次性ack所有小于deliveryTag的消息。
                 */
                
                channel.basicAck(envelope.getDeliveryTag(), false);

            }
        };

        // 监听队列，第二个参数：是否自动进行消息确认。
        //参数：String queue, boolean autoAck, Consumer callback
        channel.basicConsume(QUEUE_NAME, false, consumer);
    }
}
```

使用手动ACK可以避免一些因某些错误，导致程序异常终止，而使得消息已被消费者，且因自动ACK已返回确认消息，但未能成功处理消息时，问题难以发现。

但是使用手动ACK的时候一定要注意在处理方法中添加回复ACK的调用，否则MQ服务器会认为你还未处理成功或处理出错，下面我们将**采用手动ACK方式**添加监听，但不在处理方法中调用ACK。

控制台：生产者发送了3条消息，消费者全都收到，因未添加回复ACK

![image-20210711153740987](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711153740987.png)

RabbitMQ Management

![image-20210711154221861](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711154221861.png)

这其实都是意料之中的，需要我们注意的是，此时我们的消费者程序还在运行，当我们将其关闭后，再去查看MQ的管理端

![image-20210711154533329](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711154533329.png)

可以看到，Ready变回3,Unacked变回0，意Unacked表示的是接受了消息，但是还未回复ACK的消息数。

生产者避免数据丢失：https://www.cnblogs.com/vipstone/p/9350075.html

### 2.2 Work消息模式

又称消息队列或者竞争消费者模式

![image-20210711155817582](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210711155817582.png)

work queues与基本消息模式相比，多了一个消费者。两个消费者共同消费同一个队列中的消息，可消息本身只能被一个消费者消费。

P：生产者：任务的发布者

C1：消费者1：领取任务并且完成任务，假设完成速度较慢（模拟耗时）

C2：消费者2：领取任务并且完成任务，假设完成速度较快

#### 生产者Send

生产者循环发送50条信息

```java
/**
 * @author huzy
 * @date 2021/07/1116:10
 */
public class Send {
    private final static String QUEUE_NAME = "word_queue";

    public static void main(String[] args) throws Exception {
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 循环添加消息
        for (int i = 0; i < 50; i++) {
            String message = "task ... " + i;
            channel.basicPublish("",QUEUE_NAME,null,message.getBytes());
            System.out.println(" [x] Sent '" + message + "'");
            Thread.sleep(i * 2);
        }
        // 关闭通道和连接
        channel.close();
        connection.close();
    }
}
```

#### 消费者Recv

```java
/**
 * @author huzy
 * @date 2021/07/1116:25
 * work_queue 方式中模拟 处理较慢的一方消费者
 */
public class Recv1 {
    private final static String QUEUE_NAME = "word_queue";

    public static void main(String[] args) throws Exception{
        System.out.println("work_queue  Recv1");
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 获取通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 重写处理方法
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body,"utf-8");
                System.out.println(" [消费者1] received : " + msg + "!");
                //模拟任务耗时1s
                try { TimeUnit.SECONDS.sleep(1); } catch (Exception e) { e.printStackTrace(); }
            }
        };
        // 添加监听
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}
```

消费者2号与消费者一号相比，只是没有模拟任务耗时。

控制台：

这里现象要描述一下。Recv2是直接处理的0，2，4，6....（直接处理结束了），这个过程中Recv1还在一个一个处理。这说明，MQ服务器直接将信息一个一个的分配给了二者，并没用注意这个消费者当前的状态。二者都消费了25条信息。实现了任务的分发

![work_queue_!](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/work_queue_!.gif)

#### 能者多劳

- 消费者1比消费者2的效率要低，一次任务的耗时较长
- 然而两人最终消费的消息数量是一样的
- 消费者2大量时间处于空闲状态，消费者1一直忙碌

现在的状态属于是把任务平均分配，正确的做法应该是消费越快的人，消费的越多。

通过 BasicQos 方法设置prefetchCount = 1。这样RabbitMQ就会使得每个Consumer在同一个时间点最多处理1个Message。换句话说，在接收到该Consumer的ack前，他它不会将新的Message分发给它。相反，它会将其分派给不是仍然忙碌的下一个Consumer。

值得注意的是：prefetchCount在手动ack的情况下才生效，自动ack不生效。（因为接收后，直接会返回ACK，不会考虑处理时延）

```java
/**
 * @author huzy
 * @date 2021/07/1116:25
 * Recv*_1 : 能做多劳模式 即使用了channel.basicQos(1);并使用手动ACK模式
 * work_queue 方式中模拟 处理较慢的一方消费者
 */
public class Recv1_2 {
    private final static String QUEUE_NAME = "word_queue";

    public static void main(String[] args) throws Exception{
        System.out.println("work_queue  Recv1");
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 获取通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 重写处理方法
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body,"utf-8");
                System.out.println(" [消费者1] received : " + msg + "!");
                //模拟任务耗时1s
                try { TimeUnit.SECONDS.sleep(1); } catch (Exception e) { e.printStackTrace(); }
                channel.basicAck(envelope.getDeliveryTag(),false);
            }
        };
        // 添加监听
        channel.basicConsume(QUEUE_NAME, false, consumer);
    }
}

```



控制台

![image-20210712082244702](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712082244702.png)

这是就可以明显的看出，消费者二高效率体现出来了。

这里我们其实涉及到一个问题，上述过程中，我们都是将消息只是默认的只能被一个消费者处理，消费者将消息消费过后，MQ服务器将不再发送。可以回头看看我们代码中在创建basicPublish时第一个参数就是Exchange，我们当时用的""，微博订阅功能是怎么实现呢，就要用到exchange了。下边就来介绍一下**订阅模型分类**，看完下边的知识补充，应该就能明白如何去做了。

### 订阅模型分类

说明下：

1、一个生产者多个消费者
2、每个消费者都有一个自己的队列
3、生产者没有将消息直接发送给队列，而是发送给exchange(交换机、转发器)
4、每个队列都需要绑定到交换机上
5、生产者发送的消息，经过交换机到达队列，实现一个消息被多个消费者消费

例子：注册->发邮件、发短信

X（Exchanges）：交换机一方面：接收生产者发送的消息。另一方面：知道如何处理消息，例如递交给某个特别队列、递交给所有队列、或是将消息丢弃。到底如何操作，取决于Exchange的类型。

**Exchange类型**有以下几种：

**Fanout**：广播，将消息交给所有绑定发到交换机的队列

**Direct**：定向，将消息交给佛和指定Routing Key 的队列

**Topic**：通配符，将消息交给routing pattern (路由模式) 的队列

**Header**: header模式与routing 的不同之地在于，header模式取消了routing key，使用header中的Key/Value（键值对）匹配队列

**Exchange（交换机）只负责转发消息，不具备存储消息的能力**，因此如果没有任何队列与Exchange绑定，或者没有符合路由规则的队列，那么消息会丢失！

### 2.3 Publish/Subscribe

发布/订阅者 模式，需要用到Fanout（广播）类型的交换器

同时向许多消费者发送信息

![image-20210712093236974](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712093236974.png)

#### Send

此时的生产者和前两种模式有些不同

- 声明Exchange，不再声明Queue

- 发送消息到Exchange，不再发送到Queue

总体流程

```java
/**
 * @author huzy
 * @date 2021/07/1209:39
 *
 * 发布/订阅者模式
 * 需要用到交换机 ： Fanout（广播）
 *  获取连接
 *  创建通道
 *  声明交换机(需指定交换机类型)
 *  发送信息
 *  关闭连接
 */
public class Send {
    private final static String EXCHANGE_NAME = "fanout_exchange";
    public static void main(String[] args) throws Exception{
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        String message = "你关注的up更新了！！";
        //发布信息到Exchange
        /**
         * 参数明细
         * 1、String exchange                    交换机名称
         * 2、String routingKey                  routing key
         * 3、BasicProperties props                基本参数
         * 4、byte[] body                        要发送的数据
         *
         */
        channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
        System.out.println(" [生产者] Sent '" + message + "'");

        // 关闭连接
        channel.close();
        connection.close();
    }
}
```

#### Recv_sms

```java
/**
 * @author huzy
 * @date 2021/07/1210:02
 * 发布/订阅 模式中的订阅者1：短息服务
 *
 * 订阅者：
 * 1、获取连接
 * 2、创建通道
 * 3、声明队列（每一个订阅者都有一个序列）
 * 4、将队列绑定到交换机
 * 5、创建一个消费者，并重写消费方法
 * 6、添加监听
 */
public class Recv1_sms {
    private final static String EXCHANGE_NAME = "fanout_exchange";
    private final static String QUEUE_NAME = "fanout_exchange_queue_sms";
    public static void main(String[] args) throws Exception{
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 将队列绑定到交换机
        /**
         * 参数
         * 1、String queue,          队列名称
         * 2、String exchange,       交换机名称
         * 3、String routingKey      routing key
         */
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");

        // 重写消费者消费方法
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body);
                System.out.println(" [短信服务] received : " + msg + "!");
            }
        };

        // 添加监听
        channel.basicConsume(QUEUE_NAME, true, consumer);


    }
}

```

#### Recv_email

```java
/**
 * @author huzy
 * @date 2021/07/1210:02
 * 发布/订阅 模式中的订阅者2：邮箱服务
 *
 * 订阅者：
 * 1、获取连接
 * 2、创建通道
 * 3、声明队列（每一个订阅者都有一个序列）
 * 4、将队列绑定到交换机
 * 5、创建一个消费者，并重写消费方法
 * 6、添加监听
 */
public class Recv2_email {
    private final static String EXCHANGE_NAME = "fanout_exchange";
    private final static String QUEUE_NAME = "fanout_exchange_queue_email";
    public static void main(String[] args) throws Exception{
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 将队列绑定到交换机
        /**
         * 参数
         * 1、String queue,          队列名称
         * 2、String exchange,       交换机名称
         * 3、String routingKey      routing key
         */
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");

        // 重写消费者消费方法
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body);
                System.out.println(" [邮箱服务] received : " + msg + "!");
            }
        };

        // 添加监听
        channel.basicConsume(QUEUE_NAME, true, consumer);

    }
}
```

控制台：

![image-20210712111934480](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712111934480.png)

可以看到，连个消费者都收到了生产者发布的消息。

### **work与P/S：**

1、publish/subscribe与work queues有什么区别。

区别：

1）work queues不用定义交换机，而publish/subscribe需要定义交换机。

2）publish/subscribe的生产方是面向交换机发送消息，work queues的生产方是面向队列发送消息(底层使用默认交换机)。

3）publish/subscribe需要设置队列和交换机的绑定，work queues不需要设置，实际上work queues会将队列绑定到默认的交换机 。

相同点：

所以两者实现的发布/订阅的效果是一样的，多个消费端监听同一个队列不会重复消费消息。

2、实际工作用 publish/subscribe还是work queues。

建议使用 publish/subscribe，发布订阅模式比工作队列模式更强大（也可以做到同一队列竞争），并且发布订阅模式可以指定自己专用的交换机。

### 2.4 Routing（路由）模式

Routing路由模式，需要用到Direct类型的交换机

![image-20210712140522436](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712140522436.png)

P：生产者，向Exchange发送消息，发送消息时，会指定一个routing key。

X：Exchange（交换机），接收生产者的消息，然后把消息递交给 与routing key完全匹配的队列

C1：消费者，其所在队列指定了需要routing key 为 error 的消息

C2：消费者，其所在队列指定了需要routing key 为 info、error、warning 的消息


#### Send

```java
/**
 * @author huzy
 * @date 2021/07/1214:07
 */
public class Send {
    private final static String EXCHANGE_NAME = "direct_exchange";

    public static void main(String[] args) throws Exception{
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME,BuiltinExchangeType.DIRECT);
        // 消息内容
        String message = "注册成功！请短信回复[T]退订";
        // 发送信息，并制定routing key 为sms，只有短信业务可以收到消息
        channel.basicPublish(EXCHANGE_NAME, "sms", null, message.getBytes());

        System.out.println(" [x] Sent '" + message + "'");
        //关闭连接
        channel.close();
        connection.close();
    }
}

```

#### Recv_sms

```java
/**
 * @author huzy
 * @date 2021/07/1214:15
 */
public class Recv1_sms {
    private final static String EXCHANGE_NAME = "direct_exchange";
    private final static String QUEUE_NAME = "direct_exchange_queue_sms";

    public static void main(String[] args) throws Exception{
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 将队列绑定到交换机
        /**
         * 参数
         * 1、String queue,          队列名称
         * 2、String exchange,       交换机名称
         * 3、String routingKey      routing key
         */
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "sms");
        //channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "email");

        // 定义队列的消费者
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            // 获取消息，并且处理，这个方法类似事件监听，如果有消息的时候，会被自动调用
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body);
                System.out.println(" [短信服务] received : " + msg + "!");
            }
        };
        // 监听队列，自动ACK
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}
```

#### Recv_email

```java
/**
 * @author huzy
 * @date 2021/07/1214:15
 */
public class Recv2_email {
    private final static String EXCHANGE_NAME = "direct_exchange";
    private final static String QUEUE_NAME = "direct_exchange_queue_email";

    public static void main(String[] args) throws Exception{
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // 将队列绑定到交换机
        /**
         * 参数
         * 1、String queue,          队列名称
         * 2、String exchange,       交换机名称
         * 3、String routingKey      routing key
         */
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "email");

        // 定义队列的消费者
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            // 获取消息，并且处理，这个方法类似事件监听，如果有消息的时候，会被自动调用
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body);
                System.out.println(" [邮箱服务] received : " + msg + "!");
            }
        };
        // 监听队列，自动ACK
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}

```

控制台：

![image-20210712143144132](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712143144132.png)

### 2.5 Topics（通配符）模式

Topics（通配符）模式需要用到topics类型的交换机

![image-20210712143716817](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712143716817.png)

 每个消费者监听自己的队列，并且设置带统配符的routingkey,生产者将消息发给broker，由交换机根据routingkey来转发消息到指定的队列。

Routingkey一般都是有一个或者多个单词组成，多个单词之间以“.”分割，例如：inform.sms

通配符规则：

#：匹配一个或多个词

*：匹配不多不少恰好1个词

举例：

audit.#：能够匹配audit.irs.corporate 或者 audit.irs

audit.*：只能匹配audit.irs

从示意图可知，我们将发送所有描述动物的消息。消息将使用由三个字（两个点）组成的Routing key发送。路由关键字中的第一个单词将描述速度，第二个颜色和第三个种类：“<speed>.<color>.<species>”。

我们创建了三个绑定：Q1绑定了“*.orange.*”，Q2绑定了“.*.*.rabbit”和“lazy.＃”。

Q1匹配所有的橙色动物。

Q2匹配关于兔子以及懒惰动物的消息。

 下面做个小练习，假如生产者发送如下消息，会进入哪个队列：

quick.orange.rabbit       Q1 Q2   routingKey="quick.orange.rabbit"的消息会同时路由到Q1与Q2

lazy.orange.elephant    Q1 Q2

quick.orange.fox           Q1

lazy.pink.rabbit              Q2  (值得注意的是，虽然这个routingKey与Q2的两个bindingKey都匹配，但是只会投递Q2一次)

quick.brown.fox            不匹配任意队列，被丢弃

quick.orange.male.rabbit   不匹配任意队列，被丢弃

orange         不匹配任意队列，被丢弃

下面我们以指定Routing key="quick.orange.rabbit"为例，验证上面的答案

#### Send

```java
/**
 * @author huzy
 * @date 2021/07/1214:35
 */
public class Send {
    private final static String EXCHANGE_NAME = "topic_exchange";

    public static void main(String[] argv) throws Exception {
        // 获取到连接
        Connection connection = ConnectionUtil.getConnection();
        // 获取通道
        Channel channel = connection.createChannel();
        // 声明exchange，指定类型为topic
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        // 消息内容
        String message = "这是一只行动迅速的橙色的兔子";
        // 发送消息，并且指定routing key为：quick.orange.rabbit
        channel.basicPublish(EXCHANGE_NAME, "quick.orange.rabbit", null, message.getBytes());
        System.out.println(" [动物描述：] Sent '" + message + "'");

        channel.close();
        connection.close();
    }
}

```

#### Recv

```java
/**
 * @author huzy
 * @date 2021/07/1214:42
 */
public class Recv {
    private final static String QUEUE_NAME = "topic_exchange_queue_Q1";
    private final static String EXCHANGE_NAME = "topic_exchange";

    public static void main(String[] argv) throws Exception {
        // 获取到连接
        Connection connection = ConnectionUtil.getConnection();
        // 获取通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        // 绑定队列到交换机，同时指定需要订阅的routing key。订阅所有的橙色动物
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "*.orange.*");

        // 定义队列的消费者
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            // 获取消息，并且处理，这个方法类似事件监听，如果有消息的时候，会被自动调用
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body);
                System.out.println(" [消费者1] received : " + msg + "!");
            }
        };
        // 监听队列，自动ACK
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}

```

#### Recv2


```java
/**
 * @author huzy
 * @date 2021/07/1214:42
 */
public class Recv2 {
    private final static String QUEUE_NAME = "topic_exchange_queue_Q2";
    private final static String EXCHANGE_NAME = "topic_exchange";

    public static void main(String[] argv) throws Exception {
        // 获取到连接
        Connection connection = ConnectionUtil.getConnection();
        // 获取通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        // 绑定队列到交换机，同时指定需要订阅的routing key。订阅关于兔子以及懒惰动物的消息
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "*.*.rabbit");
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "lazy.＃");

        // 定义队列的消费者
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            // 获取消息，并且处理，这个方法类似事件监听，如果有消息的时候，会被自动调用
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body);
                System.out.println(" [消费者2] received : " + msg + "!");
            }
        };
        // 监听队列，自动ACK
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}

```



控制台：

消费者1、2都获取到了消息

![image-20210712144644828](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712144644828.png)

![image-20210712144631970](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712144631970.png)

### 2.6 PRC模式

PRC（Remote procedure call远程过程调用）模式

![image-20210712152152391](https://huzy-node-image.oss-cn-beijing.aliyuncs.com/img/image-20210712152152391.png)

基本概念：

Callback queue 回调队列，客户端向服务器发送请求，服务器端处理请求后，将其处理结果保存在一个存储体中。而客户端为了获得处理结果，那么客户在向服务器发送请求时，同时发送一个回调队列地址reply_to。

Correlation id 关联标识，客户端可能会发送多个请求给服务器，当服务器处理完后，客户端无法辨别在回调队列中的响应具体和那个请求时对应的。为了处理这种情况，客户端在发送每个请求时，同时会附带一个独有correlation_id属性，这样客户端在回调队列中根据correlation_id字段的值就可以分辨此响应属于哪个请求。

流程说明：

当客户端启动的时候，它创建一个匿名独享的回调队列。
在 RPC 请求中，客户端发送带有两个属性的消息：一个是设置回调队列的 reply_to 属性，另一个是设置唯一值的 correlation_id 属性。
将请求发送到一个 rpc_queue 队列中。
服务器等待请求发送到这个队列中来。当请求出现的时候，它执行他的工作并且将带有执行结果的消息发送给 reply_to 字段指定的队列。
客户端等待回调队列里的数据。当有消息出现的时候，它会检查 correlation_id 属性。如果此属性的值与请求匹配，将它返回给应用。

实践代码可以通过下方链接查看。

[RabbitMQ消息队列之RPC调用_baomw的博客-CSDN博客_rabbitmq rpc](https://blog.csdn.net/baomw/article/details/84864021?ops_request_misc=%7B%22request%5Fid%22%3A%22162607421816780366597930%22%2C%22scm%22%3A%2220140713.130102334..%22%7D&request_id=162607421816780366597930&biz_id=0&utm_medium=distribute.pc_search_result.none-task-blog-2~all~baidu_landing_v2~default-1-84864021.first_rank_v2_pc_rank_v29&utm_term=RabbitMQ+PRC&spm=1018.2226.3001.4187)

参考：

[RabbitMQ (rabbitmq.com)](https://www.rabbitmq.com/)

[RabbitMQ快速入门（详细）_kavito的博客-CSDN博客_rabbitmq](https://blog.csdn.net/kavito/article/details/91403659)

[RabbitMQ消息队列之RPC调用_baomw的博客-CSDN博客_rabbitmq rpc](https://blog.csdn.net/baomw/article/details/84864021?ops_request_misc=%7B%22request%5Fid%22%3A%22162607421816780366597930%22%2C%22scm%22%3A%2220140713.130102334..%22%7D&request_id=162607421816780366597930&biz_id=0&utm_medium=distribute.pc_search_result.none-task-blog-2~all~baidu_landing_v2~default-1-84864021.first_rank_v2_pc_rank_v29&utm_term=RabbitMQ+PRC&spm=1018.2226.3001.4187)

