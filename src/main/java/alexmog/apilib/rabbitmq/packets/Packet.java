package alexmog.apilib.rabbitmq.packets;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;

public abstract class Packet {
	public int packetId;
	
	/**
     * Writes packet data, excluding the packet ID, to the specified buffer.
     * <p>
     *
     * @param buf
     */
    public abstract void writeData(ByteBuf buf);

    /**
     * Reads packet data, excluding the packet ID, from the specified buffer.
     * <p>
     *
     * @param buf
     */
    public abstract void readData(ByteBuf buf);

    public static String readUTF8(ByteBuf in) {
        ByteBuf buffer = in.alloc().buffer();
        byte b;
        while (in.readableBytes() > 0 && (b = in.readByte()) != 0) {
            buffer.writeByte(b);
        }

        return buffer.toString(Charsets.UTF_8);
    }

    @SuppressWarnings("deprecation")
	public static String readUTF16(ByteBuf in) {
        in = in.order(ByteOrder.BIG_ENDIAN);
        ByteBuf buffer = in.alloc().buffer();
        char chr;
        while (in.readableBytes() > 1 && (chr = in.readChar()) != 0) {
            buffer.writeChar(chr);
        }

        return buffer.toString(Charsets.UTF_16LE);
    }

    public static void writeUTF8(ByteBuf out, String s) {
        out.writeBytes(s.getBytes(Charsets.UTF_8));
        out.writeByte(0);
    }

    @SuppressWarnings("deprecation")
	public static void writeUTF16(ByteBuf out, String s) {
        out.order(ByteOrder.BIG_ENDIAN).writeBytes(s.getBytes(Charsets.UTF_16LE));
        out.writeChar(0);
    }
    
   public static void writeObjectArray(ByteBuf out, List<Object> objects) {
        for (Object o : objects) {
            if (o instanceof Byte) {
                out.writeByte((Byte) o);
            } else if (o instanceof Boolean) {
                out.writeBoolean((Boolean) o);
            } else if (o instanceof Integer) {
                out.writeInt((Integer) o);
            } else if (o instanceof Float) {
                out.writeFloat((Float) o);
            } else if (o instanceof Double) {
                out.writeDouble((Double) o);
            } else if (o instanceof Short) {
                out.writeShort((Short) o);
            } else if (o instanceof Long) {
                out.writeLong((Long) o);
            }
        }
    }
    
    public static List<Object> readObjectArray(ByteBuf in, List<Class<?>> types) {
        List<Object> ret = new ArrayList<>();
        for (Class<?> t : types) {
            if (t.isAssignableFrom(Byte.class)) {
                ret.add(in.readByte());
            } else if (t.isAssignableFrom(Boolean.class)) {
                ret.add(in.readBoolean());
            } else if (t.isAssignableFrom(Integer.class)) {
                ret.add(in.readInt());
            } else if (t.isAssignableFrom(Float.class)) {
                ret.add(in.readFloat());
            } else if (t.isAssignableFrom(Double.class)) {
                ret.add(in.readDouble());
            } else if (t.isAssignableFrom(Short.class)) {
                ret.add(in.readShort());
            } else if (t.isAssignableFrom(Long.class)) {
                ret.add(in.readLong());
            }
        }
        return ret;
    }
}
