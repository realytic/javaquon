package quon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * JavaQuON
 * 
 * Two dimensional position class for all moving objects in JavaQuON.
 */
public class Position {
	public static final int QUADRANTS = 4;
	public static final int SIZE = 2 * Long.SIZE;
	
	public long x;
	public long y;
	
	public long old_x;
	public long old_y;
	
	public int classify(Position position) {
		int quadrant = 0;
		
		if (position.x > x) quadrant += 1;
		if (position.y > y) quadrant += 2;
		
		return quadrant;
	}
	
	public long distanceTo(Position position) {
		long dx = Math.abs(position.x - x);
		long dy = Math.abs(position.y - y);
		return Math.max(dx, dy);
	}
	
	public long oldDistanceTo(Position position) {
		long dx = Math.abs(position.x - old_x);
		long dy = Math.abs(position.y - old_y);
		return Math.max(dx, dy);
	}
	
	public Position() {
		x = 0;
		y = 0;
	}
	
	public Position(long x, long y) {
		this.x = x;
		this.y = y;
	}
	
	public Position(byte[] data, int offset) {
		ByteBuffer buffer = ByteBuffer.wrap(data, offset, SIZE).order(ByteOrder.BIG_ENDIAN);
		x = buffer.getLong();
		y = buffer.getLong();
	}

	public void insertData(byte[] data, int offset) {
		ByteBuffer buffer = ByteBuffer.wrap(data, offset, SIZE).order(ByteOrder.BIG_ENDIAN);
		buffer.putLong(x);
		buffer.putLong(y);
	}
	
	public void setTo(Position position) {
		old_x = x;
		old_y = y;
		x = position.x;
		y = position.y;
	}
}
