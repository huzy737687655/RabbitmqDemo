package rabbitMQ.demo.Routing;

import com.rabbitmq.client.*;
import rabbitMQ.utils.ConnectionUtil;

import java.io.IOException;

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
