package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.ui.dialogs.FileChooser;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.SpecialBlocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.imagebutton;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Input;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class MapEditorDialog extends Dialog{
	private MapEditor editor;
	private MapView view;
	private MapGenerateDialog dialog;
	private MapLoadDialog loadDialog;
	private MapSaveDialog saveDialog;
	private MapResizeDialog resizeDialog;
	private ScrollPane pane;
	private FileChooser openFile, saveFile;
	private boolean saved = false;
	
	private ButtonGroup<ImageButton> blockgroup;
	
	public MapEditorDialog(){
		super("$text.mapeditor", "dialog");
		if(gwt) return;

		editor = new MapEditor();
		dialog = new MapGenerateDialog(editor);
		view = new MapView(editor);
		
		openFile = new FileChooser("$text.loadimage", FileChooser.pngFilter, true, this::tryLoadMap);
		
		saveFile = new FileChooser("$saveimage", false, file -> {
			if(!file.extension().toLowerCase().equals(".png")){
				file = file.parent().child(file.nameWithoutExtension() + ".png");
			}
			FileHandle result = file;
			ui.loadfrag.show();
			Timers.run(3f, () -> {
				try{
					Pixmaps.write(editor.pixmap(), result);
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimagesave", Strings.parseException(e, false)));
					if(!mobile) Log.err(e);
				}
				ui.loadfrag.hide();
			});
		});
		
		loadDialog = new MapLoadDialog(map -> {
			saveDialog.setFieldText(map.name);
			ui.loadfrag.show();
			
			Timers.run(3f, () -> {
				Map copy = new Map();
				copy.name = map.name;
				copy.id = -1;
				copy.pixmap = Pixmaps.copy(map.pixmap);
				copy.texture = new Texture(copy.pixmap);
				copy.oreGen = map.oreGen;
				editor.beginEdit(copy);
				ui.loadfrag.hide();
				view.clearStack();
			});
		});
		
		resizeDialog = new MapResizeDialog(editor, (x, y) -> {
			Pixmap pix = editor.pixmap();
			if(!(pix.getWidth() == x && pix.getHeight() == y)){
				ui.loadfrag.show();
				Timers.run(10f, ()->{
					editor.resize(x, y);
					view.clearStack();
					ui.loadfrag.hide();
				});
			}
		});
		
		saveDialog = new MapSaveDialog(name -> {
			ui.loadfrag.show();
			if(verifyMap()){
				saved = true;
				String before = editor.getMap().name;
				editor.getMap().name = name;
				Timers.run(10f, () -> {
					world.maps().saveAndReload(editor.getMap(), editor.pixmap());
					loadDialog.rebuild();
					ui.loadfrag.hide();
					view.clearStack();

					if(!name.equals(before)) {
						Map map = new Map();
						map.name = editor.getMap().name;
						map.oreGen = editor.getMap().oreGen;
						map.pixmap = Pixmaps.copy(editor.getMap().pixmap);
						map.texture = new Texture(map.pixmap);
						map.custom = true;
						editor.beginEdit(map);
					}
				});

			}else{
				ui.loadfrag.hide();
			}
		});
		
		setFillParent(true);
		
		clearChildren();
		margin(0);
		build.begin(this);
		build();
		build.end();

		tapped(() -> {
			Element e = Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true);
			if(e == null || !e.isDescendantOf(pane)) Core.scene.setScrollFocus(null);
		});

		update(() -> {
			if(Core.scene != null && Core.scene.getKeyboardFocus() == this){
				doInput();
			}
		});
		
		shown(() -> {
			saved = true;
			editor.beginEdit(new Map());
			blockgroup.getButtons().get(2).setChecked(true);
			Core.scene.setScrollFocus(view);
			view.clearStack();

			Timers.runTask(10f, Platform.instance::updateRPC);
		});

		hidden(() -> Platform.instance.updateRPC());
	}

	public MapView getView() {
		return view;
	}

	public void resetSaved(){
		saved = false;
	}
	
	public void updateSelectedBlock(){
		Block block = editor.getDrawBlock();
		int i = 0;
		for(BlockPair pair : ColorMapper.getPairs()){
			if(pair.wall == block || (pair.wall == Blocks.air && pair.floor == block)){
				blockgroup.getButtons().get(i).setChecked(true);
				break;
			}
			i++;
		}
	}

	public boolean hasPane(){
		return Core.scene.getScrollFocus() == pane;
	}
	
	public void build(){
		
		new table(){{
			float isize = 16*2f;
			aleft();
			
			new table(){{
				
				defaults().growY().width(130f).padBottom(-6);
				
				new imagebutton("icon-terrain", isize, () ->
					dialog.show()
				).text("$text.editor.generate");
				
				row();
				
				new imagebutton("icon-resize", isize, () ->
					resizeDialog.show()
				).text("$text.editor.resize").padTop(4f);
				
				row();
				
				new imagebutton("icon-load-map", isize, () ->
					loadDialog.show()
				).text("$text.editor.loadmap");
				
				row();
				
				new imagebutton("icon-save-map", isize, ()->
					saveDialog.show()
				).text("$text.editor.savemap");
				
				row();

				//iOS does not support loading raw files.
				if(!ios) {

                    new imagebutton("icon-load-image", isize, () ->
                            openFile.show()
                    ).text("$text.editor.loadimage");

                    row();
                }
				
				new imagebutton("icon-save-image", isize, () -> {
				    //iOS doesn't really support saving raw files. Sharing is used instead.
                    if(!ios){
                        saveFile.show();
                    }else{
                        try{
                            FileHandle file = Gdx.files.local(("map-" + ((editor.getMap().name == null) ? "unknown" : editor.getMap().name) + ".png"));
                            Pixmaps.write(editor.pixmap(), file);
                            Platform.instance.shareFile(file);
                        }catch (Exception e){
                            ui.showError(Bundles.format("text.editor.errorimagesave", Strings.parseException(e, false)));
                        }
                    }
                }).text("$text.editor.saveimage");
				
				row();
				
				new imagebutton("icon-back", isize, () -> {
					if(!saved){
						ui.showConfirm("$text.confirm", "$text.editor.unsaved",
								MapEditorDialog.this::hide);
					}else{
						hide();
					}
				}).padBottom(0).text("$text.back");
				
			}}.left().growY().end();
			
			new table("button"){{
				add(view).grow();
			}}.grow().end();
			
			new table(){{
				Table tools = new Table("button");
				tools.top();
				tools.marginTop(0).marginBottom(6);
				
				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				int i = 1;

				tools.defaults().size(53f, 58f).padBottom(-6);

				ImageButton undo = tools.addImageButton("icon-undo", 16*2f, () -> view.undo()).get();
				ImageButton redo = tools.addImageButton("icon-redo", 16*2f, () -> view.redo()).get();
				ImageButton grid = tools.addImageButton("icon-grid", "toggle", 16*2f, () -> view.setGrid(!view.isGrid())).get();

				undo.setDisabled(() -> !view.getStack().canUndo());
				redo.setDisabled(() -> !view.getStack().canRedo());

				undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.GRAY : Color.WHITE));
				redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.GRAY : Color.WHITE));
				grid.update(() -> grid.setChecked(view.isGrid()));
				
				for(EditorTool tool : EditorTool.values()){
					ImageButton button = new ImageButton("icon-" + tool.name(), "toggle");
					button.clicked(() -> view.setTool(tool));
					button.resizeImage(16*2f);
					button.update(() -> button.setChecked(view.getTool() == tool));
					group.add(button);
					if (tool == EditorTool.pencil)
						button.setChecked(true);
					
					tools.add(button).padBottom(-6f);
					if(i++ % 4 == 1) tools.row();
				}
				
				add(tools).width(53*4).padBottom(-6);
				
				row();
				
				new table("button"){{
					margin(10f);
					Slider slider = new Slider(0, MapEditor.brushSizes.length-1, 1, false);
					slider.moved(f -> editor.setBrushSize(MapEditor.brushSizes[(int)(float)f]));
					new label(() -> Bundles.format("text.editor.brushsize", MapEditor.brushSizes[(int)slider.getValue()])).left();
					row();
					add(slider).growX().padTop(4f);
				}}.growX().padBottom(-6).end();
				
				row();

				new table("button"){{
					get().addCheck("$text.oregen", b -> editor.getMap().oreGen = b)
							.update(c -> c.setChecked(editor.getMap().oreGen)).padTop(3).padBottom(3);
				}}.growX().padBottom(-6).end();

				row();
				
				addBlockSelection(get());
				
				row();
				
			}}.right().growY().end();
		}}.grow().end();
	}

	public void tryLoadMap(FileHandle file){
        ui.loadfrag.show();
        Timers.runTask(3f, () -> {
            try{
                Pixmap pixmap = new Pixmap(file);
                if(verifySize(pixmap)){
                    editor.setPixmap(pixmap);
                    view.clearStack();
                }else{
                    ui.showError(Bundles.format("text.editor.badsize", Arrays.toString(MapEditor.validMapSizes)));
                }
            }catch (Exception e){
                ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
                Log.err(e);
            }
            ui.loadfrag.hide();
        });
    }

	private void doInput(){
		//tool select
		for(int i = 0; i < EditorTool.values().length; i ++){
			int code = i == 0 ? 5 : i;
			if(Inputs.keyTap("weapon_" + code)){
				view.setTool(EditorTool.values()[i]);
				break;
			}
		}

		//ctrl keys (undo, redo, save)
		if(Inputs.keyDown(Input.CONTROL_LEFT)){
			if(Inputs.keyTap(Input.Z)){
				view.undo();
			}

			if(Inputs.keyTap(Input.Y)){
				view.redo();
			}

			if(Inputs.keyTap(Input.S)){
				saveDialog.save();
			}

			if(Inputs.keyTap(Input.G)){
				view.setGrid(!view.isGrid());
			}
		}
	}
	
	private boolean verifySize(Pixmap pix){
		boolean w = false, h = false;
		for(int i : MapEditor.validMapSizes){
			if(pix.getWidth() == i)
				w = true;
			if(pix.getHeight() == i)
				h = true;
		}
		
		return w && h;
	}
	
	private boolean verifyMap(){
		int psc = ColorMapper.getColor(SpecialBlocks.playerSpawn);
		int esc = ColorMapper.getColor(SpecialBlocks.enemySpawn);
		
		int playerSpawns = 0;
		int enemySpawns = 0;
		Pixmap pix = editor.pixmap();
		
		for(int x = 0; x < pix.getWidth(); x ++){
			for(int y = 0; y < pix.getHeight(); y ++){
				int i = pix.getPixel(x, y);
				if(i == psc) playerSpawns ++;
				if(i == esc) enemySpawns ++;
			}
		}
		
		if(playerSpawns == 0){
			ui.showError("$text.editor.noplayerspawn");
			return false;
		}else if(playerSpawns > 1){
			ui.showError("$text.editor.manyplayerspawns");
			return false;
		}
		
		if(enemySpawns > MapEditor.maxSpawnpoints){
			ui.showError(Bundles.format("text.editor.manyenemyspawns", MapEditor.maxSpawnpoints));
			return false;
		}
		
		return true;
	}
	
	private void addBlockSelection(Table table){
		Table content = new Table();
		pane = new ScrollPane(content, "volume");
		pane.setScrollingDisabled(true, false);
		pane.setFadeScrollBars(false);
		pane.setOverscroll(true, false);
		ButtonGroup<ImageButton> group = new ButtonGroup<>();
		blockgroup = group;
		
		int i = 0;
		
		for(BlockPair pair : ColorMapper.getPairs()){
			Block block = pair.wall == Blocks.air ? pair.floor : pair.wall;
			
			ImageButton button = new ImageButton(Draw.hasRegion(block.name) ? Draw.region(block.name) : Draw.region(block.name + "1"), "toggle");
			button.clicked(() -> editor.setDrawBlock(block));
			button.resizeImage(8*4f);
			group.add(button);
			content.add(button).pad(4f).size(53f, 58f);
			
			if(i++ % 2 == 1){
				content.row();
			}
		}
		
		group.getButtons().get(2).setChecked(true);
		
		Table extra = new Table("button");
		extra.labelWrap(() -> editor.getDrawBlock().formalName).width(180f).center();
		table.add(extra).padBottom(-6).growX();
		table.row();
		table.add(pane).growY().fillX();
	}
}
