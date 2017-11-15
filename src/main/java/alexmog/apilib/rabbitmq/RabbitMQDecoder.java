package alexmog.apilib.rabbitmq;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import alexmog.apilib.Server;
import alexmog.apilib.rabbitmq.packets.Packet;
import io.netty.buffer.ByteBuf;

public class RabbitMQDecoder {
	private final Map<Integer, Class<? extends Packet>> packets = new HashMap<>();
	
	public void RegisterPacket(int packetId, Class<? extends Packet> clazz) {
		packets.put(packetId, clazz);
	}
	
	@SuppressWarnings("deprecation")
	public Packet decode(ByteBuf buf) {
		buf = buf.order(ByteOrder.LITTLE_ENDIAN);
		int packetId = buf.readInt();
		Class<?> c = packets.get(packetId);
		if (c != null) {
			try {
				Packet p = (Packet) c.newInstance();
				p.readData(buf);
				return p;
			} catch (InstantiationException | IllegalAccessException e) {
				Server.LOGGER.log(Level.SEVERE, "Cannot instantiate packet", e);
			}
		} else Server.LOGGER.info("Packet " + packetId + " not registered.");
		return null;
	}
}
