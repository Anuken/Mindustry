package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import static io.anuke.mindustry.Vars.*;
import io.anuke.mindustry.mapeditor.MapFilter.GenPref;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.CheckBox;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;

public class MapGenerateDialog extends FloatingDialog{
	private MapEditor editor;
	private Image image;
	private boolean loading;
	
	public MapGenerateDialog(MapEditor editor) {
		super("$text.editor.generate");
		this.editor = editor;
		
		Stack stack = new Stack();
		stack.add(image = new BorderImage());
		
		Image loadImage = new Image("icon-loading");
		loadImage.setScaling(Scaling.none);
		loadImage.setScale(3f);
		loadImage.update(() -> loadImage.setOrigin(Align.center));
		loadImage.setVisible(() -> loading);
		Image next = new Image("white");
		next.setScaling(Scaling.fit);
		next.setColor(0, 0, 0, 0.6f);
		next.setVisible(() -> loading);
		
		stack.add(next);
		stack.add(loadImage);
		
		content().add(stack).grow();
		image.setScaling(Scaling.fit);
		Table preft = new Table();
		preft.left();
		preft.margin(4f).marginRight(25f);
		
		for(GenPref pref : editor.getFilter().getPrefs().values()){
			CheckBox box = new CheckBox(pref.name);
			box.setChecked(pref.enabled);
			box.changed(() -> pref.enabled = box.isChecked());
			preft.add(box).pad(4f).left();
			preft.row();
		}
		
		ScrollPane pane = new ScrollPane(preft, "volume");
		pane.setFadeScrollBars(false);
		pane.setScrollingDisabled(true, false);
		
		content().add(pane).fillY();
		
		buttons().defaults().size(170f, 50f).pad(4f);
		buttons().addButton("$text.back", this::hide);
		buttons().addButton("$text.randomize", () ->{
			editor.getFilter().randomize();
			apply();
		});
		buttons().addButton("$text.update", this::apply);
		buttons().addButton("$text.apply", () ->{
			ui.loadfrag.show();
			
			Timers.run(3f, () ->{
				Pixmap copy = Pixmaps.copy(editor.pixmap());
				editor.applyFilter();
				ui.editor.getView().push(copy, Pixmaps.copy(editor.pixmap()));
				ui.loadfrag.hide();
				ui.editor.resetSaved();
				hide();
			});
		});
		
		shown(() ->{
			loading = true;
			Timers.run(30f, () -> {
				editor.applyFilterPreview();
				image.setDrawable(new TextureRegionDrawable(new TextureRegion(editor.getFilterTexture())));
				loading = false;
			});
		});
	}
	
	private void apply(){
		loading = true;
		Timers.run(3f, () -> {
			editor.applyFilterPreview();
			loading = false;
		});
		
	}

}
