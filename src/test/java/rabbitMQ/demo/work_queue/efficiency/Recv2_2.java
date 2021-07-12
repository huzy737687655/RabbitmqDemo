package rabbitMQ.demo.work_queue.efficiency;

import com.rabbitmq.client.*;
import rabbitMQ.utils.ConnectionUtil;

import java.io.IOException;

/**
 * @author huzy
 * @date 2021/07/1116:25
 * Recv*_1 : 能做多劳模式 即使用了channel.basicQos(1);并使用手动ACK模式
 * work_queue 方式中模拟 处理较快的一方消费者 ，与消费者1的区别是没有等待
 */
public class Recv2_2 {
    private final static String QUEUE_NAME = "word_queue";

    public static void main(String[] args) throws Exception{
        System.out.println("work_queue  Recv2");
        // 获取连接
        Connection connection = ConnectionUtil.getConnection();
        // 获取通道
        Channel channel = connection.createChannel();
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        // 设置最大同时处理任务数量
        channel.basicQos(1);
        // 重写处理方法
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                // body 即消息体
                String msg = new String(body,"utf-8");
                System.out.println(" [消费者2] received : " + msg + "!");
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        // 添加监听
        channel.basicConsume(QUEUE_NAME, false, consumer);
    }
}
