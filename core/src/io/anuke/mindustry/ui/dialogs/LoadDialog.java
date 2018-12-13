package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class LoadDialog extends FloatingDialog{
    ScrollPane pane;
    Table slots;

    public LoadDialog(){
        this("$text.loadgame");
    }

    public LoadDialog(String title){
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
        pane = new ScrollPane(slots);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        slots.marginRight(24);

        Timers.runTask(2f, () -> Core.scene.setScrollFocus(pane));

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
                    ui.showConfirm("$text.confirm", "$text.save.delete.confirm", () -> {
                        slot.delete();
                        setup();
                    });
                }).size(14 * 3).right();

                t.addImageButton("icon-pencil-small", "empty", 14 * 3, () -> {
                    ui.showTextInput("$text.save.rename", "$text.save.rename.text", slot.getName(), text -> {
                        slot.setName(text);
                        setup();
                    });
                }).size(14 * 3).right();

                t.addImageButton("icon-save", "empty", 14 * 3, () -> {
                    if(!ios){
                        Platform.instance.showFileChooser(Bundles.get("text.save.export"), "Mindustry Save", file -> {
                            try{
                                slot.exportFile(file);
                                setup();
                            }catch(IOException e){
                                ui.showError(Bundles.format("text.save.export.fail", Strings.parseException(e, false)));
                            }
                        }, false, saveExtension);
                    }else{
                        try{
                            FileHandle file = Gdx.files.local("save-" + slot.getName() + "." + Vars.saveExtension);
                            slot.exportFile(file);
                            Platform.instance.shareFile(file);
                        }catch(Exception e){
                            ui.showError(Bundles.format("text.save.export.fail", Strings.parseException(e, false)));
                        }
                    }
                }).size(14 * 3).right();


            }).padRight(-10).growX();

            String color = "[lightgray]";

            button.defaults().padBottom(3);
            button.row();
            button.add(Bundles.format("text.save.map", color + (slot.getMap() == null ? "Unknown" : slot.getMap().meta.name())));
            button.row();
            button.add(Bundles.get("text.level.mode") + " " + color + slot.getMode());
            button.row();
            button.add(Bundles.format("text.save.wave", color + slot.getWave()));
            button.row();
            button.add(Bundles.format("text.save.difficulty", color + slot.getDifficulty()));
            button.row();
            button.label(() -> Bundles.format("text.save.autosave", color + Bundles.get(slot.isAutosave() ? "text.on" : "text.off")));
            button.row();
            button.label(() -> Bundles.format("text.save.playtime", color + slot.getPlayTime()));
            button.row();
            button.add(Bundles.format("text.save.date", color + slot.getDate())).colspan(2).padTop(5).right();
            button.row();
            modifyButton(button, slot);

            slots.add(button).uniformX().fillX().pad(4).padRight(-4).margin(10f).marginLeft(20f).marginRight(20f);
            slots.row();
        }

        content().add(pane);

        addSetup();
    }

    public void addSetup(){
        boolean valids = false;
        for(SaveSlot slot : control.saves.getSaveSlots()) if(!slot.isHidden()) valids = true;

        if(!valids){

            slots.row();
            slots.addButton("$text.save.none", () -> {
            }).disabled(true).fillX().margin(20f).minWidth(340f).height(80f).pad(4f);
        }

        slots.row();

        if(ios) return;

        slots.addImageTextButton("$text.save.import", "icon-add", 14 * 3, () -> {
            Platform.instance.showFileChooser(Bundles.get("text.save.import"), "Mindustry Save", file -> {
                if(SaveIO.isSaveValid(file)){
                    try{
                        control.saves.importSave(file);
                        setup();
                    }catch(IOException e){
                        e.printStackTrace();
                        ui.showError(Bundles.format("text.save.import.fail", Strings.parseException(e, false)));
                    }
                }else{
                    ui.showError("$text.save.import.invalid");
                }
            }, true, saveExtension);
        }).fillX().margin(10f).minWidth(300f).height(70f).pad(4f).padRight(-4);
    }

    public void runLoadSave(SaveSlot slot){
        hide();
        ui.paused.hide();

        ui.loadLogic(() -> {
            try{
                slot.load();
                state.set(State.playing);
            }catch(Exception e){
                Log.err(e);
                state.set(State.menu);
                logic.reset();
                threads.runGraphics(() -> ui.showError("$text.save.corrupted"));
            }
        });
    }

    public void modifyButton(TextButton button, SaveSlot slot){
        button.clicked(() -> {
            if(!button.childrenPressed()){
                int build = slot.getBuild();
                if(SaveIO.breakingVersions.contains(build)){
                    ui.showInfo("$text.save.old");
                    slot.delete();
                }else{
                    runLoadSave(slot);
                }
            }
        });
    }
}
