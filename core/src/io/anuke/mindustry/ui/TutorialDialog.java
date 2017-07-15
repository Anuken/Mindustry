package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.*;

import io.anuke.mindustry.GameState;
import io.anuke.mindustry.GameState.State;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.TextDialog;

public class TutorialDialog extends TextDialog{
	
	public TutorialDialog(){
		super("Tutorial", android ? androidTutorialText : tutorialText);
		setup();
	}
	
	void setup(){
		setDialog();
		
		hidden(()->{
			GameState.set(State.playing);
		});
		
		getButtonTable().addButton("OK", ()->{
			hide();
		});
		
		content().pad(8);
		
		content().row();
		content().addCheck("Don't show again", b->{
			Settings.putBool("tutorial", !b);
			Settings.save();
		}).padTop(4);
	}
}
