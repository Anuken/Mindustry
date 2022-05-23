package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MapPlayDialog extends BaseDialog{
    public @Nullable Runnable playListener;

    CustomRulesDialog dialog = new CustomRulesDialog();
    Rules rules;
    Gamemode selectedGamemode = Gamemode.survival;
    Map lastMap;

    public MapPlayDialog(){
        super("");
        setFillParent(false);

        onResize(() -> {
            if(lastMap != null){
                Rules rules = this.rules;
                show(lastMap);
                this.rules = rules;
            }
        });
    }

    public void show(Map map){
        show(map, false);
    }

    public void show(Map map, boolean playtesting){
        this.lastMap = map;
        title.setText(map.name());
        cont.clearChildren();

        //reset to any valid mode after switching to attack (one must exist)
        if(!selectedGamemode.valid(map)){
            selectedGamemode = Structs.find(Gamemode.all, m -> m.valid(map));
            if(selectedGamemode == null){
                selectedGamemode = Gamemode.survival;
            }
        }

        rules = map.applyRules(selectedGamemode);

        Table selmode = new Table();
        selmode.add("@level.mode").colspan(2);
        selmode.row();

        selmode.table(Tex.button, modes -> {
            int i = 0;
            for(Gamemode mode : Gamemode.all){
                if(mode.hidden) continue;

                modes.button(mode.toString(), Styles.flatToggleMenut, () -> {
                    selectedGamemode = mode;
                    rules = map.applyRules(mode);
                }).update(b -> b.setChecked(selectedGamemode == mode)).size(140f, 54f).disabled(!mode.valid(map));
                if(i++ % 2 == 1) modes.row();
            }
        });

        selmode.button("?", this::displayGameModeHelp).width(50f).fillY().padLeft(18f);

        cont.add(selmode);
        cont.row();
        cont.button("@customize", Icon.settings, () -> dialog.show(rules, () -> rules = map.applyRules(selectedGamemode))).height(50f).width(230);
        cont.row();
        cont.add(new BorderImage(map.safeTexture(), 3f)).size(mobile && !Core.graphics.isPortrait() ? 150f : 250f).get().setScaling(Scaling.fit);
        //only maps with survival are valid for high scores
        if(Gamemode.survival.valid(map)){
            cont.row();
            cont.label((() -> Core.bundle.format("level.highscore", map.getHightScore()))).pad(3f);
        }

        buttons.clearChildren();
        addCloseButton();

        buttons.button("@play", Icon.play, () -> {
            if(playListener != null) playListener.run();
            control.playMap(map, rules, playtesting);
            hide();
            ui.custom.hide();
        }).size(210f, 64f);

        show();
    }

    private void displayGameModeHelp(){
        BaseDialog d = new BaseDialog(Core.bundle.get("mode.help.title"));
        d.setFillParent(false);
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);
        table.row();
        for(Gamemode mode : Gamemode.values()){
            if(mode.hidden) continue;
            table.labelWrap("[accent]" + mode + ":[] [lightgray]" + mode.description()).width(400f);
            table.row();
        }

        d.cont.add(pane);
        d.buttons.button("@ok", d::hide).size(110, 50).pad(10f);
        d.show();
    }
}
