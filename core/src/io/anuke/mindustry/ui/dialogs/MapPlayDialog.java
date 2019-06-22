package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;

import static io.anuke.mindustry.Vars.*;

public class MapPlayDialog extends FloatingDialog{
    CustomRulesDialog dialog = new CustomRulesDialog();
    Rules rules;
    Gamemode selectedGamemode = Gamemode.survival;

    public MapPlayDialog(){
        super("");
        setFillParent(false);
    }

    public void show(Map map){
        title.setText(map.name());
        cont.clearChildren();

        //reset to a valid mode after switching to attack
        if((selectedGamemode == Gamemode.attack && !map.hasEnemyCore()) || (selectedGamemode == Gamemode.pvp && !map.hasOtherCores())){
            selectedGamemode = Gamemode.survival;
        }

        rules = map.rules();
        rules = selectedGamemode.apply(map.rules());

        Table selmode = new Table();
        selmode.add("$level.mode").colspan(4);
        selmode.row();
        int i = 0;

        Table modes = new Table();

        for(Gamemode mode : Gamemode.values()){
            if(mode.hidden) continue;

            if((mode == Gamemode.attack && !map.hasEnemyCore()) || (mode == Gamemode.pvp && !map.hasOtherCores())){
                continue;
            }

            modes.addButton(mode.toString(), "toggle", () -> {
                selectedGamemode = mode;
                rules = mode.apply(map.rules());
            }).update(b -> b.setChecked(selectedGamemode == mode)).size(140f, 54f);
            if(i++ % 2 == 1) modes.row();
        }
        selmode.add(modes);
        selmode.addButton("?", this::displayGameModeHelp).width(50f).fillY().padLeft(18f);

        cont.add(selmode);
        cont.row();
        cont.addImageTextButton("$customize", "icon-tools-small", iconsizesmall, () -> dialog.show(rules, () -> rules = (selectedGamemode == null ? map.rules() : selectedGamemode.apply(map.rules())))).width(230);
        cont.row();
        cont.add(new BorderImage(map.texture, 3f)).size(mobile && !Core.graphics.isPortrait() ? 150f : 250f).get().setScaling(Scaling.fit);

        buttons.clearChildren();
        addCloseButton();

        buttons.addImageTextButton("$play", "icon-play", 8*3, () -> {
            control.playMap(map, rules);
            hide();
            ui.custom.hide();
        }).size(210f, 64f);

        show();
    }

    private void displayGameModeHelp(){
        FloatingDialog d = new FloatingDialog(Core.bundle.get("mode.help.title"));
        d.setFillParent(false);
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);
        table.row();
        for(Gamemode mode : Gamemode.values()){
            if(mode.hidden) continue;
            table.labelWrap("[accent]" + mode.toString() + ":[] [lightgray]" + mode.description()).width(400f);
            table.row();
        }

        d.cont.add(pane);
        d.buttons.addButton("$ok", d::hide).size(110, 50).pad(10f);
        d.show();
    }
}
