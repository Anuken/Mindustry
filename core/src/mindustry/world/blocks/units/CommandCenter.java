package mindustry.world.blocks.units;

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
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class CommandCenter extends Block{
    public TextureRegionDrawable[] commandRegions = new TextureRegionDrawable[UnitCommand.all.length];
    public Color topColor = Pal.command, bottomColor = Color.valueOf("5e5e5e");
    public Effect effect = Fx.commandSend;

    public CommandCenter(String name){
        super(name);

        flags = EnumSet.of(BlockFlag.rally);
        destructible = true;
        solid = true;
        configurable = true;

        config(UnitCommand.class, (CommandBuild build, UnitCommand command) -> {
            build.team.data().command = command;
            effect.at(build);
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

    public class CommandBuild extends Building{

        @Override
        public Object config(){
            return team.data().command;
        }

        @Override
        public void draw(){
            super.draw();

            float size = 6f;

            Draw.color(bottomColor);
            Draw.rect(commandRegions[team.data().command.ordinal()].getRegion(), tile.drawx(), tile.drawy() - 1, size, size);
            Draw.color(topColor);
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