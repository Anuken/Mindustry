package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Align;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class LoadDialog extends FloatingDialog{
	ScrollPane pane;

	public LoadDialog() {
		this("$text.loadgame");
	}

	public LoadDialog(String title) {
		super(title);
		setup();

		shown(() -> {
			setup();
			Timers.runTask(2f, () -> Core.scene.setScrollFocus(pane));
		});

		addCloseButton();
	}

	private void setup(){
		content().clear();

		content().add("$text.selectslot").padBottom(2);
		content().row();

		Table slots = new Table();
		pane = new ScrollPane(slots);
		pane.setFadeScrollBars(false);

		slots.marginRight(24);

		for(int i = 0; i < Vars.saveSlots; i++){

			TextButton button = new TextButton(Bundles.format("text.slot", (i + 1)));
			button.margin(12);
			button.getLabelCell().top().left().growX();

			button.row();

			Label info = new Label("[gray]" + (!SaveIO.isSaveValid(i) ? Bundles.get("text.empty") : SaveIO.getMode(i) + ", "
					+ SaveIO.getMap(i).name + ", " + Bundles.format("text.save.wave", SaveIO.getWave(i)) + "\n"
					+ Bundles.format("text.save.date", SaveIO.getTimeString(i))));
			info.setAlignment(Align.center, Align.center);

			button.add(info).padBottom(3).padTop(7);
			button.row();
			button.row();
			modifyButton(button, i);

			slots.add(button).size(404, 104).pad(4);
			slots.row();
		}

		content().add(pane);

	}

	public void modifyButton(TextButton button, int slot){
		button.setDisabled(!SaveIO.isSaveValid(slot));
		button.clicked(() -> {
			if(!button.isDisabled()){
				Vars.ui.showLoading();

				Timers.runTask(3f, () -> {
					Vars.ui.hideLoading();
					hide();
					try{
						SaveIO.loadFromSlot(slot);
						GameState.set(State.playing);
						Vars.ui.hideMenu();
					}catch(Exception e){
						e.printStackTrace();
						Vars.ui.hideMenu();
						GameState.set(State.menu);
						Vars.control.reset();
						Vars.ui.showError("$text.save.corrupted");
						return;
					}
				});
			}
		});
	}
}
