package quon;

/**
 * JavaQuON
 * 
 * Entity class containing all relevant attributes of a moving object.
 */
public class Entity {
	public final Identifier identifier;
	public final Position position = new Position();
	public long aoiRadius;
	
	public Entity(Identifier identifier) {
		this.identifier = identifier;
	}
	
	@Override
	public int hashCode() {	
		return identifier.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		Entity other = (Entity) obj;
		return other.identifier.equals(identifier);
	}
}
