package quon;

/**
 * JavaQuON
 * 
 * Information about remote entities needed by the node class. 
 */
public class RemoteEntity extends Entity {
	public long lastContact;
	public boolean requestedUpdates;
	
	public RemoteEntity(Identifier identifier) {
		super(identifier);
	}
}
