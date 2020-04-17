package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.Effects.*;
import mindustry.entities.type.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

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
        entityType = CommandCenterEntity::new;
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);
        ObjectSet<Tile> set = indexer.getAllied(tile.getTeam(), BlockFlag.comandCenter);

        if(set.size > 0){
            CommandCenterEntity entity = tile.ent();
            CommandCenterEntity oe = set.first().ent();
            entity.command = oe.command;
        }
    }

    @Override
    public void removed(Tile tile){
        super.removed(tile);

        ObjectSet<Tile> set = indexer.getAllied(tile.getTeam(), BlockFlag.comandCenter);

        if(set.size == 1){
            Units.each(tile.getTeam(), u -> u.onCommand(UnitCommand.all[0]));
        }
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

    @Override
    public void draw(Tile tile){
        CommandCenterEntity entity = tile.ent();
        super.draw(tile);

        float size = 6f;

        Draw.color(bottomColor);
        Draw.rect(commandRegions[entity.command.ordinal()].getRegion(), tile.drawx(), tile.drawy() - 1, size, size);
        Draw.color(topColor);
        Draw.rect(commandRegions[entity.command.ordinal()].getRegion(), tile.drawx(), tile.drawy(), size, size);
        Draw.color();
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        CommandCenterEntity entity = tile.ent();
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        Table buttons = new Table();

        for(UnitCommand cmd : UnitCommand.all){
            buttons.addImageButton(commandRegions[cmd.ordinal()], Styles.clearToggleTransi, () -> tile.configure(cmd.ordinal()))
            .size(44).group(group).update(b -> b.setChecked(entity.command == cmd));
        }
        table.add(buttons);
        table.row();
        table.label(() -> entity.command.localized()).style(Styles.outlineLabel).center().growX().get().setAlignment(Align.center);
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        UnitCommand command = UnitCommand.all[value];
        Effects.effect(((CommandCenter)tile.block()).effect, tile);

        for(Tile center : indexer.getAllied(tile.getTeam(), BlockFlag.comandCenter)){
            if(center.block() instanceof CommandCenter){
                CommandCenterEntity entity = center.ent();
                entity.command = command;
            }
        }

        Units.each(tile.getTeam(), u -> u.onCommand(command));
        Events.fire(new CommandIssueEvent(tile, command));
    }

    public class CommandCenterEntity extends TileEntity{
        public UnitCommand command = UnitCommand.attack;

        @Override
        public int config(){
            return command.ordinal();
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeByte(command.ordinal());
        }

        @Override
        public void read(DataInput stream, byte version) throws IOException{
            super.read(stream, version);
            command = UnitCommand.all[stream.readByte()];
        }
    }
}
