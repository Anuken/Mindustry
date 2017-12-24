package io.anuke.mindustry.ui;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.utils.Align;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.UCore;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

public class MindustrySettingsDialog extends SettingsDialog{
	public SettingsTable graphics;
	public SettingsTable game;
	public SettingsTable sound;

	private Table prefs;
	private Table menu;
	private boolean built = false;
	
	public MindustrySettingsDialog(){
		setFillParent(true);
		title().setAlignment(Align.center);
		getTitleTable().row();
		getTitleTable().add(new Image("white"))
		.growX().height(3f).pad(4f).get().setColor(Colors.get("accent"));

		content().clearChildren();
		content().remove();
		buttons().remove();

		menu = new Table();

		Consumer<SettingsTable> s = table -> {
			table.row();
			table.addImageTextButton("Back", "icon-arrow-left", 10*3, this::back).size(240f, 60f).colspan(2).padTop(15f);
		};

		game = new SettingsTable(s);
		graphics = new SettingsTable(s);
		sound = new SettingsTable(s);

		prefs = new Table();
		prefs.top();
		prefs.margin(14f);

		menu.defaults().size(300f, 60f).pad(3f);
		menu.addButton("Game", () -> visible(0));
		menu.row();
		menu.addButton("Graphics", () -> visible(1));
		menu.row();
		menu.addButton("Sound", () -> visible(2));

		//if(!Vars.android) {
		menu.row();
		menu.addButton("Controls", () -> Vars.ui.showControls());
		//}

		prefs.clearChildren();
		prefs.add(menu);

		ScrollPane pane = new ScrollPane(prefs, "clear");
		pane.setFadeScrollBars(false);

		row();
		add(pane).grow().top();
		row();
		add(buttons()).fillX();

		hidden(this::back);
	}

	private void back(){
		prefs.clearChildren();
		prefs.add(menu);
	}

	private void visible(int index){
	    prefs.clearChildren();
	    Table table = Mathf.select(index, game, graphics, sound);
        prefs.add(table);
	}
	
	@Override
	public void addCloseButton(){
		buttons().addImageTextButton("Menu", "icon-arrow-left", 30f, this::hide).size(230f, 64f);
		
		keyDown(key->{
			if(key == Keys.ESCAPE || key == Keys.BACK)
				hide();
		});
	}
}
