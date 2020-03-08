package mindustry.plugin.spiderweb;

import arc.struct.*;
import arc.struct.Array;
import arc.util.*;
import mindustry.io.*;

import java.sql.*;

public class SpiderWeb{

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
                return sl;
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public void add(String uuid){
        try{
            preparedStatement = connect.prepareStatement("INSERT INTO uuids VALUES (?, ?)");
            preparedStatement.setString(1, uuid);
            preparedStatement.setString(2, "[]");
            preparedStatement.executeUpdate();

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void saveNames(Spiderling spiderling){
        try{
            preparedStatement = connect.prepareStatement("UPDATE uuids SET names = ? WHERE uuid = ?");
            preparedStatement.setString(1, JsonIO.write(spiderling.names.toArray(String.class)));
            preparedStatement.setString(2, spiderling.uuid);
            preparedStatement.execute();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
