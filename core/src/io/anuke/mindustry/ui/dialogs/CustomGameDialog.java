package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.RulePreset;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;

import static io.anuke.mindustry.Vars.*;

public class CustomGameDialog extends FloatingDialog{
    Difficulty difficulty = Difficulty.normal;
    RulePreset lastPreset = RulePreset.survival;
    CustomRulesDialog dialog = new CustomRulesDialog();

    public CustomGameDialog(){
        super("$customgame");
        addCloseButton();
        shown(this::setup);

        onResize(this::setup);
    }

    void setup(){
        if(lastPreset == null){
            lastPreset = RulePreset.survival;
        }
        cont.clear();

        Table maps = new Table();
        maps.marginRight(14);
        ScrollPane pane = new ScrollPane(maps);
        pane.setFadeScrollBars(false);

        int maxwidth = (Core.graphics.getHeight() > Core.graphics.getHeight() ? 2 : 4);

        Table selmode = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        selmode.add("$level.mode").colspan(4);
        selmode.row();
        int i = 0;

        Table modes = new Table();
        modes.marginBottom(5);

        for(RulePreset mode : RulePreset.values()){
            modes.addButton(mode.toString(), "toggle", () -> {
                lastPreset = mode;
            }).update(b -> b.setChecked(lastPreset == mode)).group(group).size(140f, 54f);
            if(i++ % 2 == 1) modes.row();
        }
        selmode.add(modes);
        selmode.addButton("$mode.custom", "toggle", () -> {
        })
        .update(b -> b.setChecked(lastPreset == null)).size(108f).group(group).get().tapped(() -> {
            lastPreset = null;
            dialog.show();
        });
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

        sdif.addButton("", () -> {
        })
        .update(t -> {
            t.setText(difficulty.toString());
            t.touchable(Touchable.disabled);
        }).width(180f);

        sdif.addImageButton("icon-arrow-right", 10 * 3, () -> {
            difficulty = (ds[Mathf.mod(difficulty.ordinal() + 1, ds.length)]);
            state.wavetime = difficulty.waveTime;
        }).width(s);

        cont.add(sdif).visible(() -> lastPreset != null);
        cont.row();

        float images = 146f;

        i = 0;
        maps.defaults().width(170).fillY().top().pad(4f);
        for(Map map : world.maps.all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            ImageButton image = new ImageButton(new TextureRegion(map.texture), "clear");
            image.margin(5);
            image.getImageCell().size(images);
            image.top();
            image.row();
            image.add("[accent]" + map.name()).pad(3f).growX().wrap().get().setAlignment(Align.center, Align.center);
            image.row();
            image.label((() -> Core.bundle.format("level.highscore", map.getHightScore()))).pad(3f);

            BorderImage border = new BorderImage(map.texture, 3f);
            border.setScaling(Scaling.fit);
            image.replaceImage(border);

            image.clicked(() -> {
                hide();
                control.playMap(map, lastPreset == null ? dialog.rules : lastPreset.get());
            });

            maps.add(image);

            i++;
        }

        if(world.maps.all().size == 0){
            maps.add("$maps.none").pad(50);
        }

        cont.add(pane).uniformX();
    }

    private void displayGameModeHelp(){
        FloatingDialog d = new FloatingDialog(Core.bundle.get("mode.help.title"));
        d.setFillParent(false);
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);
        table.row();
        for(RulePreset mode : RulePreset.values()){
            table.labelWrap("[accent]" + mode.toString() + ":[] [lightgray]" + mode.description()).width(400f);
            table.row();
        }

        d.cont.add(pane);
        d.buttons.addButton("$ok", d::hide).size(110, 50).pad(10f);
        d.show();
    }

}
