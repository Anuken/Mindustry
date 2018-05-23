package io.anuke.mindustry.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.ui.dialogs.FileChooser;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.input.Input;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.builders.build;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.utils.UIUtils;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class MapEditorDialog extends Dialog implements Disposable{
	private MapEditor editor;
	private MapView view;
	private MapInfoDialog infoDialog;
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
		view = new MapView(editor);

		infoDialog = new MapInfoDialog(editor);

		saveFile = new FileChooser("$text.saveimage", false, file -> {
			file = file.parent().child(file.nameWithoutExtension() + "." + mapExtension);
			FileHandle result = file;
			ui.loadAnd(() -> {

				try{
					if(!editor.getTags().containsKey("name")){
						tags.put("name", result.nameWithoutExtension());
					}
					MapIO.writeMap(result.write(false), editor.getTags(), editor.getMap());
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimagesave", Strings.parseException(e, false)));
					Log.err(e);
				}
			});
		});

		openFile = new FileChooser("$text.loadimage", FileChooser.mapFilter, true, file -> {
			ui.loadAnd(() -> {
				try{
					DataInputStream stream = new DataInputStream(file.read());

					MapMeta meta = MapIO.readMapMeta(stream);
					MapTileData data = MapIO.readTileData(stream, meta, false);

					editor.beginEdit(data, meta.tags);
					view.clearStack();
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
					Log.err(e);
				}
			});
		});

		saveImage = new FileChooser("$text.saveimage", false, file -> {
			file = file.parent().child(file.nameWithoutExtension() + ".png");
			FileHandle result = file;
			ui.loadAnd(() -> {
				try{
					Pixmaps.write(MapIO.generatePixmap(editor.getMap()), result);
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimagesave", Strings.parseException(e, false)));
					Log.err(e);
				}
			});
		});

		openImage = new FileChooser("$text.loadimage", FileChooser.pngFilter, true, file -> {
			ui.loadAnd(() -> {
				try{
					MapTileData data = MapIO.readPixmap(new Pixmap(file));

					editor.beginEdit(data, new ObjectMap<>());
					view.clearStack();
				}catch (Exception e){
					ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
					Log.err(e);
				}
			});
		});

		menu = new FloatingDialog("$text.menu");
		menu.addCloseButton();

		float isize = 16*2f;
		float swidth = 180f;

		menu.content().table(t -> {
			t.defaults().size(swidth, 60f).padBottom(5).padRight(5).padLeft(5);

			t.addImageTextButton("$text.editor.savemap", "icon-floppy-16", isize, () -> {
				String name = editor.getTags().get("name", "");

				if(name.isEmpty()){
					ui.showError("$text.editor.save.noname");
				}else{
					Map map = world.maps().getByName(name);
					if(map != null && !map.custom){
						ui.showError("$text.editor.save.overwrite");
					}else{
						world.maps().saveAndReload(name, editor.getMap(), editor.getTags());
						ui.showInfoFade("$text.editor.saved");
					}
				}

				menu.hide();
			}).size(swidth*2f + 10, 60f).colspan(2);

			t.row();

			t.addImageTextButton("$text.editor.mapinfo", "icon-pencil", isize, () -> {
				infoDialog.show();
				menu.hide();
			});

			t.addImageTextButton("$text.editor.resize", "icon-resize", isize, () -> {
				resizeDialog.show();
				menu.hide();
			});

			t.row();

			t.addImageTextButton("$text.editor.import", "icon-load-map", isize, () ->
				createDialog("$text.editor.import",
						"$text.editor.importmap", "$text.editor.importmap.description", "icon-load-map", (Listenable)loadDialog::show,
						"$text.editor.importfile", "$text.editor.importfile.description", "icon-file", (Listenable)openFile::show,
						"$text.editor.importimage", "$text.editor.importimage.description", "icon-file-image", (Listenable)openImage::show));

			t.addImageTextButton("$text.editor.export", "icon-save-map", isize, () -> createDialog("$text.editor.export",
					"$text.editor.exportfile", "$text.editor.exportfile.description", "icon-file", (Listenable)saveFile::show,
					"$text.editor.exportimage", "$text.editor.exportimage.description", "icon-file-image", (Listenable)saveImage::show));

			t.row();

			t.row();
		});

		menu.content().row();

		menu.content().addImageTextButton("$text.quit", "icon-back", isize, () -> {
			if(!saved){
				ui.showConfirm("$text.confirm", "$text.editor.unsaved", this::hide);
			}else{
				hide();
			}
			menu.hide();
		}).padTop(-5).size(swidth*2f + 10, 60f);
		
		resizeDialog = new MapResizeDialog(editor, (x, y) -> {
			if(!(editor.getMap().width() == x && editor.getMap().height() == y)){
				ui.loadAnd(() -> {
					editor.resize(x, y);
					view.clearStack();
				});
			}
		});

		loadDialog = new MapLoadDialog(map -> {
			saveDialog.setFieldText(map.name);

			ui.loadAnd(() -> {
				try (DataInputStream stream = new DataInputStream(map.stream.get())){
					MapMeta meta = MapIO.readMapMeta(stream);
					MapTileData data = MapIO.readTileData(stream, meta, false);

					editor.beginEdit(data, meta.tags);
					view.clearStack();
				}catch (IOException e){
					ui.showError(Bundles.format("text.editor.errormapload", Strings.parseException(e, false)));
					Log.err(e);
				}
			});
		});


		saveDialog = new MapSaveDialog(name -> {
			ui.loadAnd(() -> {
				saved = true;
				world.maps().saveAndReload(editor.getTags().get("name", name), editor.getMap(), editor.getTags());
				loadDialog.rebuild();
			});
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
			editor.beginEdit(new MapTileData(256, 256), new ObjectMap<>());
			blockgroup.getButtons().get(2).setChecked(true);
			Core.scene.setScrollFocus(view);
			view.clearStack();

			Timers.runTask(10f, Platform.instance::updateRPC);
		});

		hidden(() -> Platform.instance.updateRPC());
	}

	/**Argument format:
	 * 0) button name
	 * 1) description
	 * 2) icon name
	 * 3) listener
	 */
	private FloatingDialog createDialog(String title, Object... arguments){
		FloatingDialog dialog = new FloatingDialog(title);

		float h = 90f;

		dialog.content().defaults().size(360f, h).padBottom(5).padRight(5).padLeft(5);

		for(int i = 0; i < arguments.length; i += 4){
			String name = (String)arguments[i];
			String description = (String)arguments[i + 1];
			String iconname = (String)arguments[i + 2];
			Listenable listenable = (Listenable)arguments[i + 3];

			TextButton button = dialog.content().addButton(name, () -> {
				listenable.listen();
				dialog.hide();
				menu.hide();
			}).left().get();

			button.clearChildren();
			button.table("button", t -> {
				t.addImage(iconname).size(16*3);
				t.update(() -> t.background(button.getClickListener().isOver() ? "button-over" : "button"));
			}).padLeft(-10).padBottom(-3).size(h);
			button.table(t -> {
				t.add(name).growX().wrap();
				t.row();
				t.add(description).color(Color.GRAY).growX().wrap();
			}).growX().padLeft(8);

			button.row();

			dialog.content().row();
		}

		dialog.addCloseButton();
		dialog.show();

		return dialog;
	}

	@Override
	public Dialog show(){
		return super.show(Core.scene, Actions.sequence(Actions.alpha(0f), Actions.scaleTo(1f, 1f),  Actions.fadeIn(0.3f)));
	}

	@Override
	public void dispose(){
		editor.renderer().dispose();
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
				atop();

				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				int i = 1;

				tools.defaults().size(60f, 64f).padBottom(-5.1f);

				//tools.addImageButton("icon-back", 16*2, () -> hide());

				tools.addImageButton("icon-menu-large", 16*2f, menu::show);

				ImageButton grid = tools.addImageButton("icon-grid", "toggle", 16*2f, () -> view.setGrid(!view.isGrid())).get();

				tools.row();

				ImageButton undo = tools.addImageButton("icon-undo", 16*2f, () -> view.undo()).get();
				ImageButton redo = tools.addImageButton("icon-redo", 16*2f, () -> view.redo()).get();

				tools.row();

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
					if(i++ % 2 == 0) tools.row();
				}

				ImageButton rotate = tools.addImageButton("icon-arrow-16", 16*2f, () -> editor.setDrawRotation((editor.getDrawRotation() + 1)%4)).get();
				rotate.getImage().update(() ->{
					rotate.getImage().setRotation(editor.getDrawRotation() * 90);
					rotate.getImage().setOrigin(Align.center);
				});

				tools.row();

				tools.table("button", t -> {
					t.add("$text.editor.teams");
				}).colspan(2).height(40).width(120f);

				tools.row();

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

				add(tools).top().padBottom(-6);

				row();

				new table("button"){{
					Slider slider = new Slider(0, MapEditor.brushSizes.length-1, 1, false);
					slider.moved(f -> editor.setBrushSize(MapEditor.brushSizes[(int)(float)f]));
					new label("brush");
					//new label(() -> Bundles.format("text.editor.brushsize", MapEditor.brushSizes[(int)slider.getValue()])).left();
					row();
					add(slider).width(100f).padTop(4f);
				}}.padBottom(10).padTop(5).top().end();
				
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
			if(Inputs.keyTap(Input.valueOf("NUM_" + (i+1)))){
				view.setTool(EditorTool.values()[i]);
				break;
			}
		}

		if(Inputs.keyTap(Input.R)){
			editor.setDrawRotation((editor.getDrawRotation() + 1)%4);
		}

		if(Inputs.keyTap(Input.E)){
			editor.setDrawRotation(Mathf.mod((editor.getDrawRotation() + 1), 4));
		}

		//ctrl keys (undo, redo, save)
		if(UIUtils.ctrl()){
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
			TextureRegion[] regions = block.getCompactIcon();

			if(regions.length == 0) continue;

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
			button.update(() -> button.setChecked(editor.getDrawBlock() == block));
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
