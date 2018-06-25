package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.io.Map;
import io.anuke.mindustry.io.MapMeta;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.mapgen.WorldGenerator;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.scene.utils.Elements;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class LevelDialog extends FloatingDialog{
	
	public LevelDialog(){
		super("$text.level.select");
		addCloseButton();
		shown(this::setup);
	}
	
	void setup(){
		content().clear();

		Table maps = new Table();
		ScrollPane pane = new ScrollPane(maps);
		pane.setFadeScrollBars(false);
		
		int maxwidth = 4;
		
		Table selmode = new Table();
		ButtonGroup<TextButton> group = new ButtonGroup<>();
		selmode.add("$text.level.mode").padRight(15f);
		
		for(GameMode mode : GameMode.values()){
			TextButton[] b = {null};
			b[0] = Elements.newButton("$mode." + mode.name() + ".name", "toggle", () -> state.mode = mode);
			b[0].update(() -> b[0].setChecked(state.mode == mode));
			b[0].setDisabled(true);
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
		for(Map map : world.maps().all()){

			if(i % maxwidth == 0){
				maps.row();
			}
			
			Table inset = new Table("pane-button");
			inset.add("[accent]" + Bundles.get("map."+map.name+".name", map.name)).pad(3f);
			inset.row();
			inset.label((() -> Bundles.format("text.level.highscore", Settings.getInt("hiscore" + map.name, 0)))).pad(3f);
			inset.pack();
			
			float images = 154f;
			
			Stack stack = new Stack();
			
			Image back = new Image("white");
			back.setColor(Color.valueOf("646464"));
			
			ImageButton image = new ImageButton(new TextureRegion(map.texture), "togglemap");
			image.row();
			image.add(inset).width(images+6);

			image.clicked(() -> {
				hide();
				control.playMap(map);
			});
			image.getImageCell().size(images);
			
			stack.add(back);
			stack.add(image);
			
			maps.add(stack).width(170).top().pad(4f);
			
			i ++;
		}

		maps.addImageButton("icon-editor", 16*4, () -> {
			hide();

			ui.loadfrag.show();

			Timers.run(5f, () -> {
				Cursors.restoreCursor();
				threads.run(() -> {
					Timers.mark();
					MapTileData data = WorldGenerator.generate();
					Map map = new Map("generated-map", new MapMeta(0, new ObjectMap<>(), data.width(), data.height(), null), true, () -> null);

					logic.reset();

					world.beginMapLoad();
					world.setMap(map);

					int width = map.meta.width, height = map.meta.height;

					Tile[][] tiles = world.createTiles(data.width(), data.height());

					EntityPhysics.resizeTree(0, 0, width * tilesize, height * tilesize);

					WorldGenerator.generate(tiles, data, true, Mathf.random(9999999));

					world.endMapLoad();

					Log.info("Time to generate: {0}", Timers.elapsed());

					logic.play();

					Gdx.app.postRunnable(ui.loadfrag::hide);
				});
			});
		}).size(170, 154f + 73).pad(4f);
		
		content().add(pane).uniformX();
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
