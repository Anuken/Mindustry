package io.anuke.ucore.scene;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;

import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.function.KeyListenable;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.function.BooleanProvider;
import io.anuke.ucore.scene.event.EventListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.event.ChangeListener;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.utils.Disableable;

/**Extends the BaseElement (Actor) class to provide more functionality.
 * (this is probably a terrible idea)*/
public class Element extends BaseElement{
	private static final Vector2 vec = new Vector2();
	
	protected float alpha = 1f;
	protected Vector2 translation = new Vector2(0, 0);
	private BooleanProvider visibility;
	private Listenable update;
	private Supplier<Touchable> touchableSupplier = null;
	
	@Override
	public void draw (Batch batch, float parentAlpha) {
		validate();
		draw();
	}
	
	/**Simple drawing. Use the alpha variable if needed.*/
	public void draw(){
		
	}
	
	/**Find and draws a drawable by name on the width/height.*/
	protected void patch(String name){
		Draw.patch(name, getX(), getY(), getWidth(), getHeight());
	}
	
	/**Find and draws a drawable by name on the width/height. Padding on the sides is applied.*/
	protected void patch(String name, float padding){
		Draw.patch(name, getX() + padding, getY()+padding, getWidth()-padding*2, getHeight()-padding*2);
	}
	
	@Override
	public void act (float delta) {
		super.act(delta);
		if(visibility != null)
			setVisible(visibility.get());
		if(touchableSupplier != null)
			setTouchable(touchableSupplier.get());
		if(update != null)
			update.listen();
	}
	
	public Vector2 worldPos(){
		return localToStageCoordinates(vec.set(0, 0));
	}

	public void setTranslation(float x, float y){
		translation.x = x;
		translation.y = y;
	}

	public Vector2 getTranslation(){
		return translation;
	}
	
	public void keyDown(int key, Listenable l){
		keyDown(k->{
			if(k == key)
				l.listen();
		});
	}
	
	/**Adds a keydown input listener.*/
	public void keyDown(KeyListenable cons){
		addListener(new InputListener(){
			public boolean keyDown (InputEvent event, int keycode) {
				cons.pressed(keycode);
				return true;
			}
		});
	}
	
	/**Fakes a click event on all ClickListeners.*/
	public void fireClick(){
		for(EventListener listener : getListeners()){
			if(listener instanceof ClickListener){
				((ClickListener)listener).clicked(new InputEvent(), -1, -1);
			}
		}
	}
	
	/**Adds a click listener.*/
	public ClickListener clicked(Listenable r){
		ClickListener click;
		Element elem = this;
		addListener(click = new ClickListener(){
			@Override
			public void clicked(InputEvent event, float x, float y){
				if(r != null && !(elem instanceof Disableable && ((Disableable)elem).isDisabled())) r.listen();
			}
		});
		return click;
	}
	
	/**Adds a touch listener.*/
	public void tapped(Listenable r){
		addListener(new InputListener(){
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				r.listen();
				return true;
			}
		});
	}
	
	/**Adds a mouse up listener.*/
	public void released(Listenable r){
		addListener(new InputListener(){
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				return true;
			}
			
			public void touchUp(InputEvent event, float x, float y, int pointer, int button){
				r.listen();
			}
		});
	}
	
	/**Fires a change event on all listeners.*/
	public void change(){
		fire(new ChangeListener.ChangeEvent());
	}
	
	/**Adds a click listener.*/
	public void changed(Listenable r){
		Element elem = this;
		addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Element actor){
				if(!(elem instanceof Disableable && ((Disableable)elem).isDisabled()))r.listen();
			}
		});
	}
	
	public void update(Listenable r){
		update = r;
	}
	
	public void setVisible(BooleanProvider vis){
		visibility = vis;
	}

	public void setTouchable(Supplier<Touchable> touch){
		this.touchableSupplier = touch;
	}
}
