package rabbitMQ.demo.publish_subscribe;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import rabbitMQ.utils.ConnectionUtil;


/**
 * @author huzy
 * @date 2021/07/1209:39
 *
 * 发布/订阅者模式
 * 需要用到交换机 ： Fanout（广播）
 *  获取连接
 *  创建通道
 *  声明交换机
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
