package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.event.ClickListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.utils.Elements;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class LevelDialog extends FloatingDialog{
	private Map selectedMap = world.maps().getMap(0);
	private ScrollPane pane;
	
	public LevelDialog(){
		super("$text.level.select");
		getTitleTable().getCell(title()).growX().center();
		getTitleTable().center();
		addCloseButton();
		setup();
	}
	
	public void reload(){
		content().clear();
		setup();
	}
	
	void setup(){
		Table maps = new Table();
		pane = new ScrollPane(maps);
		pane.setFadeScrollBars(false);
		
		int maxwidth = 4;
		
		Table selmode = new Table();
		ButtonGroup<TextButton> group = new ButtonGroup<>();
		selmode.add("$text.level.mode").padRight(15f);
		
		for(GameMode mode : GameMode.values()){
			TextButton[] b = {null};
			b[0] = Elements.newButton("$mode." + mode.name() + ".name", "toggle", () -> state.mode = mode);
			b[0].update(() -> b[0].setChecked(state.mode == mode));
			b[0].getLabelCell().wrap().growX().get().setAlignment(Align.center, Align.left);
			group.add(b[0]);
			selmode.add(b[0]).size(130f, 54f);
		}
		selmode.addButton("?", this::displayGameModeHelp).size(50f, 54f).padLeft(18f);
		
		content().add(selmode);
		content().row();

		Difficulty[] ds = Difficulty.values();

		float s = 50f;

		Table sdif = new Table();

		sdif.add("$setting.difficulty.name").padRight(15f);

		sdif.defaults().height(s+4);
		sdif.addImageButton("icon-arrow-left", 10*3, () -> {
			state.difficulty = (ds[Mathf.mod(state.difficulty.ordinal() - 1, ds.length)]);
		}).width(s);

		sdif.addButton("", () -> {

		}).update(t -> {
			t.setText(state.difficulty.toString());
			t.setTouchable(Touchable.disabled);
		}).width(180f);

		sdif.addImageButton("icon-arrow-right", 10*3, () -> {
			state.difficulty = (ds[Mathf.mod(state.difficulty.ordinal() + 1, ds.length)]);
		}).width(s);

		content().add(sdif);
		content().row();

		int i = 0;
		for(Map map : world.maps().list()){
			
			if(!map.visible && !debug) continue;
			
			if(i % maxwidth == 0){
				maps.row();
			}
			
			Table inset = new Table("pane-button");
			inset.add("[accent]" + Bundles.get("map."+map.name+".name", map.name)).pad(3f);
			inset.row();
			inset.label((() -> Bundles.format("text.level.highscore", Settings.getInt("hiscore" + map.name, 0)))).pad(3f).padLeft(-2).padRight(-2)
					.wrap().growX().get().setAlignment(Align.center, Align.left);
			inset.pack();
			
			float images = 154f;
			
			Stack stack = new Stack();
			
			Image back = new Image("white");
			back.setColor(map.backgroundColor);
			
			ImageButton image = new ImageButton(new TextureRegion(map.texture), "togglemap");
			image.row();
			image.add(inset).width(images+6);
			TextButton[] delete = new TextButton[1];
			if(map.custom){
				image.row();
				delete[0] = image.addButton("Delete", () -> {
					Timers.run(1f, () -> {
						ui.showConfirm("$text.level.delete.title", Bundles.format("text.level.delete", Bundles.get("map."+map.name+".name", map.name)), () -> {
							world.maps().removeMap(map);
							reload();
							Core.scene.setScrollFocus(pane);
						});
					});
				}).width(images+16).padBottom(-10f).grow().get();
			}

			Vector2 hit = new Vector2();
			
			image.addListener(new ClickListener(){
				public void clicked(InputEvent event, float x, float y){
					image.localToStageCoordinates(hit.set(x, y));
					if(delete[0] != null && (delete[0].getClickListener().isOver() || delete[0].getClickListener().isPressed()
							|| (Core.scene.hit(hit.x, hit.y, true) != null &&
							Core.scene.hit(hit.x, hit.y, true).isDescendantOf(delete[0])))){
						return;
					}
					
					selectedMap = map;
					hide();
					control.playMap(selectedMap);
				}
			});
			image.getImageCell().size(images);
			
			stack.add(back);
			stack.add(image);
			
			maps.add(stack).width(170).top().pad(4f);
			
			maps.marginRight(26);
			
			i ++;
		}
		
		content().add(pane).uniformX();
		
		shown(()->{
			//this is necessary for some reason?
			Timers.run(2f, ()->{
				Core.scene.setScrollFocus(pane);
			});
		});
	}

	private void displayGameModeHelp() {
		FloatingDialog d = new FloatingDialog(Bundles.get("mode.text.help.title"));
		d.setFillParent(false);
		Table table = new Table();
		table.defaults().pad(1f);
		ScrollPane pane = new ScrollPane(table, "clear");
		pane.setFadeScrollBars(false);
		table.row();
		for(GameMode mode : GameMode.values()){
			table.labelWrap("[accent]" + mode.toString() + ":[] [lightgray]" + mode.description()).width(600f);
			table.row();
		}

		d.content().add(pane);
		d.buttons().addButton("$text.ok", d::hide).size(110, 50).pad(10f);
		d.show();
	}

}
