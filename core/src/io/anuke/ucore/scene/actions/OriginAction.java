package io.anuke.ucore.scene.actions;

import com.badlogic.gdx.utils.Align;

import io.anuke.ucore.scene.Action;

public class OriginAction extends Action{

	@Override
	public boolean act(float delta){
		actor.setOrigin(Align.center);
		return true;
	}

}
