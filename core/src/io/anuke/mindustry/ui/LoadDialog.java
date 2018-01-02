package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.Saves.SaveSlot;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.builders.button;
import io.anuke.ucore.scene.builders.dialog;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

public class LoadDialog extends FloatingDialog{
	ScrollPane pane;
	Table slots;

	public LoadDialog() {
		this("$text.loadgame");
	}

	public LoadDialog(String title) {
		super(title);
		setup();

		shown(() -> {
			setup();
			Timers.runTask(2f, () -> Core.scene.setScrollFocus(pane));
		});

		addCloseButton();
	}

	protected void setup(){
		content().clear();

		slots = new Table();
		pane = new ScrollPane(slots, "clear-black");
		pane.setFadeScrollBars(false);
		pane.setScrollingDisabled(true, false);

		slots.marginRight(24);

		Timers.runTask(2f, () -> Core.scene.setScrollFocus(pane));

		Array<SaveSlot> array = Vars.control.getSaves().getSaveSlots();

		for(SaveSlot slot : array){

			TextButton button = new TextButton("[accent]" + slot.getName(), "clear");
			button.getLabelCell().growX().left();
			button.getLabelCell().padBottom(8f);
			button.getLabelCell().top().left().growX();

			button.defaults().left();

			button.table(t -> {
				t.right();

				t.addIButton("icon-floppy", "emptytoggle", 14*3, () -> {
					slot.setAutosave(!slot.isAutosave());
				}).checked(slot.isAutosave()).right();

				t.addIButton("icon-trash", "empty", 14*3, () -> {
					Vars.ui.showConfirm("$text.confirm", "$text.save.delete.confirm", () -> {
						slot.delete();
						setup();
					});
				}).size(14*3).right();

				t.addIButton("icon-dots", "empty", 14*3, () -> {
					FloatingDialog dialog = new FloatingDialog("Save Options");
					dialog.addCloseButton();

					dialog.content().defaults().left().uniformX().size(230f, 60f);

					dialog.content().addImageTextButton("$text.save.rename", "icon-rename", 14*3, () -> {
						Vars.ui.showTextInput("$text.save.rename", "$text.save.rename.text", slot.getName(), text -> {
							slot.setName(text);
							dialog.hide();
							setup();
						});
					});

					dialog.content().row();

					dialog.content().addImageTextButton("$text.save.import", "icon-save", 14*3, () -> {
						new FileChooser("$text.save.import", f -> f.extension().equals("mins"), true, file -> {
							if(SaveIO.isSaveValid(file)){
								try{
									slot.importFile(file);
									setup();
								}catch (IOException e){
									Vars.ui.showError(Bundles.format("text.save.import.fail", Strings.parseException(e, false)));
								}
							}else{
								Vars.ui.showError("$text.save.import.invalid");
							}
							dialog.hide();
						}).show();
					});

					dialog.content().row();

					dialog.content().addImageTextButton("$text.save.export", "icon-load", 14*3, () -> {
						new FileChooser("$text.save.export", false, file -> {
							try{
								slot.exportFile(file);
								setup();
							}catch (IOException e){
								Vars.ui.showError(Bundles.format("text.save.export.fail", Strings.parseException(e, false)));
							}
							dialog.hide();
						}).show();
					});

					dialog.show();
				}).size(14*3).right();

			}).padRight(-10).growX();

			String color = "[lightgray]";

			button.defaults().padBottom(3);
			button.row();
			button.add(Bundles.format("text.save.map", color+slot.getMap().localized()));
			button.row();
			button.add(Bundles.get("text.level.mode") + " " +color+ slot.getMode());
			button.row();
			button.add(Bundles.format("text.save.wave", color+slot.getWave()));
			button.row();
			button.label(() -> Bundles.format("text.save.autosave", color + Bundles.get(slot.isAutosave() ? "text.on" : "text.off")));
			button.row();
			button.add();
			button.add(Bundles.format("text.save.date", color+slot.getDate()));
			button.row();
			modifyButton(button, slot);

			slots.add(button).uniformX().fillX().pad(4).padRight(-4).margin(10f).marginLeft(20f).marginRight(20f);
			slots.row();
		}

		content().add(pane);

		addSetup();
	}

	public void addSetup(){
		if(Vars.control.getSaves().getSaveSlots().size != 0) return;

		slots.row();
		slots.addButton("$text.save.none", "clear", ()->{})
				.disabled(true).fillX().margin(20f).minWidth(340f).height(80f).pad(4f);
	}

	public void modifyButton(TextButton button, SaveSlot slot){
		button.clicked(() -> {
			if(!button.childrenPressed()){
				Vars.ui.showLoading();

				Timers.runTask(3f, () -> {
					Vars.ui.hideLoading();
					hide();
					try{
						slot.load();
						GameState.set(State.playing);
						Vars.ui.hideMenu();
					}catch(Exception e){
						e.printStackTrace();
						Vars.ui.hideMenu();
						GameState.set(State.menu);
						Vars.control.reset();
						Vars.ui.showError("$text.save.corrupted");
					}
				});
			}
		});
	}
}
