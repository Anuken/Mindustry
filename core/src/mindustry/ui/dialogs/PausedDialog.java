package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class PausedDialog extends BaseDialog{
    private MapProcessorsDialog processors = new MapProcessorsDialog();
    private SaveDialog save = new SaveDialog();
    private LoadDialog load = new LoadDialog();
    private CustomRulesDialog rulesDialog = new CustomRulesDialog();

    public PausedDialog(){
        super("@menu");
        shouldPause = true;

        clearChildren();
        add(titleTable).growX().row();

        stack(cont, new Table(t -> {
            t.bottom().left();
            t.button(Icon.book, () -> {
                Rules toEdit = Vars.state.rules.copy();
                rulesDialog.show(toEdit, () -> state.rules.copy());
                rulesDialog.hidden(() -> {
                    //apply rule changes only once it is hidden
                    Vars.state.rules = toEdit;
                    Call.setRules(toEdit);
                });
            }).size(70f).tooltip("@customize").visible(() -> state.rules.allowEditRules && (net.server() || !net.active()));
        })).grow().row();

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

            cont.button("@objective", Icon.info, () -> ui.fullText.show("@objective", state.rules.sector.preset.description))
            .visible(() -> state.rules.sector != null && state.rules.sector.preset != null && state.rules.sector.preset.description != null).padTop(-60f);

            cont.button("@abandon", Icon.cancel, () -> ui.planet.abandonSectorConfirm(state.rules.sector, this::hide)).padTop(-60f)
            .disabled(b -> net.client()).visible(() -> state.rules.sector != null).row();

            cont.button("@back", Icon.left, this::hide).name("back");
            cont.button("@settings", Icon.settings, ui.settings::show).name("settings");

            if(!state.isCampaign() && !state.isEditor()){
                cont.row();
                cont.button("@savegame", Icon.save, save::show);
                cont.button("@loadgame", Icon.upload, load::show).disabled(b -> net.active());
            }

            cont.row();

            //the button runs out of space when the editor button is added, so use the mobile text
            cont.button(state.isEditor() ? "@hostserver.mobile" : "@hostserver", Icon.host, () -> {
                if(net.server() && steam){
                    platform.inviteFriends();
                }else{
                    ui.host.show();
                }
            }).disabled(b -> !((steam && net.server()) || !net.active())).colspan(state.isEditor() ? 1 : 2).width(state.isEditor() ? dw : dw * 2 + 10f)
                .update(e -> e.setText(net.server() && steam ? "@invitefriends" : state.isEditor() ? "@hostserver.mobile" : "@hostserver"));

            if(state.isEditor()){
                cont.button("@editor.worldprocessors", Icon.logic, () -> {
                    hide();
                    processors.show();
                });
            }

            cont.row();

            cont.button("@quit", Icon.exit, this::showQuitConfirm).colspan(2).width(dw + 10f).update(s -> s.setText(control.saves.getCurrent() != null && control.saves.getCurrent().isAutosave() ? "@save.quit" : "@quit"));

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
            runExitSave();
            hide();
        };

        if(confirmExit){
            ui.showConfirm("@confirm", "@quit.confirm", quit);
        }else{
            quit.run();
        }
    }

    public boolean checkPlaytest(){
        if(state.playtestingMap != null){
            //no exit save here
            var testing = state.playtestingMap;
            logic.reset();
            ui.editor.resumeAfterPlaytest(testing);
            return true;
        }
        return false;
    }

    public void runExitSave(){
        boolean wasClient = net.client();
        if(net.client()) netClient.disconnectQuietly();

        if(state.isEditor() && !wasClient){
            ui.editor.resumeEditing();
            return;
        }else if(checkPlaytest()){
            return;
        }

        if(control.saves.getCurrent() == null || !control.saves.getCurrent().isAutosave() || wasClient || state.gameOver){
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
