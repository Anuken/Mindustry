package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Array;

import io.anuke.ucore.scene.ui.Button;

public class PressGroup{
	private Array<Button> buttons = new Array<>();
	private boolean active = true;
	
	public void add(Button button){
		//TODO make only one button in the group be clickable, add implementation
		buttons.add(button);
	}
	
}
