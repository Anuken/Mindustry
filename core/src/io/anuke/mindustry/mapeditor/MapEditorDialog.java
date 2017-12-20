package io.anuke.mindustry.mapeditor;

import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ui.FileChooser;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.SpecialBlocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MapEditorDialog extends Dialog{
	private MapEditor editor;
	private MapView view;
	private MapGenerateDialog dialog;
	private MapLoadDialog loadDialog;
	private MapSaveDialog saveDialog;
	private MapResizeDialog resizeDialog;
	private FileChooser openFile, saveFile;
	private boolean saved = false;
	
	private ButtonGroup<ImageButton> blockgroup;
	
	public MapEditorDialog(MapEditor editor){
		super("Map Editor", "dialog");
		this.editor = editor;
		dialog = new MapGenerateDialog(editor);
		view = new MapView(editor);
		
		openFile = new FileChooser("Load Image", FileChooser.pngFilter, true, file -> {
			Vars.ui.showLoading();
			Timers.run(3f, () -> {
				try{
					Pixmap pixmap = new Pixmap(file);
					if(verifySize(pixmap)){
						editor.setPixmap(pixmap);
					}else{
						Vars.ui.showError("[orange]Invalid image dimensions![]\nValid map dimensions: " + Arrays.toString(MapEditor.validMapSizes));
					}
				}catch (Exception e){
					Vars.ui.showError("Error loading image file!");
					e.printStackTrace();
				}
				Vars.ui.hideLoading();
			});
		});
		
		saveFile = new FileChooser("Save Image", false, file -> {
			if(!file.extension().toLowerCase().equals(".png")){
				file = file.parent().child(file.nameWithoutExtension() + ".png");
			}
			FileHandle result = file;
			Vars.ui.showLoading();
			Timers.run(3f, () -> {
				try{
					Gdx.app.error("MINDUSTRYAAAAAAAAAA", "Saving to file: " + result.toString() + " " + result.type());
					Pixmaps.write(editor.pixmap(), result);
				}catch (Exception e){
					Vars.ui.showError("Error saving image file!");
					e.printStackTrace();
				}
				Vars.ui.hideLoading();
			});
		});
		
		loadDialog = new MapLoadDialog(map -> {
			saveDialog.setFieldText(map.name);
			Vars.ui.showLoading();
			
			Timers.run(3f, () -> {
				Map copy = new Map();
				copy.name = map.name;
				copy.id = -1;
				copy.pixmap = Pixmaps.copy(map.pixmap);
				copy.texture = new Texture(copy.pixmap);
				editor.beginEdit(copy);
				Vars.ui.hideLoading();
			});
		});
		
		resizeDialog = new MapResizeDialog(editor, (x, y) -> {
			Pixmap pix = editor.pixmap();
			if(!(pix.getWidth() == x && pix.getHeight() == y)){
				Vars.ui.showLoading();
				Timers.run(10f, ()->{
					editor.resize(x, y);
					Vars.ui.hideLoading();
				});
			}
		});
		
		saveDialog = new MapSaveDialog(name -> {
			Vars.ui.showLoading();
			if(verifyMap()){
				saved = true;
				editor.getMap().name = name;
				Timers.run(10f, () -> {
					Vars.world.maps().saveAndReload(editor.getMap(), editor.pixmap());
					Vars.ui.hideLoading();
				});
			}else{
				Vars.ui.hideLoading();
			}
		});
		
		setFillParent(true);
		
		clearChildren();
		pad(0);
		build.begin(this);
		build();
		build.end();
		
		shown(() -> {
			saved = true;
			editor.beginEdit(new Map());
			Core.scene.setScrollFocus(view);
		});
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
	
	public void build(){
		
		new table(){{
			float isize = Unit.dp.inPixels(16*2f);
			aleft();
			
			new table(){{
				
				defaults().growY().width(130f).units(Unit.dp).padBottom(-6);
				
				new imagebutton("icon-terrain", isize, () -> {
					dialog.show();
				}).text("generate").units(Unit.dp);
				
				row();
				
				new imagebutton("icon-resize", isize, () -> {
					resizeDialog.show();
				}).text("resize").padTop(4f);
				
				row();
				
				new imagebutton("icon-load-map", isize, () -> {
					loadDialog.show();
				}).text("load map");
				
				row();
				
				new imagebutton("icon-save-map", isize, ()->{
					saveDialog.show();
				}).text("save map");
				
				row();
				
				new imagebutton("icon-load-image", isize, () -> {
					openFile.show();
				}).text("load image");
				
				row();
				
				new imagebutton("icon-save-image", isize, () -> {
					saveFile.show();
				}).text("save image");
				
				row();
				
				new imagebutton("icon-back", isize, () -> {
					if(!saved){
						Vars.ui.showConfirm("Confirm Exit", "[scarlet]You have unsaved changes![]\nAre you sure you want to exit?", () -> hide());
					}else{
						hide();
					}
				}).padBottom(0).text("back");
				
			}}.left().growY().end();
			
			new table("button"){{
				add(view).grow();
			}}.grow().end();
			
			new table(){{
				Table tools = new Table("button");
				tools.top();
				tools.padTop(0).padBottom(6);
				
				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				int i = 0;
				
				for(EditorTool tool : EditorTool.values()){
					ImageButton button = new ImageButton("icon-" + tool.name(), "toggle");
					button.clicked(() -> view.setTool(tool));
					button.resizeImage(Unit.dp.inPixels(16*2f));
					group.add(button);
					
					tools.add(button).size(80f, 85f).padBottom(-6f).units(Unit.dp);
					if(i++ % 2 == 1) tools.row();
				}
				
				add(tools).units(Unit.dp).width(160f).padBottom(-6);
				
				row();
				
				new table("button"){{
					margin(10f);
					Slider slider = new Slider(0, MapEditor.brushSizes.length-1, 1, false);
					slider.moved(f -> {
						editor.setBrushSize(MapEditor.brushSizes[(int)(float)f]);
					});
					new label(() -> "Brush size: " + MapEditor.brushSizes[(int)slider.getValue()]).left();
					row();
					add(slider).growX().padTop(4f).units(Unit.dp);
				}}.growX().end();
				
				row();
				
				addBlockSelection(get());
				
				row();
				
			}}.right().growY().end();
		}}.grow().end();
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
			Vars.ui.showError("This map has no player spawnpoint!");
			return false;
		}else if(playerSpawns > 1){
			Vars.ui.showError("Maps cannot have more than one\nplayer spawnpoint!");
			return false;
		}
		
		if(enemySpawns > MapEditor.maxSpawnpoints){
			Vars.ui.showError("Cannot have more than\n" + MapEditor.maxSpawnpoints + " enemy spawnpoints!");
			return false;
		}
		
		return true;
	}
	
	private void addBlockSelection(Table table){
		Table content = new Table();
		ScrollPane pane = new ScrollPane(content, "volume");
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
			button.resizeImage(Unit.dp.inPixels(8*4f));
			group.add(button);
			content.add(button).pad(4f).size(53f, 58f).units(Unit.dp);
			
			if(i++ % 2 == 1){
				content.row();
			}
		}
		
		content.padLeft(Unit.dp.inPixels(-5f));
		
		group.getButtons().get(2).setChecked(true);
		
		Table extra = new Table("button");
		extra.labelWrap(() -> editor.getDrawBlock().name).width(120f).center();
		table.add(extra).growX();
		table.row();
		table.add(pane).growY().fillX();
	}
}
