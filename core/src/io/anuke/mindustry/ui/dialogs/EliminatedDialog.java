package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.core.GameState;

import static io.anuke.mindustry.Vars.*;

public class EliminatedDialog extends FloatingDialog{

	public EliminatedDialog(){
		super("$gameover");
		setFillParent(true);
		shown(this::rebuild);
	}

	void rebuild(){
		buttons.clear();
		cont.clear();
		buttons.margin(10);
		cont.add("$eliminated").pad(6);
		buttons.addButton("$menu", () -> {
			hide();
			state.set(GameState.State.menu);
		}).size(130f, 60f);
	}
}
