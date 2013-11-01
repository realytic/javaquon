package quon;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

/**
 * JavaQuON
 * 
 * The node class implements the QuON protocol. Peers create a JavaQuON node initialized with
 * their local entity and enter the network using the join method via the identifier of an
 * already participating node or they create a new network by passing null.
 */
public class Node extends Thread {
	private final Entity localEntity;
	
	private final HashMap<Identifier,RemoteEntity> neighbors = new HashMap<Identifier,RemoteEntity>();
	
	private final HashSet<RemoteEntity> directNeighbors    = new HashSet<RemoteEntity>();
	private final RemoteEntity[]        bindingNeighbors   = new RemoteEntity[Position.QUADRANTS];
	private final HashSet<RemoteEntity> temporaryNeighbors = new HashSet<RemoteEntity>();
	
	private DatagramSocket socket;
	private CountDownLatch connected = new CountDownLatch(1);
	
	private int updateInterval = 167; /* send position updated six times a second */
	private int timeout        = 10000; /* after which time (ms) a peer should be detected as inactive */ 
	private TimerTask updateTask;
	private Timer updateTimer;
	
	public Node(Entity entity) {
		this.localEntity = entity;
	}
	
	public void run() {
		while(true) {
			Packet packet = new Packet();
			packet.receive(socket);
			handlePacket(packet);
		}
	}
	
