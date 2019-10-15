package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.input.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.gen.*;

import static io.anuke.mindustry.Vars.*;

public class PausedDialog extends FloatingDialog{
    private SaveDialog save = new SaveDialog();
    private LoadDialog load = new LoadDialog();
    private boolean wasClient = false;

    public PausedDialog(){
        super("$menu");
        shouldPause = true;

        shown(this::rebuild);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK){
                hide();
            }
        });
    }

    void rebuild(){
        cont.clear();

        update(() -> {
            if(state.is(State.menu) && isShown()){
                hide();
            }
        });

        if(!mobile){
            float dw = 210f;
            cont.defaults().width(dw).height(50).pad(5f);

            cont.addButton("$back", this::hide).colspan(2).width(dw * 2 + 20f);

            cont.row();
            if(world.isZone()){
                cont.addButton("$techtree", ui.tech::show);
            }else{
                cont.addButton("$database", ui.database::show);
            }
            cont.addButton("$settings", ui.settings::show);

            if(!state.rules.tutorial){
                if(!world.isZone() && !state.isEditor()){
                    cont.row();
                    cont.addButton("$savegame", save::show);
                    cont.addButton("$loadgame", load::show).disabled(b -> net.active());
                }

                cont.row();

                cont.addButton("$hostserver", () -> {
                    if(net.server() && steam){
                        platform.inviteFriends();
                    }else{
                        if(steam){
                            ui.host.runHost();
                        }else{
                            ui.host.show();
                        }
                    }
                }).disabled(b -> !((steam && net.server()) || !net.active())).colspan(2).width(dw * 2 + 20f).update(e -> e.setText(net.server() && steam ? "$invitefriends" : "$hostserver"));
            }

            cont.row();

            cont.addButton("$quit", this::showQuitConfirm).colspan(2).width(dw + 10f).update(s -> s.setText(control.saves.getCurrent() != null && control.saves.getCurrent().isAutosave() ? "$save.quit" : "$quit"));

        }else{
            cont.defaults().size(130f).pad(5);
            cont.addRowImageTextButton("$back", Icon.play2, this::hide);
            cont.addRowImageTextButton("$settings", Icon.tools, ui.settings::show);

            if(!world.isZone() && !state.isEditor()){
                cont.addRowImageTextButton("$save", Icon.save, save::show);

                cont.row();

                cont.addRowImageTextButton("$load", Icon.load, load::show).disabled(b -> net.active());
            }else{
                cont.row();
            }

            cont.addRowImageTextButton("$hostserver.mobile", Icon.host, ui.host::show).disabled(b -> net.active());

            cont.addRowImageTextButton("$quit", Icon.quit, this::showQuitConfirm).update(s -> s.setText(control.saves.getCurrent() != null && control.saves.getCurrent().isAutosave() ? "$save.quit" : "$quit"));
        }
    }

    void showQuitConfirm(){
        ui.showConfirm("$confirm", state.rules.tutorial ? "$quit.confirm.tutorial" : "$quit.confirm", () -> {
            if(state.rules.tutorial){
                Core.settings.put("playedtutorial", true);
                Core.settings.save();
            }
            wasClient = net.client();
            if(net.client()) netClient.disconnectQuietly();
            runExitSave();
            hide();
        });
    }

    public void runExitSave(){
        if(state.isEditor() && !wasClient){
            ui.editor.resumeEditing();
            return;
        }

        if(control.saves.getCurrent() == null || !control.saves.getCurrent().isAutosave() || state.rules.tutorial || wasClient){
            state.set(State.menu);
            logic.reset();
            return;
        }

        ui.loadAnd("$saveload", () -> {
            try{
                control.saves.getCurrent().save();
            }catch(Throwable e){
                e.printStackTrace();
                ui.showException("[accent]" + Core.bundle.get("savefail"), e);
            }
            state.set(State.menu);
            logic.reset();
        });
    }
}
