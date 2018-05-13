package io.anuke.mindustry.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.ui.dialogs.FileChooser;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.input.Input;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;

import static io.anuke.mindustry.Vars.*;

public class MapEditorDialog extends Dialog{
	private MapEditor editor;
	private MapView view;
	private MapGenerateDialog dialog;
	private MapLoadDialog loadDialog;
	private MapSaveDialog saveDialog;
	private MapResizeDialog resizeDialog;
	private ScrollPane pane;
	private FloatingDialog menu;
	private FileChooser openFile, saveFile, openImage, saveImage;
	private ObjectMap<String, String> tags = new ObjectMap<>();
	private boolean saved = false;
	
	private ButtonGroup<ImageButton> blockgroup;
	
	public MapEditorDialog(){
		super("$text.mapeditor", "dialog");
		if(gwt) return;

		editor = new MapEditor();
		dialog = new MapGenerateDialog(editor);
		view = new MapView(editor);

		saveFile = new FileChooser("$text.saveimage", false, file -> {
			file = file.parent().child(file.nameWithoutExtension() + "." + mapExtension);
			FileHandle result = file;
			ui.loadfrag.show();
			Timers.run(3f, () -> {

				try{
					tags.put("name", result.nameWithoutExtension());
					MapIO.writeMap(result, tags, editor.getMap());
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimagesave", Strings.parseException(e, false)));
					Log.err(e);
				}
				ui.loadfrag.hide();
			});
		});

		openFile = new FileChooser("$text.loadimage", FileChooser.mapFilter, true, file -> {
			ui.loadfrag.show();
			Timers.run(3f, () -> {
				try{
					MapTileData data = MapIO.readTileData(new DataInputStream(file.read()), false);

					editor.beginEdit(data);
					view.clearStack();
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
					Log.err(e);
				}
				ui.loadfrag.hide();
			});
		});

		saveImage = new FileChooser("$text.saveimage", false, file -> {
			file = file.parent().child(file.nameWithoutExtension() + ".png");
			FileHandle result = file;
			ui.loadfrag.show();
			Timers.run(3f, () -> {
				try{
					Pixmaps.write(MapIO.generatePixmap(editor.getMap()), result);
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimagesave", Strings.parseException(e, false)));
					Log.err(e);
				}
				ui.loadfrag.hide();
			});
		});

		openImage = new FileChooser("$text.loadimage", FileChooser.pngFilter, true, file -> {
			ui.loadfrag.show();
			Timers.run(3f, () -> {
				try{
					MapTileData data = MapIO.readPixmap(new Pixmap(file));

					editor.beginEdit(data);
					view.clearStack();
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
					Log.err(e);
				}
				ui.loadfrag.hide();
			});
		});

		menu = new FloatingDialog("$text.menu");
		menu.addCloseButton();

		menu.content().defaults().size(280f, 60f).padBottom(5);

		float isize = 16*2f;

		menu.content().addImageTextButton("$text.quit", "icon-back", isize, () -> {
			if(!saved){
				ui.showConfirm("$text.confirm", "$text.editor.unsaved", this::hide);
			}else{
				hide();
			}
			menu.hide();
		});

		menu.content().row();

		menu.content().addImageTextButton("$text.editor.savemap", "icon-save-map", isize, () -> {
			saveFile.show();
			menu.hide();
		});

		menu.content().row();

		menu.content().addImageTextButton("$text.editor.loadmap", "icon-load-map", isize, () -> {
			openFile.show();
			menu.hide();
		});

		menu.content().row();

		menu.content().addImageTextButton("$text.editor.saveimage", "icon-save-map", isize, () -> {
			saveImage.show();
			menu.hide();
		});

		menu.content().row();

		menu.content().addImageTextButton("$text.editor.loadimage", "icon-load-map", isize, () -> {
			openImage.show();
			menu.hide();
		});


		/*
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
		});*/
		
		resizeDialog = new MapResizeDialog(editor, (x, y) -> {
			if(!(editor.getMap().width() == x && editor.getMap().height() == y)){
				ui.loadfrag.show();
				Timers.run(10f, () -> {
					editor.resize(x, y);
					view.clearStack();
					ui.loadfrag.hide();
				});
			}
		});

		/*
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
		});*/
		
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
			editor.beginEdit(new MapTileData(256, 256));
			blockgroup.getButtons().get(2).setChecked(true);
			Core.scene.setScrollFocus(view);
			view.clearStack();

			Timers.runTask(10f, Platform.instance::updateRPC);
		});

		hidden(() -> Platform.instance.updateRPC());
	}


	@Override
	public Dialog show(){
		return super.show(Core.scene, Actions.sequence(Actions.alpha(0f), Actions.scaleTo(1f, 1f),  Actions.fadeIn(0.3f)));
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
			aleft();

			new table("button"){{
				margin(0);
				Table tools = new Table();
				tools.top();

				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				int i = 1;

				tools.defaults().size(60f, 64f).padBottom(-5.1f);

				tools.addImageButton("icon-back", 16*2, () -> hide());

				tools.addImageButton("icon-menu-large", 16*2f, menu::show);

				tools.row();

				ImageButton undo = tools.addImageButton("icon-undo", 16*2f, () -> view.undo()).get();
				ImageButton redo = tools.addImageButton("icon-redo", 16*2f, () -> view.redo()).get();

				tools.row();

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

					tools.add(button).padBottom(-5.1f);
					if(i++ % 2 == 1) tools.row();
				}

				ButtonGroup<ImageButton> teamgroup = new ButtonGroup<>();

				for(Team team : Team.values()){
					ImageButton button = new ImageButton("white", "toggle");
					button.margin(4f, 4f, 10f, 4f);
					button.getImageCell().grow();
					button.getStyle().imageUpColor = team.color;
					button.clicked(() -> editor.setDrawTeam(team));
					button.update(() -> button.setChecked(editor.getDrawTeam() == team));
					teamgroup.add(button);
					tools.add(button).padBottom(-5.1f);

					if(i++ % 2 == 1) tools.row();
				}

				add(tools).growY().top().padBottom(-6);

				row();

				new table(){{
					Slider slider = new Slider(0, MapEditor.brushSizes.length-1, 1, true);
					slider.moved(f -> editor.setBrushSize(MapEditor.brushSizes[(int)(float)f]));
					//new label(() -> Bundles.format("text.editor.brushsize", MapEditor.brushSizes[(int)slider.getValue()])).left();
					row();
					add(slider).growX().padTop(4f);
				}}.growX().padBottom(10).end();
				
			}}.left().growY().end();


			new table("button"){{
				margin(5);
				marginBottom(10);
				add(view).grow();
			}}.grow().end();

			new table(){{

				row();
				
				addBlockSelection(get());
				
				row();
				
			}}.right().growY().end();
		}}.grow().end();
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

	private void addBlockSelection(Table table){
		Table content = new Table();
		pane = new ScrollPane(content, "volume");
		pane.setFadeScrollBars(false);
		pane.setOverscroll(true, false);
		ButtonGroup<ImageButton> group = new ButtonGroup<>();
		blockgroup = group;
		
		int i = 0;
		
		for(Block block : Block.getAllBlocks()){
			TextureRegion[] regions;
			try {
				regions = block.getCompactIcon();
			}catch (Exception e){
				continue;
			}

			Stack stack = new Stack();

			for(TextureRegion region : regions){
				stack.add(new Image(region));
			}
			
			ImageButton button = new ImageButton("white", "toggle");
			button.clicked(() -> editor.setDrawBlock(block));
			button.resizeImage(8*4f);
			button.getImageCell().setActor(stack);
			button.addChild(stack);
			button.getImage().remove();
			group.add(button);
			content.add(button).pad(4f).size(53f, 58f);
			
			if(i++ % 3 == 2){
				content.row();
			}
		}
		
		group.getButtons().get(2).setChecked(true);
		
		Table extra = new Table("button");
		extra.labelWrap(() -> editor.getDrawBlock().formalName).width(220f).center();
		table.add(extra).growX();
		table.row();
		table.add(pane).growY().fillX();
	}
}
