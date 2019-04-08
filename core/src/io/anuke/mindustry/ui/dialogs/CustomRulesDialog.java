package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.function.FloatConsumer;
import io.anuke.arc.function.FloatProvider;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Rules;

import static io.anuke.mindustry.Vars.tilesize;

public class CustomRulesDialog extends FloatingDialog{
    public final Rules rules = new Rules();
    private Table main;

    public CustomRulesDialog(){
        super("$mode.custom");

        rules.waves = true;
        rules.waveTimer = true;

        setFillParent(true);
        shown(this::setup);
        addCloseButton();
    }

    void setup(){
        cont.clear();
        cont.pane(m -> main = m);
        main.margin(10f);

        main.left().defaults().fillX().left().pad(5);
        main.row();
        main.addCheck("$rules.infiniteresources", b -> rules.infiniteResources = b).checked(b -> rules.infiniteResources);
        main.row();
        main.addCheck("$rules.wavetimer", b -> rules.waveTimer = b).checked(b -> rules.waveTimer);
        main.row();
        main.addCheck("$rules.waves", b -> rules.waves = b).checked(b -> rules.waves);
        main.row();
        main.addCheck("$rules.pvp", b -> rules.pvp = b).checked(b -> rules.pvp);
        main.row();
        main.addCheck("$rules.unitdrops", b -> rules.unitDrops = b).checked(b -> rules.unitDrops);
        main.row();
        main.addCheck("$rules.waitForWaveToEnd", b -> rules.waitForWaveToEnd = b).checked(b -> rules.waitForWaveToEnd);
        main.row();
        main.addCheck("$rules.limitedRespawns", b -> rules.limitedRespawns= b).checked(b -> rules.limitedRespawns);
        main.row();
        number("$rules.buildcostmultiplier", f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier);
        number("$rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier);
        number("$rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier);
        number("$rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier);
        number("$rules.playerdamagemultiplier", f -> rules.playerDamageMultiplier = f, () -> rules.playerDamageMultiplier);
        number("$rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);
        number("$rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200));
        number("$rules.respawntime", f -> rules.respawnTime = f * 60f, () -> rules.respawnTime / 60f);
        number("$rules.respawns", f -> rules.respawns = (int) f, () -> rules.respawns);
        number("$rules.wavespacing", f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f);
    }

    void number(String text, FloatConsumer cons, FloatProvider prov){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5);
            Platform.instance.addDialog(t.addField(prov.get() + "", s -> cons.accept(Strings.parseFloat(s)))
            .valid(Strings::canParsePositiveFloat).width(120f).left().get());
        });

        main.row();

    }
}
