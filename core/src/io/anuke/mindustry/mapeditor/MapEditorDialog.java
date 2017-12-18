package io.anuke.mindustry.mapeditor;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MapEditorDialog extends Dialog{
	private MapEditor editor;
	private MapView view;
	private MapGenerateDialog dialog;
	private ButtonGroup<ImageButton> blockgroup;
	
	public MapEditorDialog(MapEditor editor){
		super("Map Editor", "dialog");
		this.editor = editor;
		dialog = new MapGenerateDialog(editor);
		view = new MapView(editor);
		
		setFillParent(true);
		
		clearChildren();
		pad(0);
		build.begin(this);
		build();
		build.end();
		
		shown(() -> {
			editor.beginEdit(new Map());
			Core.scene.setScrollFocus(view);
		});
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
			float isize = Unit.dp.inPixels(14*3f);
			aleft();
			
			new table(){{
				
				defaults().growY().width(130f).units(Unit.dp).padBottom(-6);
				
				new imagebutton("icon-terrain", isize, () -> {
					dialog.show();
				}).text("generate").units(Unit.dp);
				
				row();
				
				new imagebutton("icon-load", isize, () -> {
					
				}).text("load map");
				
				row();
				
				new imagebutton("icon-save", isize, ()->{
					
				}).text("save map");
				
				row();
				
				new imagebutton("icon-load", isize, () -> {
					
				}).text("load image");
				
				row();
				
				new imagebutton("icon-save", isize, () -> {
					
				}).text("save image");
				
				row();
				
				new imagebutton("icon-cursor", 10f*3f, () -> {
					
				}).text("resize").padTop(4f);
				
				row();
				
				new imagebutton("icon-arrow-left", isize, () -> {
					hide();
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
	
	private void addBlockSelection(Table table){
		Table content = new Table();
		ScrollPane pane = new ScrollPane(content, "volume");
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
		
		Table extra = new Table("button");
		extra.addWrap(() -> editor.getDrawBlock().name).width(120f).center();
		table.add(extra).growX();
		table.row();
		table.add(pane).growY().fillX();
	}
}
