package mindustry.ui.dialogs;

import arc.*;
import arc.input.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

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
            float dw = 220f;
            cont.defaults().width(dw).height(55).pad(5f);

            cont.addImageTextButton("$back", Icon.left, this::hide).colspan(2).width(dw * 2 + 20f);

            cont.row();
            if(world.isZone()){
                cont.addImageTextButton("$techtree", Icon.tree, ui.tech::show);
            }else{
                cont.addImageTextButton("$database", Icon.book, ui.database::show);
            }
            cont.addImageTextButton("$settings", Icon.settings, ui.settings::show);

            if(!state.rules.tutorial){
                if(!world.isZone() && !state.isEditor()){
                    cont.row();
                    cont.addImageTextButton("$savegame", Icon.save, save::show);
                    cont.addImageTextButton("$loadgame", Icon.upload, load::show).disabled(b -> net.active());
                }

                cont.row();

                cont.addImageTextButton("$hostserver", Icon.host, () -> {
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

            cont.addImageTextButton("$quit", Icon.exit, this::showQuitConfirm).colspan(2).width(dw + 20f).update(s -> s.setText(control.saves.getCurrent() != null && control.saves.getCurrent().isAutosave() ? "$save.quit" : "$quit"));

        }else{
            cont.defaults().size(130f).pad(5);
            cont.addRowImageTextButton("$back", Icon.play, this::hide);
            cont.addRowImageTextButton("$settings", Icon.settings, ui.settings::show);

            if(!world.isZone() && !state.isEditor()){
                cont.addRowImageTextButton("$save", Icon.save, save::show);

                cont.row();

                cont.addRowImageTextButton("$load", Icon.download, load::show).disabled(b -> net.active());
            }else{
                cont.row();
            }

            cont.addRowImageTextButton("$hostserver.mobile", Icon.host, ui.host::show).disabled(b -> net.active());

            cont.addRowImageTextButton("$quit", Icon.exit, this::showQuitConfirm).update(s -> {
                s.setText(control.saves.getCurrent() != null && control.saves.getCurrent().isAutosave() ? "$save.quit" : "$quit");
                s.getLabelCell().growX().wrap();
            });
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
