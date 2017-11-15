package alexmog.apilib.rabbitmq;

import java.nio.ByteOrder;

import alexmog.apilib.rabbitmq.packets.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class RabbitMQEncoder {
	public static byte[] encode(Packet packet) {
		@SuppressWarnings("deprecation")
		ByteBuf buf = Unpooled.buffer().order(ByteOrder.LITTLE_ENDIAN);
		buf.writeInt(packet.packetId);
		packet.writeData(buf);
		return buf.array();
	}
}
