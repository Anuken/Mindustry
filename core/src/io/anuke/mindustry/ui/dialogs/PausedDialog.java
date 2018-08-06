package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class PausedDialog extends FloatingDialog{
    public boolean wasPaused = false;
    private SaveDialog save = new SaveDialog();
    private LoadDialog load = new LoadDialog();
    private Table missionTable;

    public PausedDialog(){
        super("$text.menu");
        setup();

        shown(this::rebuild);
    }

    void rebuild(){
        missionTable.clear();
        if(world.getSector() != null && !world.getSector().complete){
            missionTable.add("[LIGHT_GRAY]" + Bundles.format("text.mission", ""));
            missionTable.row();
            missionTable.table(t -> {
                world.getSector().currentMission().display(t);
            });
        }
    }

    void setup(){
        update(() -> {
            if(state.is(State.menu) && isShown()){
                hide();
            }
        });

        shown(() -> {
            wasPaused = state.is(State.paused);
            if(!Net.active()) state.set(State.paused);
        });

        content().table(t -> missionTable = t);
        content().row();

        if(!mobile){
            content().defaults().width(220).height(50);

            content().addButton("$text.back", () -> {
                hide();
                if((!wasPaused || Net.active()) && !state.is(State.menu))
                    state.set(State.playing);
            });

            content().row();
            content().addButton("$text.settings", ui.settings::show);

            content().row();
            content().addButton("$text.savegame", () -> {
                save.show();
            }).disabled(s -> world.getSector() != null);

            content().row();
            content().addButton("$text.loadgame", () -> {
                load.show();
            }).disabled(b -> Net.active());

            content().row();

            content().addButton("$text.hostserver", () -> {
                if(!gwt){
                    ui.host.show();
                }else{
                    ui.showInfo("$text.web.unsupported");
                }
            }).disabled(b -> Net.active());


            content().row();

            content().addButton("$text.quit", () -> {
                ui.showConfirm("$text.confirm", "$text.quit.confirm", () -> {
                    if(Net.client()) netClient.disconnectQuietly();
                    runExitSave();
                    hide();
                });
            });

        }else{
            content().defaults().size(120f).pad(5);
            float isize = 14f * 4;

            content().addRowImageTextButton("$text.back", "icon-play-2", isize, () -> {
                hide();
                if(!wasPaused && !state.is(State.menu))
                    state.set(State.playing);
            });
            content().addRowImageTextButton("$text.settings", "icon-tools", isize, ui.settings::show);
            content().addRowImageTextButton("$text.save", "icon-save", isize, save::show).disabled(b -> world.getSector() != null);

            content().row();

            content().addRowImageTextButton("$text.load", "icon-load", isize, load::show).disabled(b -> Net.active());
            content().addRowImageTextButton("$text.host", "icon-host", isize, ui.host::show).disabled(b -> Net.active());
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
        if(control.getSaves().getCurrent() == null ||
                !control.getSaves().getCurrent().isAutosave()){
            state.set(State.menu);
            return;
        }

        ui.loadLogic("$text.saveload", () -> {
            try{
                control.getSaves().getCurrent().save();
            }catch(Throwable e){
                e.printStackTrace();
                threads.runGraphics(() -> ui.showError("[orange]" + Bundles.get("text.savefail")));
            }
            state.set(State.menu);
        });
    }
}
