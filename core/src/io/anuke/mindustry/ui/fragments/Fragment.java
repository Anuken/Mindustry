package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.scene.Group;

public abstract class Fragment{

	public Fragment(){
		Gdx.app.postRunnable(() -> Vars.ui.addFragment(this));
	}

	public abstract void build(Group parent);

	public void onResize(){

	}
}
