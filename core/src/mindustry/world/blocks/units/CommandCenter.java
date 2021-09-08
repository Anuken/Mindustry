package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class CommandCenter extends Block{
    public final int timerEffect = timers ++;

    public TextureRegionDrawable[] commandRegions = new TextureRegionDrawable[UnitCommand.all.length];
    public Color topColor = null, bottomColor = Color.valueOf("5e5e5e");
    public Effect effect = Fx.commandSend;
    public float effectSize = 150f;
    public float forceRadius = 31f, forceStrength = 0.2f;

    public CommandCenter(String name){
        super(name);

        flags = EnumSet.of(BlockFlag.rally);
        update = true;
        solid = true;
        configurable = true;
        drawDisabled = false;
        logicConfigurable = true;

        config(UnitCommand.class, (CommandBuild build, UnitCommand command) -> {
            if(build.team.data().command != command){
                build.team.data().command = command;
                //do not spam effect
                if(build.timer(timerEffect, 60f)){
                    effect.at(build, effectSize);
                }
                Events.fire(new CommandIssueEvent(build, command));
            }
        });
    }

    @Override
    public void load(){
        super.load();

        if(Vars.ui != null){
            for(UnitCommand cmd : UnitCommand.all){
                commandRegions[cmd.ordinal()] = Vars.ui.getIcon("command" + Strings.capitalize(cmd.name()), "cancel");
            }
        }
    }

    @Override
    public boolean configSenseable(){
        return true;
    }

    public class CommandBuild extends Building{

        @Override
        public Object config(){
            return team.data().command;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            //push away allied units
            team.data().tree().intersect(x - forceRadius/2f, y - forceRadius/2f, forceRadius, forceRadius, u -> {
                if(!u.isPlayer()){
                    float dst = dst(u);
                    float rs = forceRadius + u.hitSize/2f;
                    if(dst < rs){
                        u.vel.add(Tmp.v1.set(u).sub(x, y).setLength(1f - dst / rs).scl(forceStrength));
                    }
                }
            });
        }

        @Override
        public void draw(){
            super.draw();

            float size = 6f;

            Draw.color(bottomColor);
            Draw.rect(commandRegions[team.data().command.ordinal()].getRegion(), tile.drawx(), tile.drawy() - 1, size, size);
            Draw.color(topColor == null ? team.color : topColor);
            Draw.rect(commandRegions[team.data().command.ordinal()].getRegion(), tile.drawx(), tile.drawy(), size, size);
            Draw.color();
        }

        @Override
        public void buildConfiguration(Table table){
            ButtonGroup<ImageButton> group = new ButtonGroup<>();
            Table buttons = new Table();

            for(UnitCommand cmd : UnitCommand.all){
                buttons.button(commandRegions[cmd.ordinal()], Styles.clearToggleTransi, () -> {
                    if(team.data().command != cmd) configure(cmd);
                }).size(44).group(group).update(b -> b.setChecked(team.data().command == cmd));
            }
            table.add(buttons);
            table.row();
            table.label(() -> team.data().command.localized()).style(Styles.outlineLabel).center().growX().get().setAlignment(Align.center);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(team.data().command.ordinal());
        }

        @Override
        public void read(Reads read, byte version){
            super.read(read, version);
            team.data().command = UnitCommand.all[read.b()];
        }
    }
}
