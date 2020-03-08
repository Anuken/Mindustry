package mindustry.plugin.spiderweb;

import arc.*;
import arc.struct.*;
import arc.struct.Array;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.io.*;

import java.sql.*;

public class SpiderWeb implements ApplicationListener{

    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    private ObjectSet<String> uuids = new ObjectSet<>();

    public SpiderWeb(){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://localhost/nydus?user=root&password=root&useSSL=false");
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
                sl.names = Array.with(JsonIO.read(String[].class, resultSet.getString("names")));
//                sl.unlocked = JsonIO.read(ObjectSet.class, resultSet.getString("unlocked"));
                return sl;
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public void add(String uuid){
        try{
            preparedStatement = connect.prepareStatement("INSERT INTO uuids VALUES (?, ?, ?)");
            preparedStatement.setString(1, uuid);
            preparedStatement.setString(2, "[]");
            preparedStatement.setString(3, "[]");
            preparedStatement.executeUpdate();

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void save(Spiderling spiderling){
        try{
            preparedStatement = connect.prepareStatement("UPDATE uuids SET names = ?, unlocked = ? WHERE uuid = ?");
            Log.info(JsonIO.write(spiderling.unlocked));
            preparedStatement.setString(1, JsonIO.write(spiderling.names.toArray(String.class)));
            preparedStatement.setString(2, JsonIO.write(spiderling.unlocked));
            preparedStatement.setString(3, spiderling.uuid);
            preparedStatement.execute();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void init(){
        Events.on(BlockBuildEndEvent.class, event -> {
            if(event.breaking) return;
            if(event.player == null) return;
            if(event.player.spiderling.unlocked.contains(event.tile.block)) return;
            event.player.spiderling.unlocked.add(event.tile.block);
            event.player.spiderling.save();
        });
    }
}
