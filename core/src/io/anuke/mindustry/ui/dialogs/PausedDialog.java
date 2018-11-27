package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Input.Keys;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class PausedDialog extends FloatingDialog{
    private SaveDialog save = new SaveDialog();
    private LoadDialog load = new LoadDialog();
    private Table missionTable;

    public PausedDialog(){
        super("$text.menu");
        shouldPause = true;
        setup();

        shown(this::rebuild);

        keyDown(key -> {
            if(key == Keys.ESCAPE || key == Keys.BACK) {
                hide();
            }
        });
    }

    void rebuild(){
        missionTable.clear();
        if(world.getSector() != null && !world.getSector().complete){
            missionTable.add("[LIGHT_GRAY]" + Bundles.format("text.mission", ""));
            missionTable.row();
            missionTable.table(t -> {
                world.getSector().currentMission().display(t);
            });
            missionTable.row();
            missionTable.add(Bundles.format("text.sector", world.getSector().x + ", " + world.getSector().y));
        }
    }

    void setup(){
        update(() -> {
            if(state.is(State.menu) && isShown()){
                hide();
            }
        });

        content().table(t -> missionTable = t).colspan(mobile ? 3 : 1);
        content().row();

        if(!mobile){
            float dw = 210f;
            content().defaults().width(dw).height(50).pad(5f);

            content().addButton("$text.back", this::hide).colspan(2).width(dw*2 + 10f);

            content().row();
            content().addButton("$text.unlocks", ui.unlocks::show);
            content().addButton("$text.settings", ui.settings::show);

            content().row();
            content().addButton("$text.savegame", save::show).disabled(s -> world.getSector() != null);
            content().addButton("$text.loadgame", load::show).disabled(b -> Net.active());

            content().row();

            content().addButton("$text.hostserver", ui.host::show).disabled(b -> Net.active()).colspan(2).width(dw*2 + 10f);

            content().row();

            content().addButton("$text.quit", () -> {
                ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
                    if(Net.client()) netClient.disconnectQuietly();
                    runExitSave();
                    hide();
                });
            }).colspan(2).width(dw + 10f);

        }else{
            content().defaults().size(120f).pad(5);
            float isize = 14f * 4;

            content().addRowImageTextButton("$text.back", "icon-play-2", isize, () -> {
                hide();
            });
            content().addRowImageTextButton("$text.settings", "icon-tools", isize, ui.settings::show);
            content().addRowImageTextButton("$text.save", "icon-save", isize, save::show).disabled(b -> world.getSector() != null);

            content().row();

            content().addRowImageTextButton("$text.load", "icon-load", isize, load::show).disabled(b -> Net.active());
            content().addRowImageTextButton("$text.hostserver.mobile", "icon-host", isize, ui.host::show).disabled(b -> Net.active());
            content().addRowImageTextButton("$text.quit", "icon-quit", isize, () -> {
                ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
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

        ui.loadLogic("$text.saveload", () -> {
            try{
                control.saves.getCurrent().save();
            }catch(Throwable e){
                e.printStackTrace();
                threads.runGraphics(() -> ui.showError("[accent]" + Bundles.get("text.savefail")));
            }
            state.set(State.menu);
        });
    }
}
