package alexmog.apilib.managers;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP.BasicProperties;

import alexmog.apilib.Server;
import alexmog.apilib.config.DatabasesConfig;
import alexmog.apilib.managers.Managers.Manager;
import alexmog.apilib.rabbitmq.RabbitMQEncoder;
import alexmog.apilib.rabbitmq.packets.Packet;

@Manager
public class RabbitMQManager extends alexmog.apilib.managers.Manager {
	private Connection mConnection = null;
	private Channel mChannel = null;

	public Connection getConnection() {
		return mConnection;
	}
	
	public Channel getChannel() {
		return mChannel;
	}
	
	@Override
	public void shutdown() {
			try {
				if (mChannel != null) mChannel.close();
				if (mConnection != null) mConnection.close();
			} catch (IOException | TimeoutException e) {
				Server.LOGGER.log(Level.WARNING, "RabbitMQ Exception", e);
			}
	}

	@Override
	public void init(Properties config, DatabasesConfig databasesConfig) throws Exception {
		if (!config.getProperty("rabbitmq.active", "false").equals("true")) {
			Server.LOGGER.info("RabbitMQ active is set to false. Didn't activate the manager.");
			return;
		}
		Server.LOGGER.info("Connecting to RabbitMQ server...");
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri(config.getProperty("rabbitmq.uri"));
		
		mConnection = factory.newConnection();
		mChannel = mConnection.createChannel();
		Server.LOGGER.info("RabbitMQ started.");
	}

	public void basicPublish(String exchange, String routingKey, BasicProperties properties, Packet packet) {
		try {
			mChannel.basicPublish(exchange, routingKey, properties, RabbitMQEncoder.encode(packet));
		} catch (IOException e) {
			Server.LOGGER.log(Level.WARNING, "Cannot publish on rabbitMQ", e);
		}
	}
}
