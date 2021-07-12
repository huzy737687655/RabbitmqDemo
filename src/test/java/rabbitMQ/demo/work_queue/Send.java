package rabbitMQ.demo.work_queue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import rabbitMQ.utils.ConnectionUtil;

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
