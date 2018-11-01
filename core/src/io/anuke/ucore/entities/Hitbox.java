package io.anuke.ucore.entities;

import com.badlogic.gdx.math.Rectangle;

public class Hitbox{
	private final static float sqrt2 = (float)Math.sqrt(2);
	private final Rectangle rect = new Rectangle();
	public float offsetx, offsety;
	public float width, height;
	/**Whether this is a solid hitbox to be used as a circle collider.*/
	public boolean solid = false;

	public Hitbox(float size) {
		width = height = size;
	}

	public Hitbox(float width, float height) {
		this.width = width;
		this.height = height;
	}

	public Hitbox() {

	}

	public float radius(){
		return (width + height)/2f;
	}

	public void bounds(float offsetx, float offsety, float width, float height){
		this.offsety = offsety;
		this.offsetx = offsetx;
		this.width = width;
		this.height = height;
	}

	public Rectangle getRect(float x, float y, float expansion){
		return rect.setSize(width, height).setCenter(x + offsetx + expansion, y + offsety + expansion);
	}

	public Rectangle getRect(float x, float y){
		return getRect(rect, x, y);
	}
	
	public Rectangle getRect(Rectangle rect, float x, float y){
		return rect.setSize(width, height).setCenter(x + offsetx, y + offsety);
	}

	public void setSize(float size){
		this.width = this.height = size;
	}

	public void setSize(float width, float height){
		this.width = width;
		this.height = height;
	}
}
