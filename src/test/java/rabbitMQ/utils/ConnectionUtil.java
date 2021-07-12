package rabbitMQ.utils;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


/**
 * @author huzy
 * @date 2021/07/1109:18
 */
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
        factory.setHost("8.140.111.189");
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
