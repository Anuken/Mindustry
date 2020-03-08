package mindustry.plugin.spiderweb;

import arc.util.*;
import mindustry.world.*;

import java.sql.*;

import static mindustry.Vars.spiderweb;

public class Spiderling{
    public String uuid;

    public ObjectWeb<String> names = new ObjectWeb<String>(){{
        adder = (web, name) -> {try{
                web.preparedStatement = web.connect.prepareStatement("INSERT INTO names VALUES(?, ?) ON DUPLICATE KEY UPDATE uuid = uuid");
                web.preparedStatement.setString(1, uuid);
                web.preparedStatement.setString(2, name);
                web.preparedStatement.execute();
            }catch(SQLException e){
                e.printStackTrace();
            }
        };
    }};

    public ObjectWeb<Block> unlockedBlocks = new ObjectWeb<Block>(){{
        adder = (web, block) -> {try{
                web.preparedStatement = web.connect.prepareStatement("INSERT INTO unlocked_blocks VALUES(?, ?) ON DUPLICATE KEY UPDATE uuid = uuid");
                web.preparedStatement.setString(1, uuid);
                web.preparedStatement.setString(2, block.name);
                web.preparedStatement.execute();
            }catch(SQLException e){
                e.printStackTrace();
            }
        };
    }};

    public void load(){
        spiderweb.loadNames(this);
        spiderweb.loadUnlockedBlocks(this);
    }

    public void save(){
        //
    }

    public void log(){
        Log.warn("names: " + names);
        Log.warn("unlocked: " + unlockedBlocks);
    }
}
