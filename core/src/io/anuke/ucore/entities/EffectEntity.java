package io.anuke.ucore.entities;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;

public class EffectEntity extends TimedEntity{
	public Effect renderer;
	public Color color = Color.WHITE;
	public float rotation = 0f;
	
	public EffectEntity(Effect effect){
		renderer = effect;
		lifetime = effect.lifetime;
	}
	
	public EffectEntity(Effect effect, Color color, float rotation){
		this(effect);
		this.color = color;
		this.rotation = rotation;
	}
	
	@Override
	public void drawOver(){
		Effects.renderEffect(id, renderer, color, time, rotation, x, y);
	}
	
}
