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
import mindustry.ai.BlockIndexer.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class CommandCenter extends Block{
    protected TextureRegionDrawable[] commandRegions = new TextureRegionDrawable[UnitCommand.all.length];
    protected Color topColor = Pal.command;
    protected Color bottomColor = Color.valueOf("5e5e5e");
    protected Effect effect = Fx.commandSend;

    public CommandCenter(String name){
        super(name);

        flags = EnumSet.of(BlockFlag.comandCenter);
        destructible = true;
        solid = true;
        configurable = true;
        config(Integer.class, (tile, value) -> {
            UnitCommand command = UnitCommand.all[value];
            ((CommandCenter)tile.block()).effect.at(tile);

            for(Tile center : indexer.getAllied(tile.team(), BlockFlag.comandCenter)){
                if(center.block() instanceof CommandCenter){
                    CommandCenterEntity entity = center.ent();
                    entity.command = command;
                }
            }

            Groups.unit.each(t -> t.team() == tile.team(), u -> u.controller().command(command));
            Events.fire(new CommandIssueEvent(tile, command));
        });
    }

    @Override
    public void load(){
        super.load();

        if(ui != null){
            for(UnitCommand cmd : UnitCommand.all){
                commandRegions[cmd.ordinal()] = ui.getIcon("command" + Strings.capitalize(cmd.name()));
            }
        }
    }

    public class CommandCenterEntity extends TileEntity{
        public UnitCommand command = UnitCommand.attack;

        @Override
        public void draw(){
            super.draw();

            float size = 6f;

            Draw.color(bottomColor);
            Draw.rect(commandRegions[command.ordinal()].getRegion(), x, y - 1, size, size);
            Draw.color(topColor);
            Draw.rect(commandRegions[command.ordinal()].getRegion(), x, y, size, size);
            Draw.color();
        }

        @Override
        public void buildConfiguration(Table table){
            ButtonGroup<ImageButton> group = new ButtonGroup<>();
            Table buttons = new Table();

            for(UnitCommand cmd : UnitCommand.all){
                buttons.addImageButton(commandRegions[cmd.ordinal()], Styles.clearToggleTransi, () -> tile.configure(cmd.ordinal()))
                .size(44).group(group).update(b -> b.setChecked(command == cmd));
            }
            table.add(buttons);
            table.row();
            table.label(() -> command.localized()).style(Styles.outlineLabel).center().growX().get().setAlignment(Align.center);
        }

        @Override
        public void placed(){
            super.placed();
            TileArray set = indexer.getAllied(team, BlockFlag.comandCenter);

            if(set.size() > 0){
                CommandCenterEntity oe = set.first().ent();
                command = oe.command;
            }
        }

        @Override
        public void onRemoved(){
            super.onRemoved();

            TileArray set = indexer.getAllied(team, BlockFlag.comandCenter);

            if(set.size() == 1){
                Groups.unit.each(t -> t.team() == team, u -> u.controller().command(UnitCommand.all[0]));
            }
        }

        @Override
        public Integer config(){
            return command.ordinal();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(command.ordinal());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            command = UnitCommand.all[read.b()];
        }
    }
}
