package mindustry.plugin.spiderweb;

import arc.*;
import arc.struct.*;
import arc.struct.Array;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.io.*;

import java.sql.*;

public class SpiderWeb implements ApplicationListener{

    public Connection connect = null;
    public Statement statement = null;
    public PreparedStatement preparedStatement = null;
    public ResultSet resultSet = null;

    private ObjectSet<String> uuids = new ObjectSet<>();

    public SpiderWeb(){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://mindustry.nydus.app/nydus?user=crater&password=conveyor&useSSL=false&characterEncoding=UTF-8");
        }catch(ClassNotFoundException | SQLException e){
            e.printStackTrace();
        }
    }

    public boolean has(String uuid){
        return get(uuid) != null;
    }

    public Spiderling get(String uuid){
        try{
            preparedStatement = connect.prepareStatement("SELECT * FROM uuids WHERE uuid = ?");
            preparedStatement.setString(1, uuid);
            resultSet = preparedStatement.executeQuery();

            uuids.clear();
            if(resultSet.next()){
                Spiderling sl = new Spiderling();
                sl.uuid = resultSet.getString("uuid");
                return sl;
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public void add(String uuid){
        try{
            preparedStatement = connect.prepareStatement("INSERT INTO uuids VALUES (?)");
            preparedStatement.setString(1, uuid);
            preparedStatement.executeUpdate();

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void loadNames(Spiderling spiderling){
        try{
            preparedStatement = connect.prepareStatement("SELECT * FROM names WHERE uuid = ?");
            preparedStatement.setString(1, spiderling.uuid);
            resultSet = preparedStatement.executeQuery();
            spiderling.names.clear();

            while(resultSet.next()){
                spiderling.names.add(resultSet.getString("name"));
            }

            spiderling.names.ready();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void loadUnlockedBlocks(Spiderling spiderling){
        try{
            preparedStatement = connect.prepareStatement("SELECT * FROM unlocked_blocks WHERE uuid = ?");
            preparedStatement.setString(1, spiderling.uuid);
            resultSet = preparedStatement.executeQuery();
            spiderling.unlockedBlocks.clear();

            while(resultSet.next()){
                spiderling.unlockedBlocks.add(Vars.content.getByName(ContentType.block, resultSet.getString("block")));
            }

            spiderling.unlockedBlocks.ready();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }


    @Override
    public void init(){
        Events.on(BlockBuildEndEvent.class, event -> {
            if(event.breaking) return;
            if(event.player == null) return;
            if(!Vars.world.isZone()) return;
            if(event.player.spiderling.unlockedBlocks.contains(event.tile.block)) return;
            event.player.spiderling.unlockedBlocks.add(event.tile.block);
        });
    }
}
