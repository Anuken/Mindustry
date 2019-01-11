package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.style.Drawable;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;

import static io.anuke.mindustry.Vars.*;

public class PausedDialog extends FloatingDialog{
    private SaveDialog save = new SaveDialog();
    private LoadDialog load = new LoadDialog();
    private Table missionTable;

    public PausedDialog(){
        super("$menu");
        shouldPause = true;
        setup();

        shown(this::rebuild);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK) {
                hide();
            }
        });
    }

    void rebuild(){
        missionTable.clear();
        missionTable.background((Drawable) null);
    }

    void setup(){
        update(() -> {
            if(state.is(State.menu) && isShown()){
                hide();
            }
        });

        cont.table(t -> missionTable = t).colspan(mobile ? 3 : 2);
        cont.row();

        if(!mobile){
            float dw = 210f;
            cont.defaults().width(dw).height(50).pad(5f);

            cont.addButton("$back", this::hide).colspan(2).width(dw*2 + 20f);

            cont.row();
            cont.addButton("$unlocks", ui.unlocks::show);
            cont.addButton("$settings", ui.settings::show);

            cont.row();
            cont.addButton("$savegame", save::show);
            cont.addButton("$loadgame", load::show).disabled(b -> Net.active());

            cont.row();

            cont.addButton("$hostserver", ui.host::show).disabled(b -> Net.active()).colspan(2).width(dw*2 + 20f);

            cont.row();

            cont.addButton("$quit", () -> {
                ui.showConfirm("$confirm", "$quit.confirm", () -> {
                    if(Net.client()) netClient.disconnectQuietly();
                    runExitSave();
                    hide();
                });
            }).colspan(2).width(dw + 10f);

        }else{
            cont.defaults().size(120f).pad(5);
            float isize = 14f * 4;

            cont.addRowImageTextButton("$back", "icon-play-2", isize, this::hide);
            cont.addRowImageTextButton("$settings", "icon-tools", isize, ui.settings::show);
            cont.addRowImageTextButton("$save", "icon-save", isize, save::show);

            cont.row();

            cont.addRowImageTextButton("$load", "icon-load", isize, load::show).disabled(b -> Net.active());
            cont.addRowImageTextButton("$hostserver.mobile", "icon-host", isize, ui.host::show).disabled(b -> Net.active());
            cont.addRowImageTextButton("$quit", "icon-quit", isize, () -> {
                ui.showConfirm("$confirm", "$quit.confirm", () -> {
                    if(Net.client()) netClient.disconnectQuietly();
                    runExitSave();
                    hide();
                });
            });
        }
    }

    public void runExitSave(){
        if(control.saves.getCurrent() == null ||
                !control.saves.getCurrent().isAutosave()){
            state.set(State.menu);
            return;
        }

        ui.loadAnd("$saveload", () -> {
            try{
                control.saves.getCurrent().save();
            }catch(Throwable e){
                e.printStackTrace();
               ui.showError("[accent]" + Core.bundle.get("savefail"));
            }
            state.set(State.menu);
        });
    }
}
