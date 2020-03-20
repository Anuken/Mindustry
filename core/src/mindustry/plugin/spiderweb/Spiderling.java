package mindustry.plugin.spiderweb;

import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.world.*;

import java.sql.*;

import static mindustry.Vars.spiderweb;

public class Spiderling{
    public String uuid;

    public ObjectWeb<String> names = new ObjectWeb<String>(){{
        loader = (web -> {try{
                web.preparedStatement = web.connect.prepareStatement("SELECT * FROM names WHERE uuid = ?");
                web.preparedStatement.setString(1, uuid);
                web.resultSet = web.preparedStatement.executeQuery();
                names.clear();

                while(web.resultSet.next()){
                    names.add(web.resultSet.getString("name"));
                }

                names.ready();
            }catch(SQLException e){
                e.printStackTrace();
            }
        });

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
        loader = (web -> {try{
                web.preparedStatement = web.connect.prepareStatement("SELECT * FROM unlocked_blocks WHERE uuid = ?");
                web.preparedStatement.setString(1, uuid);
                web.resultSet = web.preparedStatement.executeQuery();
                unlockedBlocks.clear();

                while(web.resultSet.next()){
                    unlockedBlocks.add(Vars.content.getByName(ContentType.block, web.resultSet.getString("block")));
                }

                unlockedBlocks.ready();
            }catch(SQLException e){
                e.printStackTrace();
            }
        });

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
        names.loader.get(spiderweb);
        unlockedBlocks.loader.get(spiderweb);
    }

    public void save(){
        //
    }

    public void log(){
        Log.warn("names: " + names);
        Log.warn("unlocked: " + unlockedBlocks);
    }
}
