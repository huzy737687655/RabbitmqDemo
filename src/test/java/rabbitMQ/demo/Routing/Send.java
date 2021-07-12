package rabbitMQ.demo.Routing;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import rabbitMQ.utils.ConnectionUtil;

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
