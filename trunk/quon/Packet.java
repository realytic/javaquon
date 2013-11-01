package quon;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * JavaQuON
 * 
 * Packet class encapsulating JavaQuON UDP datagrams.
 */
public class Packet {
	public static final byte MOVE     =  0;
	public static final byte MOVE_BNR =  1;
	public static final byte NEIGHBOR =  2;
	public static final byte JOIN	  =  3;
	public static final byte JOIN_ACK =  4;
	public static final byte LEAVE    =  5;
	public static final byte INVALID  =  6;
	
	public static final String[] NAMES = {"MOVE",
										  "MOVE_BNR",
										  "NEIGHBOR",
										  "JOIN",
										  "JOIN_ACK",
										  "LEAVE",
										  "INVALID"};

	/* | TYPE | POSITION | AOI_RADIUS [| IDENTIFIER ] */
	public final static int TYPE       = 0;
	public final static int POSITION   = TYPE + Byte.SIZE;
	public final static int AOI_RADIUS = POSITION + Position.SIZE;
	public final static int IDENTIFIER = AOI_RADIUS + Long.SIZE;
	
	public final static int SIZE_MOVE_PACKET     = IDENTIFIER + 1;
	public final static int SIZE_NEIGHBOR_PACKET = IDENTIFIER + Identifier.SIZE + 1;
	public final static int SIZE_JOIN_PACKET     = IDENTIFIER + Identifier.SIZE + 1;
	public final static int SIZE_JOIN_ACK_PACKET = TYPE + 1;
	public final static int MAXIMUM_SIZE         = IDENTIFIER + Identifier.SIZE + 1;
	
	public byte[] data;
	public Identifier origin;
	
	public byte getType() {
		switch(data[TYPE]) {
		case MOVE:
		case MOVE_BNR:
		case NEIGHBOR: 
		case JOIN:
		case JOIN_ACK:
		case LEAVE: return data[TYPE];
		default: return INVALID;
		}
	}
	
	public Position getPosition() {
		return new Position(data, POSITION);
	}
	
	public long getAoiRadius() {
		return ByteBuffer.wrap(data, AOI_RADIUS, Long.SIZE).order(ByteOrder.BIG_ENDIAN).getLong();
	}
	
	public Identifier getIdentifier() {
		if ((getType() == NEIGHBOR) || (getType() == JOIN))
			return new Identifier(data, IDENTIFIER);
		else
			return origin;
	}
	
	public Identifier getOrigin() {
		return origin;
	}
	
	public void setMovePacket(Entity entity, boolean bindingNeighborRequest) {
		data = new byte[SIZE_MOVE_PACKET];
		if (bindingNeighborRequest) {
			data[TYPE] = MOVE_BNR;
		} else {
			data[TYPE] = MOVE;
		}
		entity.position.insertData(data, POSITION);
		ByteBuffer.wrap(data, AOI_RADIUS, Long.SIZE).order(ByteOrder.BIG_ENDIAN).putLong(entity.aoiRadius);
	}
	
	public void setNeighborPacket(RemoteEntity entity) {
		data = new byte[SIZE_NEIGHBOR_PACKET];
		data[TYPE] = NEIGHBOR;
		entity.position.insertData(data, POSITION);
		ByteBuffer.wrap(data, AOI_RADIUS, Long.SIZE).order(ByteOrder.BIG_ENDIAN).putLong(entity.aoiRadius);
		entity.identifier.insertData(data, IDENTIFIER);
	}
	
	public void setJoinPacket(Entity entity) {
		data = new byte[SIZE_JOIN_PACKET];
		data[TYPE] = JOIN;
		entity.position.insertData(data, POSITION);
		ByteBuffer.wrap(data, AOI_RADIUS, Long.SIZE).order(ByteOrder.BIG_ENDIAN).putLong(entity.aoiRadius);
		entity.identifier.insertData(data, IDENTIFIER);
	}
	
	public void setJoinAckPacket() {
		data = new byte[SIZE_JOIN_ACK_PACKET];
		data[TYPE] = JOIN_ACK;
	}
	
	public void receive(DatagramSocket socket) {
		data = new byte[MAXIMUM_SIZE];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		
		try {
			socket.receive(packet);
		} catch (IOException e) { e.printStackTrace(); }
		
		origin = new Identifier(packet.getAddress(), packet.getPort());
	}
	
	public void send(Identifier identifier, DatagramSocket socket) {
		DatagramPacket packet = new DatagramPacket(data, data.length, identifier.address, identifier.port);
		
		try {
			socket.send(packet);
		} catch (IOException e) { e.printStackTrace(); }
	}
}
