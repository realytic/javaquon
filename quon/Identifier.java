package quon;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * JavaQuON
 *
 * In JavaQuON, a peer is identified by its IPv4 address and port number.
 */
public class Identifier {
	public static final int SIZE = 4 + Integer.SIZE;
	
	public final InetAddress address;
	public final int port;
	
	public Identifier(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public Identifier(byte[] data, int offset) {
		InetAddress temp = null;
		try {
			temp = InetAddress.getByAddress(Arrays.copyOfRange(data, offset, offset + 4));
		} catch (UnknownHostException e) { }
		address = temp; 
		port    = ByteBuffer.wrap(data, offset + 4, Integer.SIZE).order(ByteOrder.BIG_ENDIAN).getInt();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Identifier other = (Identifier) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
	public void insertData(byte data[], int offset) {
		byte[] ip = address.getAddress();
		data[offset    ] = ip[0];
		data[offset + 1] = ip[1];
		data[offset + 2] = ip[2];
		data[offset + 3] = ip[3];
		ByteBuffer.wrap(data, offset + 4, Integer.SIZE).order(ByteOrder.BIG_ENDIAN).putInt(port);
	}
	
	private String ipToString(byte[] ip) {
		return (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF);
	}
	
	public String toString() {
		
		return "[" + ipToString(address.getAddress()) + ":" + port + "]"; 
	}
}
