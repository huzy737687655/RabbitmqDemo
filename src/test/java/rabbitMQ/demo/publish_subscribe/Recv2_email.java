package rabbitMQ.demo.publish_subscribe;

import com.rabbitmq.client.*;
import rabbitMQ.utils.ConnectionUtil;

import java.io.IOException;

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
