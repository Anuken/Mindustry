package mindustry.plugin.spiderweb;

import java.sql.*;

public class SpiderWeb{

    private Connection connect = null;

    public SpiderWeb(){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://localhost/nydus?user=root&password=root&useSSL=false");
        }catch(ClassNotFoundException | SQLException e){
            e.printStackTrace();
        }
    }
}
