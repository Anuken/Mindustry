package io.anuke.mindustry.world.blocks.units;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.EnumSet;

import static io.anuke.mindustry.Vars.unitGroups;
import static io.anuke.mindustry.Vars.world;

public class CommandCenter extends Block{
    protected TextureRegion[] commandRegions = new TextureRegion[UnitCommand.values().length];

    public CommandCenter(String name){
        super(name);

        flags = EnumSet.of(BlockFlag.comandCenter);
        destructible = true;
        solid = true;
        configurable = true;
    }

    @Override
    public void load(){
        super.load();

        for(UnitCommand cmd : UnitCommand.values()){
            commandRegions[cmd.ordinal()] = Draw.region("command-" + cmd.name());
        }
    }

    @Override
    public void draw(Tile tile){
        CommandCenterEntity entity = tile.entity();
        super.draw(tile);

        Draw.rect(commandRegions[entity.command.ordinal()], tile.drawx(), tile.drawy());
    }

    @Override
    public void buildTable(Tile tile, Table table){
        //TODO
    }

    @Remote(called = Loc.server, forward = true, targets = Loc.both)
    public static void onCommandCenterSet(Player player, Tile tile, UnitCommand command){
        for(Tile center : world.indexer().getAllied(tile.getTeam(), BlockFlag.comandCenter)){
            if(center.block() instanceof CommandCenter){
                CommandCenterEntity entity = tile.entity();
                entity.command = command;
            }
        }

        for(BaseUnit unit : unitGroups[player.getTeam().ordinal()].all()){
            unit.onCommand(command);
        }
    }

    @Override
    public TileEntity getEntity(){
        return new CommandCenterEntity();
    }

    class CommandCenterEntity extends TileEntity{
        UnitCommand command = UnitCommand.idle;
    }
}
