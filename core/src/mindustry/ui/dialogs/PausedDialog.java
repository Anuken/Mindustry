package mindustry.ui.dialogs;

import arc.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class PausedDialog extends BaseDialog{
    private SaveDialog save = new SaveDialog();
    private LoadDialog load = new LoadDialog();
    private boolean wasClient = false;

    public PausedDialog(){
        super("@menu");
        shouldPause = true;

        shown(this::rebuild);

        addCloseListener();
    }

    void rebuild(){
        cont.clear();

        update(() -> {
            if(state.isMenu() && isShown()){
                hide();
            }
        });

        if(!mobile){
            float dw = 220f;
            cont.defaults().width(dw).height(55).pad(5f);

            cont.button("@back", Icon.left, this::hide).name("back");
            cont.button("@settings", Icon.settings, ui.settings::show).name("settings");

            if(!state.isCampaign() && !state.isEditor()){
                cont.row();
                cont.button("@savegame", Icon.save, save::show);
                cont.button("@loadgame", Icon.upload, load::show).disabled(b -> net.active());
            }

            cont.row();

            cont.button("@hostserver", Icon.host, () -> {
                if(net.server() && steam){
                    platform.inviteFriends();
                }else{
                    if(steam){
                        ui.host.runHost();
                    }else{
                        ui.host.show();
                    }
                }
            }).disabled(b -> !((steam && net.server()) || !net.active())).colspan(2).width(dw * 2 + 20f).update(e -> e.setText(net.server() && steam ? "@invitefriends" : "@hostserver"));

            cont.row();

            cont.button("@quit", Icon.exit, this::showQuitConfirm).colspan(2).width(dw + 20f).update(s -> s.setText(control.saves.getCurrent() != null && control.saves.getCurrent().isAutosave() ? "@save.quit" : "@quit"));

        }else{
            cont.defaults().size(130f).pad(5);
            cont.buttonRow("@back", Icon.play, this::hide);
            cont.buttonRow("@settings", Icon.settings, ui.settings::show);

            if(!state.isCampaign() && !state.isEditor()){
                cont.buttonRow("@save", Icon.save, save::show);

                cont.row();

                cont.buttonRow("@load", Icon.download, load::show).disabled(b -> net.active());
            }else if(state.isCampaign()){
                cont.buttonRow("@research", Icon.tree, ui.research::show);

                cont.row();

                cont.buttonRow("@planetmap", Icon.map, () -> {
                    hide();
                    ui.planet.show();
                });
            }else{
                cont.row();
            }

            cont.buttonRow("@hostserver.mobile", Icon.host, ui.host::show).disabled(b -> net.active());

            cont.buttonRow("@quit", Icon.exit, this::showQuitConfirm).update(s -> {
                s.setText(control.saves.getCurrent() != null && control.saves.getCurrent().isAutosave() ? "@save.quit" : "@quit");
                s.getLabelCell().growX().wrap();
            });
        }
    }

    void showQuitConfirm(){
        Runnable quit = () -> {
            wasClient = net.client();
            if(net.client()) netClient.disconnectQuietly();
            runExitSave();
            hide();
        };

        if(confirmExit){
            ui.showConfirm("@confirm", "@quit.confirm", quit);
        }else{
            quit.run();
        }

    }

    public void runExitSave(){
        if(state.isEditor() && !wasClient){
            ui.editor.resumeEditing();
            return;
        }

        if(control.saves.getCurrent() == null || !control.saves.getCurrent().isAutosave() || wasClient){
            logic.reset();
            return;
        }

        ui.loadAnd("@saving", () -> {
            try{
                control.saves.getCurrent().save();
            }catch(Throwable e){
                e.printStackTrace();
                ui.showException("[accent]" + Core.bundle.get("savefail"), e);
            }
            logic.reset();
        });
    }
}
