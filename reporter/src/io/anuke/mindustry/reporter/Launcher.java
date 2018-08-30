package io.anuke.mindustry.reporter;

import com.sun.net.httpserver.HttpServer;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

public class Launcher{
    private static final long REQUEST_TIME = 1000 * 6;

    public static void main(String[] args) throws IOException{
        ReportHandler handler = new ReportHandler();
        HashMap<String, Long> rateLimit = new HashMap<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/report", t -> {
            String key = t.getRemoteAddress().getAddress().getHostName();
            if(rateLimit.get(key) != null && (currentTimeMillis() - rateLimit.get(key)) < REQUEST_TIME){
                rateLimit.put(key, currentTimeMillis());
                out.println("connection " + key + " is being rate limited");
                return;
            }

            rateLimit.put(key, currentTimeMillis());
            byte[] bytes = new byte[t.getRequestBody().available()];
            new DataInputStream(t.getRequestBody()).readFully(bytes);
            handler.handle(new String(bytes));

            t.sendResponseHeaders(200, 0);
        });
        server.setExecutor(null);
        server.start();
        out.println("server up");
    }

}
