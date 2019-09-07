package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.Saves.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.io.SaveIO.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.*;

import java.io.*;

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
        array.sort((slot, other) -> -Long.compare(slot.getTimestamp(), other.getTimestamp()));

        for(SaveSlot slot : array){
            if(slot.isHidden()) continue;

            TextButton button = new TextButton("", "clear");
            button.getLabel().remove();
            button.clearChildren();

            button.defaults().left();

            button.table(title -> {
                title.add("[accent]" + slot.getName()).left().growX().width(230f).wrap();

                title.table(t -> {
                    t.right();

                    t.addImageButton("icon-floppy", "emptytoggle", iconsize, () -> {
                        slot.setAutosave(!slot.isAutosave());
                    }).checked(slot.isAutosave()).right();

                    t.addImageButton("icon-trash", "empty", iconsize, () -> {
                        ui.showConfirm("$confirm", "$save.delete.confirm", () -> {
                            slot.delete();
                            setup();
                        });
                    }).size(iconsize).right();

                    t.addImageButton("icon-pencil", "empty", iconsize, () -> {
                        ui.showTextInput("$save.rename", "$save.rename.text", slot.getName(), text -> {
                            slot.setName(text);
                            setup();
                        });
                    }).size(iconsize).right();

                t.addImageButton("icon-save", "empty", iconsize, () -> {
                    if(!ios){
                        platform.showFileChooser(false, saveExtension, file -> {
                            try{
                                slot.exportFile(file);
                                setup();
                            }catch(IOException e){
                                ui.showError(Core.bundle.format("save.export.fail", Strings.parseException(e, true)));
                            }
                        });
                    }else{
                        try{
                            FileHandle file = Core.files.local("save-" + slot.getName() + "." + saveExtension);
                            slot.exportFile(file);
                            platform.shareFile(file);
                        }catch(Exception e){
                            ui.showError(Core.bundle.format("save.export.fail", Strings.parseException(e, true)));
                        }
                    }
                }).size(iconsize).right();


                }).padRight(-10).growX();
            }).growX().colspan(2);
            button.row();

            String color = "[lightgray]";
            TextureRegion def = Core.atlas.find("nomap");

            button.left().add(new BorderImage(def, 4f)).update(i -> {
                TextureRegionDrawable draw = (TextureRegionDrawable)i.getDrawable();
                if(draw.getRegion().getTexture().isDisposed()){
                    draw.setRegion(def);
                }

                Texture text = slot.previewTexture();
                if(draw.getRegion() == def && text != null){
                    draw.setRegion(new TextureRegion(text));
                }
                i.setScaling(Scaling.fit);
            }).left().size(160f).padRight(6);

            button.table(meta -> {
                meta.left().top();
                meta.defaults().padBottom(-2).left().width(290f);
                meta.row();
                meta.labelWrap(Core.bundle.format("save.map", color + (slot.getMap() == null ? Core.bundle.get("unknown") : slot.getMap().name())));
                meta.row();
                meta.labelWrap(slot.mode().toString() + " /" + color + " " + Core.bundle.format("save.wave", color + slot.getWave()));
                meta.row();
                meta.labelWrap(() -> Core.bundle.format("save.autosave", color + Core.bundle.get(slot.isAutosave() ? "on" : "off")));
                meta.row();
                meta.labelWrap(() -> Core.bundle.format("save.playtime", color + slot.getPlayTime()));
                meta.row();
                meta.labelWrap(color + slot.getDate());
                meta.row();
            }).left().growX().width(250f);

            modifyButton(button, slot);

            slots.add(button).uniformX().fillX().pad(4).padRight(-4).margin(10f).row();
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

        slots.addImageTextButton("$save.import", "icon-add", iconsize, () -> {
            platform.showFileChooser(true, saveExtension, file -> {
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
            });
        }).fillX().margin(10f).minWidth(300f).height(70f).pad(4f).padRight(-4);
    }

    public void runLoadSave(SaveSlot slot){
        hide();
        ui.paused.hide();

        ui.loadAnd(() -> {
            try{
                Net.reset();
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
                runLoadSave(slot);
            }
        });
    }
}
