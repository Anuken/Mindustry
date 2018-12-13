package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class SaveDialog extends LoadDialog{

    public SaveDialog(){
        super("$text.savegame");

        update(() -> {
            if(state.is(State.menu) && isShown()){
                hide();
            }
        });
    }

    public void addSetup(){
        slots.row();
        slots.addImageTextButton("$text.save.new", "icon-add",14 * 3, () ->
                ui.showTextInput("$text.save", "$text.save.newslot", "", text -> {
                    ui.loadGraphics("$text.saving", () -> {
                        control.saves.addSave(text);
                        threads.runGraphics(() -> threads.run(() -> threads.runGraphics(this::setup)));
                    });
                })
        ).fillX().margin(10f).minWidth(300f).height(70f).pad(4f).padRight(-4);
    }

    @Override
    public void modifyButton(TextButton button, SaveSlot slot){
        button.clicked(() -> {
            if(button.childrenPressed()) return;

            ui.showConfirm("$text.overwrite", "$text.save.overwrite", () -> save(slot));
        });
    }

    void save(SaveSlot slot){

        ui.loadfrag.show("$text.saveload");

        Timers.runTask(5f, () -> {
            hide();
            ui.loadfrag.hide();
            try{
                slot.save();
            }catch(Throwable e){
                e.printStackTrace();

                ui.showError("[accent]" + Bundles.get("text.savefail"));
            }
        });
    }

}
