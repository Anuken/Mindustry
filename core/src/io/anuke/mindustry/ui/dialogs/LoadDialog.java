package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.SaveIO.SaveException;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class LoadDialog extends FloatingDialog{
    ScrollPane pane;
    Table slots;

    public LoadDialog(){
        this("$loadgame");
    }

    public LoadDialog(String title){
        super(title);
        setup();

        shown(() -> {
            setup();
            Time.runTask(2f, () -> Core.scene.setScrollFocus(pane));
        });

        addCloseButton();
    }

    protected void setup(){
        cont.clear();

        slots = new Table();
        pane = new ScrollPane(slots);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        slots.marginRight(24);

        Time.runTask(2f, () -> Core.scene.setScrollFocus(pane));

        Array<SaveSlot> array = control.saves.getSaveSlots();

        for(SaveSlot slot : array){
            if(slot.isHidden()) continue;

            TextButton button = new TextButton("[accent]" + slot.getName(), "clear");
            button.getLabelCell().growX().left();
            button.getLabelCell().padBottom(8f);
            button.getLabelCell().top().left().growX();

            button.defaults().left();

            button.table(t -> {
                t.right();

                t.addImageButton("icon-floppy", "emptytoggle", 14 * 3, () -> {
                    slot.setAutosave(!slot.isAutosave());
                }).checked(slot.isAutosave()).right();

                t.addImageButton("icon-trash", "empty", 14 * 3, () -> {
                    ui.showConfirm("$confirm", "$save.delete.confirm", () -> {
                        slot.delete();
                        setup();
                    });
                }).size(14 * 3).right();

                t.addImageButton("icon-pencil-small", "empty", 14 * 3, () -> {
                    ui.showTextInput("$save.rename", "$save.rename.text", slot.getName(), text -> {
                        slot.setName(text);
                        setup();
                    });
                }).size(14 * 3).right();

                t.addImageButton("icon-save", "empty", 14 * 3, () -> {
                    if(!ios){
                        Platform.instance.showFileChooser(Core.bundle.get("save.export"), "Mindustry Save", file -> {
                            try{
                                slot.exportFile(file);
                                setup();
                            }catch(IOException e){
                                ui.showError(Core.bundle.format("save.export.fail", Strings.parseException(e, true)));
                            }
                        }, false, FileChooser.saveFiles);
                    }else{
                        try{
                            FileHandle file = Core.files.local("save-" + slot.getName() + "." + Vars.saveExtension);
                            slot.exportFile(file);
                            Platform.instance.shareFile(file);
                        }catch(Exception e){
                            ui.showError(Core.bundle.format("save.export.fail", Strings.parseException(e, true)));
                        }
                    }
                }).size(14 * 3).right();


            }).padRight(-10).growX();

            String color = "[lightgray]";

            button.defaults().padBottom(3);
            button.row();
            button.add(Core.bundle.format("save.map", color + (slot.getMap() == null ? Core.bundle.get("unknown") : slot.getMap().name())));
            button.row();
            button.add(Core.bundle.format("save.wave", color + slot.getWave()));
            button.row();
            button.label(() -> Core.bundle.format("save.autosave", color + Core.bundle.get(slot.isAutosave() ? "on" : "off")));
            button.row();
            button.label(() -> Core.bundle.format("save.playtime", color + slot.getPlayTime()));
            button.row();
            button.add(Core.bundle.format("save.date", color + slot.getDate())).colspan(2).padTop(5).right();
            button.row();
            modifyButton(button, slot);

            slots.add(button).uniformX().fillX().pad(4).padRight(-4).margin(10f).marginLeft(20f).marginRight(20f);
            slots.row();
        }

        cont.add(pane);

        addSetup();
    }

    public void addSetup(){
        boolean valids = false;
        for(SaveSlot slot : control.saves.getSaveSlots()) if(!slot.isHidden()) valids = true;

        if(!valids){

            slots.row();
            slots.addButton("$save.none", () -> {
            }).disabled(true).fillX().margin(20f).minWidth(340f).height(80f).pad(4f);
        }

        slots.row();

        if(ios) return;

        slots.addImageTextButton("$save.import", "icon-add", 14 * 3, () -> {
            Platform.instance.showFileChooser(Core.bundle.get("save.import"), "Mindustry Save", file -> {
                if(SaveIO.isSaveValid(file)){
                    try{
                        control.saves.importSave(file);
                        setup();
                    }catch(IOException e){
                        e.printStackTrace();
                        ui.showError(Core.bundle.format("save.import.fail", Strings.parseException(e, true)));
                    }
                }else{
                    ui.showError("$save.import.invalid");
                }
            }, true, FileChooser.saveFiles);
        }).fillX().margin(10f).minWidth(300f).height(70f).pad(4f).padRight(-4);
    }

    public void runLoadSave(SaveSlot slot){
        hide();
        ui.paused.hide();

        ui.loadAnd(() -> {
            try{
                slot.load();
                state.set(State.playing);
            }catch(SaveException e){
                Log.err(e);
                state.set(State.menu);
                logic.reset();
                ui.showError("$save.corrupted");
            }
        });
    }

    public void modifyButton(TextButton button, SaveSlot slot){
        button.clicked(() -> {
            if(!button.childrenPressed()){
                int build = slot.getBuild();
                runLoadSave(slot);
            }
        });
    }
}