	public void join(Identifier identifier) {
		try {
			socket = new DatagramSocket(localEntity.identifier.port, localEntity.identifier.address);
		} catch (SocketException e) {
			return;
		}
		
		start();
		
		if (identifier != null) {
			Packet packet = new Packet();
			packet.setJoinPacket(localEntity);
			packet.send(identifier, socket);
		
			try {
				connected.await();
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
		

		updateTask = new TimerTask() {
			@Override
			public void run() {
				sendPositionUpdates();
				checkForTimeOuts();
			}
		};
		updateTimer = new Timer();
		updateTimer.schedule(updateTask, 0, updateInterval);
	}
	
	public synchronized ArrayList<Entity> entitiesInAoi() {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		entities.addAll(directNeighbors);
		return entities;
	}
	
	private void updateDirectNeighbors() {
		directNeighbors.clear();
		for (RemoteEntity neighbor : neighbors.values()) {
			if (localEntity.position.distanceTo(neighbor.position) <= localEntity.aoiRadius)
				directNeighbors.add(neighbor);
		}
	}
	
	private void updateBindingNeighbors() {
		for (int quadrant = 0; quadrant < Position.QUADRANTS; quadrant++)
			bindingNeighbors[quadrant] = null;
		
		for (RemoteEntity neighbor : neighbors.values()) {
			int quadrant = localEntity.position.classify(neighbor.position);
			
			if ((bindingNeighbors[quadrant] == null) || 
				(localEntity.position.distanceTo(neighbor.position) < localEntity.position.distanceTo(bindingNeighbors[quadrant].position))) {
				bindingNeighbors[quadrant] = neighbor;
			}
		}
	}
	
	private int isBindingNeighbor(RemoteEntity entity) {
		for (int quadrant = 0; quadrant < Position.QUADRANTS; quadrant++) {
			if (bindingNeighbors[quadrant] == entity)
				return quadrant;
		}
		return -1;
	}
	
	private void updateTemporaryNeighbors() {
		temporaryNeighbors.clear();
		for (RemoteEntity neighbor : neighbors.values()) {
			if ((neighbor.requestedUpdates) && (isBindingNeighbor(neighbor) < 0)) {
				temporaryNeighbors.add(neighbor);
			}
		}
	}
	
	private void updateNeighbors() {
		neighbors.clear();
		for (RemoteEntity neighbor : directNeighbors)
			neighbors.put(neighbor.identifier, neighbor);
		for (int quadrant = 0; quadrant < Position.QUADRANTS; quadrant++)
			if (bindingNeighbors[quadrant] != null)
				neighbors.put(bindingNeighbors[quadrant].identifier, bindingNeighbors[quadrant]);
		for (RemoteEntity neighbor : temporaryNeighbors)
			neighbors.put(neighbor.identifier, neighbor);
	}
	
	private synchronized void sendPositionUpdates() {
		updateDirectNeighbors();
		updateBindingNeighbors();
		updateTemporaryNeighbors();
		
		/* create MOVE packet for direct and temporary neighbors */
		Packet movePacket = new Packet();
		movePacket.setMovePacket(localEntity, false);
		
		/* create MOVE_BNR packet for binding neighbors */
		Packet moveBNRPacket = new Packet();
		moveBNRPacket.setMovePacket(localEntity, true);
		
		/* create NEIGHBOR packets of all binding and temporary neighbors */
		ArrayList<Packet> neighborPackets = new ArrayList<Packet>();
		for (int quadrant = 0; quadrant < Position.QUADRANTS; quadrant++) {
			if (bindingNeighbors[quadrant] == null)
				continue;
			
			Packet packet = new Packet();
			packet.setNeighborPacket(bindingNeighbors[quadrant]);
			neighborPackets.add(packet);
		}
		for (RemoteEntity entity : temporaryNeighbors) {
			Packet packet = new Packet();
			packet.setNeighborPacket(entity);
			neighborPackets.add(packet);
		}
		
		/* send MOVE packet to all direct neighbors */
		for (RemoteEntity neighbor : directNeighbors) {
			if ((isBindingNeighbor(neighbor) < 0) && !temporaryNeighbors.contains(neighbor))
				movePacket.send(neighbor.identifier, socket);
		}
		
		/* send MOVE_BNR and NEIGHBOR packets to all binding neighbors */
		for (int quadrant = 0; quadrant < Position.QUADRANTS; quadrant++) {
			if (bindingNeighbors[quadrant] == null)
				continue;
			
			moveBNRPacket.send(bindingNeighbors[quadrant].identifier, socket);
			
			for (Packet packet : neighborPackets) {
				if (!packet.getIdentifier().equals(bindingNeighbors[quadrant].identifier))
					packet.send(bindingNeighbors[quadrant].identifier, socket);
			}
		}
		
		/* send MOVE and NEIGHBOR packets to all temporary neighbors */
		for (RemoteEntity neighbor : temporaryNeighbors) {
			movePacket.send(neighbor.identifier, socket);
			
			for (Packet packet : neighborPackets) {
				if (!packet.getIdentifier().equals(neighbor.identifier))
					packet.send(neighbor.identifier, socket);
			}
		}
		
		updateNeighbors();
	}
	
	private synchronized void checkForTimeOuts() {
		ArrayList<RemoteEntity> timeouts = new ArrayList<RemoteEntity>();
		
		long currentTime = System.currentTimeMillis();
		
		for (RemoteEntity entity : neighbors.values()) {
			if (entity.lastContact - currentTime >= timeout) {
				int quadrant = isBindingNeighbor(entity);
				if (quadrant >= 0) {
					bindingNeighbors[quadrant] = null;
				}
				
				timeouts.add(entity);
			}
		}
		
		for (Entity entity : timeouts) {
			neighbors.remove(entity.identifier);
		}
		directNeighbors.removeAll(timeouts);
		temporaryNeighbors.removeAll(timeouts);
	}
	
	private void notifyAboutNewNeighbors(RemoteEntity entity) {
		for (RemoteEntity neighbor : directNeighbors) {
			if (neighbor == entity) 
				continue;
			if ((entity.position.distanceTo(neighbor.position) <= entity.aoiRadius) &&
				(entity.position.oldDistanceTo(neighbor.position) > entity.aoiRadius)) {
				Packet packet = new Packet();
				packet.setNeighborPacket(neighbor);
				packet.send(entity.identifier, socket);
			}
		}
	}
	
	private void handleMovePacket(Packet packet, boolean bindingNeighborRequest) {
		Identifier identifier = packet.getIdentifier();
		
		if (!neighbors.containsKey(identifier))
			neighbors.put(identifier, new RemoteEntity(identifier));
		
		RemoteEntity entity = neighbors.get(identifier);
		entity.position.setTo(packet.getPosition());
		entity.aoiRadius = packet.getAoiRadius();
		entity.lastContact = System.currentTimeMillis();
		entity.requestedUpdates = bindingNeighborRequest;
		
		if (directNeighbors.contains(identifier)) {
			notifyAboutNewNeighbors(neighbors.get(identifier));
		}
	}
	
	private void handleNeighborPacket(Packet packet) {
		RemoteEntity origin = neighbors.get(packet.getOrigin());
		if (origin != null)
			origin.lastContact = System.currentTimeMillis();
		
		Identifier identifier = packet.getIdentifier();
		
		if (!neighbors.containsKey(identifier)) {
			neighbors.put(identifier, new RemoteEntity(identifier));
		
			RemoteEntity entity = neighbors.get(identifier);
			entity.position.setTo(packet.getPosition());
			entity.aoiRadius = packet.getAoiRadius();
			entity.lastContact = System.currentTimeMillis();
			entity.requestedUpdates = false;
		}
	}
	
	private void handleJoinPacket(Packet packet) {
		Position position = packet.getPosition();
		Entity nearestEntity = localEntity;
		
		for (RemoteEntity neighbor : neighbors.values()) {
			if (neighbor.position.distanceTo(position) < localEntity.position.distanceTo(position)) 
				nearestEntity = neighbor;
		}
		
		if (nearestEntity != localEntity) {
			/* forward to closest neighbor */
			packet.send(nearestEntity.identifier, socket);
		} else {
			Identifier identifier = packet.getIdentifier();
			
			/* send join acknowledgement */
			Packet ack = new Packet();
			ack.setJoinAckPacket();
			ack.send(identifier, socket);
			
			/* send own info */
			Packet movePacket = new Packet();
			movePacket.setMovePacket(localEntity, false);
			movePacket.send(identifier, socket);
			
			/* inform about all neighbors */
			for (RemoteEntity neighbor : neighbors.values()) {
				Packet neighborPacket = new Packet();
				neighborPacket.setNeighborPacket(neighbor);
				neighborPacket.send(identifier, socket);
			}
		}
	}
	
	private synchronized void handlePacket(Packet packet) {
		switch(packet.getType()) {
		case Packet.MOVE: {
			handleMovePacket(packet, false);
		} break;
		
		case Packet.MOVE_BNR: {
			handleMovePacket(packet, true);
		} break;
		
		case Packet.NEIGHBOR: {
			handleNeighborPacket(packet);
		} break;
		
		case Packet.JOIN: {
			handleJoinPacket(packet);
		}
		
		case Packet.JOIN_ACK: {
			connected.countDown();
		} break;
		
		default: break;
		}
	}
}
