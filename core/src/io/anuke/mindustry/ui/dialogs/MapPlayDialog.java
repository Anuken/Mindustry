package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;

import static io.anuke.mindustry.Vars.state;

public class MapPlayDialog extends FloatingDialog{
    Difficulty difficulty = Difficulty.normal;
    CustomRulesDialog dialog = new CustomRulesDialog();
    Rules rules;
    Gamemode selectedGamemode;

    public MapPlayDialog(){
        super("");
        addCloseButton();
    }

    public void show(Map map){
        title.setText(map.name());
        cont.clearChildren();

        selectedGamemode = Gamemode.survival;
        rules = selectedGamemode.apply(new Rules());

        Table selmode = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
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
                //rules = mode.get();
                //dialog.selectedGamemode = null;
                dialog.rules = null;
            }).update(b -> b.setChecked(selectedGamemode == mode)).group(group).size(140f, 54f);
            if(i++ % 2 == 1) modes.row();
        }
        selmode.add(modes);
        selmode.addButton("?", this::displayGameModeHelp).width(50f).fillY().padLeft(18f);

        cont.add(selmode);
        cont.row();

        Difficulty[] ds = Difficulty.values();

        float s = 50f;

        Table sdif = new Table();

        sdif.add("$setting.difficulty.name").colspan(3);
        sdif.row();
        sdif.defaults().height(s + 4);
        sdif.addImageButton("icon-arrow-left", 10 * 3, () -> {
            difficulty = (ds[Mathf.mod(difficulty.ordinal() - 1, ds.length)]);
            state.wavetime = difficulty.waveTime;
        }).width(s);

        sdif.addButton("", () -> {}).update(t -> {
            t.setText(difficulty.toString());
            t.touchable(Touchable.disabled);
        }).width(180f);

        sdif.addImageButton("icon-arrow-right", 10 * 3, () -> {
            difficulty = (ds[Mathf.mod(difficulty.ordinal() + 1, ds.length)]);
            state.wavetime = difficulty.waveTime;
        }).width(s);
        sdif.addButton("$customize", () -> dialog.show(rules)).width(140).padLeft(10);

        cont.add(sdif);
        cont.row();
        if(map.hasTag("description")){
            cont.add(map.description()).color(Color.LIGHT_GRAY);
            cont.row();
        }
        cont.add(new BorderImage(map.texture, 3f)).grow().get().setScaling(Scaling.fit);

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
