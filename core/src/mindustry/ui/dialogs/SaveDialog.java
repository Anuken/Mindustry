package mindustry.ui.dialogs;

import arc.Core;
import arc.scene.ui.TextButton;
import arc.util.Time;
import mindustry.game.Saves.SaveSlot;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class SaveDialog extends LoadDialog{

    public SaveDialog(){
        super("$savegame");

        update(() -> {
            if(state.isMenu() && isShown()){
                hide();
            }
        });
    }

    @Override
    public void addSetup(){

        buttons.button("$save.new", Icon.add, () ->
            ui.showTextInput("$save", "$save.newslot", 30, "",
            text -> ui.loadAnd("$saving", () -> {
            control.saves.addSave(text);
            Core.app.post(() -> Core.app.post(this::setup));
        }))).fillX().margin(10f);
    }

    @Override
    public void modifyButton(TextButton button, SaveSlot slot){
        button.clicked(() -> {
            if(button.childrenPressed()) return;

            ui.showConfirm("$overwrite", "$save.overwrite", () -> save(slot));
        });
    }

    void save(SaveSlot slot){

        ui.loadfrag.show("$saving");

        Time.runTask(5f, () -> {
            hide();
            ui.loadfrag.hide();
            try{
                slot.save();
            }catch(Throwable e){
                e.printStackTrace();

                ui.showException("[accent]" + Core.bundle.get("savefail"), e);
            }
        });
    }

}
