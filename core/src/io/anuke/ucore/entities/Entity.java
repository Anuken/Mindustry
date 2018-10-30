package io.anuke.ucore.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Position;

public abstract class Entity implements Position{
	private static int lastid;
	
	protected transient EntityGroup<?> group;

	/**Do not modify. Used for network operations.*/
	public int id;
	public float x,y;
	
	public void update(){}
	public void draw(){}
	public void drawOver(){}
	public void removed(){}
	public void added(){}
	
	public Entity(){
		id = lastid++;
	}
	
	public <T extends Entity> T set(float x, float y){
		this.x = x;
		this.y = y;
		return (T)this;
	}
	
	public <T extends Entity> T add(EntityGroup group){
		group.add(this);
		return (T)this;
	}
	
	public <T extends Entity> T add(){
		return (T) add(Entities.defaultGroup());
	}
	
	public Entity remove(){
		if(group != null)
			((EntityGroup)group).remove(this);
		removed();
		return this;
	}

	public EntityGroup<?> getGroup() {
		return group;
	}

	public float angleTo(Entity other){
		return Mathf.atan2(other.x - x, other.y - y);
	}
	
	public float angleTo(Entity other, float yoffset){
		return Mathf.atan2(other.x - x, other.y - (y+yoffset));
	}
	
	public float angleTo(float ox, float oy){
		return Mathf.atan2(ox - x, oy - y);
	}
	
	public float angleTo(Entity other, float xoffset, float yoffset){
		return Mathf.atan2(other.x - (x+xoffset), other.y - (y+yoffset));
	}
	
	public float distanceTo(Entity other){
		return Vector2.dst(other.x, other.y, x, y);
	}
	
	public float distanceTo(float ox, float oy){
		return Vector2.dst(ox, oy, x, y);
	}
	
	public float drawSize(){
		return 20;
	}
	
	public boolean isAdded(){
		return group != null;
	}

	@Override
	public float getX() {
		return x;
	}

	@Override
	public float getY() {
		return y;
	}

	@Override
	public String toString(){
		return getClass() + " " + id;
	}
}
