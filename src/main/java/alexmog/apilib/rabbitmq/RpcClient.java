package alexmog.apilib.rabbitmq;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import alexmog.apilib.rabbitmq.packets.Packet;
import io.netty.buffer.Unpooled;

public class RpcClient {
	private final String mReplyQueue;
	private final Channel mChannel;
	private final RabbitMQDecoder mDecoder;
	private final String mRequestQueueName;
	
	public RpcClient(Channel channel, RabbitMQDecoder decoder, String requestQueueName) throws IOException {
		mReplyQueue = channel.queueDeclare().getQueue();
		mChannel = channel;
		mDecoder = decoder;
		mRequestQueueName = requestQueueName;
	}
	
	public Packet call(Packet cmd) throws IOException, InterruptedException {
		final String corrId = UUID.randomUUID().toString();
		
		AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
				.correlationId(corrId)
				.replyTo(mReplyQueue)
				.build();
		
		mChannel.basicPublish("", mRequestQueueName, props, RabbitMQEncoder.encode(cmd));
		
		final BlockingQueue<Packet> response = new ArrayBlockingQueue<>(1);
		
		mChannel.basicConsume(mRequestQueueName, true, new DefaultConsumer(mChannel) {
			@Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (properties.getCorrelationId().equals(corrId)) response.offer(mDecoder.decode(Unpooled.wrappedBuffer(body)));
            }
		});
		return response.take();
	}
}
