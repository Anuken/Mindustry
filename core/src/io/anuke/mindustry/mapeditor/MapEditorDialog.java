package io.anuke.mindustry.mapeditor;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class MapEditorDialog extends Dialog{
	private MapEditor editor;
	private MapView view;
	
	public MapEditorDialog(MapEditor editor){
		super("Map Editor", "dialog");
		this.editor = editor;
		view = new MapView(editor);
		
		setFillParent(true);
		
		clearChildren();
		pad(0);
		build.begin(this);
		build();
		build.end();
		
		shown(() -> {
			editor.beginEdit(new Map());
		});
	}
	
	public void build(){
		
		new table(){{
			float isize = Unit.dp.inPixels(14*3f);
			aleft();
			
			new table(){{
				defaults().growY().width(130f).units(Unit.dp).padBottom(-6);
				
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
					
				}).padBottom(0).text("back");
				
			}}.left().growY().end();
			
			new table("button"){{
				add(view).grow();
			}}.grow().end();
			
			new table(){{
				new imagebutton("icon-terrain", isize, () -> {
					
				}).margin(12f).text("generate...").width(148f).units(Unit.dp);
				
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
