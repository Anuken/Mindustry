package io.anuke.mindustry.ui;

import static io.anuke.mindustry.Vars.ui;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.ui.ConfirmDialog;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MenuDialog extends FloatingDialog{
	private SaveDialog save = new SaveDialog();
	private LoadDialog load = new LoadDialog();
	public boolean wasPaused = false;

	public MenuDialog() {
		super("Paused");
		setup();
	}

	void setup(){
		shown(() -> {
			wasPaused = GameState.is(State.paused);
			GameState.set(State.paused);
		});
		
		if(!Vars.android){
			content().defaults().width(220).height(50).units(Unit.dp);

			content().addButton("Back", () -> {
				hide();
				if(!wasPaused)
					GameState.set(State.playing);
			});

			content().row();
			content().addButton("Settings", () -> {
				ui.showPrefs();
			});

			if(Gdx.app.getType() != ApplicationType.WebGL){
				content().row();
				content().addButton("Save Game", () -> {
					save.show();
				});

				content().row();
				content().addButton("Load Game", () -> {
					load.show();
				});
			}

			content().row();
			content().addButton("Quit", () -> {
				new ConfirmDialog("Confirm", "Are you sure you want to quit?", () -> {
					hide();
					GameState.set(State.menu);
				}){
					{
						for(Cell<?> cell : getButtonTable().getCells())
							cell.pad(3).size(180, 44).units(Unit.dp);
					}
				}.show();
			});

		}else{
			build.begin(content());
			
			content().defaults().size(120f).pad(5).units(Unit.dp);
			float isize = Unit.dp.inPixels(14f*4);
			
			new imagebutton("icon-play-2", isize, () -> {
				hide();
				if(!wasPaused)
					GameState.set(State.playing);
			}).text("Back").padTop(4f);
			
			new imagebutton("icon-tools", isize, () -> ui.showPrefs()).text("Settings").padTop(4f);
			
			new imagebutton("icon-save", isize, ()-> save.show()).text("Save").padTop(4f);
			
			new imagebutton("icon-load", isize, () -> load.show()).text("Load").padTop(4f);
			
			new imagebutton("icon-quit", isize, () -> {
				new ConfirmDialog("Confirm", "Are you sure you want to quit?", () -> {
					hide();
					GameState.set(State.menu);
				}){{
					for(Cell<?> cell : getButtonTable().getCells())
						cell.pad(3).size(180, 44).units(Unit.dp);
				}}.show();
			}).text("Quit").padTop(4f);
			
			build.end();
		}
	}
}
