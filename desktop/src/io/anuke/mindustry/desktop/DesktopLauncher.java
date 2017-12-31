package io.anuke.mindustry.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.kryonet.util.InputStreamSender;
import com.esotericsoftware.minlog.Log;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Net.ServerProvider;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Registrator;
import io.anuke.mindustry.net.Streamable;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.ucore.UCore;
import io.anuke.ucore.scene.ui.TextField;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry");
		config.setMaximized(true);
		config.setWindowedMode(960, 540);
		config.setWindowIcon("sprites/icon.png");

		Mindustry.platforms = new PlatformFunction(){
			DateFormat format = SimpleDateFormat.getDateTimeInstance();
			
			@Override
			public String format(Date date){
				return format.format(date);
			}

			@Override
			public String format(int number){
				return NumberFormat.getIntegerInstance().format(number);
			}
			
			@Override
			public void openLink(String link){
				try{
					Desktop.getDesktop().browse(URI.create(link));
				}catch(IOException e){
					e.printStackTrace();
					Vars.ui.showError("Error opening link.");
				}
			}

			@Override
			public void addDialog(TextField field){
				
			}
		};
		
		Mindustry.args = Array.with(arg);

		Log.set(Log.LEVEL_DEBUG);

		Net.setClientProvider(new ClientProvider() {
			Client client;

			{
				client = new Client();
				client.start();
				client.addListener(new Listener(){
					@Override
					public void connected (Connection connection) {
						Connect c = new Connect();
						c.id = connection.getID();
						c.addressTCP = connection.getRemoteAddressTCP().toString();
						Net.handleClientReceived(c);
					}

					@Override
					public void disconnected (Connection connection) {
						Disconnect c = new Disconnect();
						Net.handleClientReceived(c);
					}

					@Override
					public void received (Connection connection, Object object) {
						if(object instanceof FrameworkMessage) return;
						Net.handleClientReceived(object);
					}
				});

				register(Registrator.getClasses());
			}

			@Override
			public void connect(String ip, int port) throws IOException {
				client.connect(5000, ip, port, port);
			}

			@Override
			public void disconnect() {
				client.close();
			}

			@Override
			public void send(Object object, SendMode mode) {
				if(mode == SendMode.tcp){
					client.sendTCP(object);
				}else{
					client.sendUDP(object);
				}
			}

			@Override
			public void updatePing() {
				client.updateReturnTripTime();
			}

			@Override
			public int getPing() {
				return client.getReturnTripTime();
			}

			@Override
			public void register(Class<?>... types) {
				for(Class<?> c : types){
					client.getKryo().register(c);
				}
			}
		});

		Net.setServerProvider(new ServerProvider() {
			Server server;

			{
				server = new Server();
				Thread thread = new Thread(server, "Kryonet Server");
				thread.setDaemon(true);
				thread.start();
				server.addListener(new Listener(){
					@Override
					public void connected (Connection connection) {
						Connect c = new Connect();
						c.id = connection.getID();
						c.addressTCP = connection.getRemoteAddressTCP().toString();
						Net.handleServerReceived(c, c.id);
					}

					@Override
					public void disconnected (Connection connection) {
						Disconnect c = new Disconnect();
						c.id = connection.getID();
						Net.handleServerReceived(c, c.id);
					}

					@Override
					public void received (Connection connection, Object object) {
						if(object instanceof FrameworkMessage) return;
						Net.handleServerReceived(object, connection.getID());
					}
				});

				register(Registrator.getClasses());
			}

			@Override
			public void host(int port) throws IOException {
				server.bind(port, port);
			}

			@Override
			public void close() {
				server.close();
			}

			@Override
			public void sendStream(int id, Streamable stream) {
				Connection connection = getByID(id);

				connection.addListener(new InputStreamSender(stream.stream, 512) {
					int id;

					protected void start () {
						//send an object so the receiving side knows how to handle the following chunks
						StreamBegin begin = new StreamBegin();
						begin.total = stream.stream.available();
						begin.type = stream.getClass();
						connection.sendTCP(begin);
						id = begin.id;
						UCore.log("Sending begin packet: " + begin);
					}

					protected Object next (byte[] bytes) {
						StreamChunk chunk = new StreamChunk();
						chunk.id = id;
						chunk.data = bytes;
						UCore.log("Sending chunk of size " + chunk.data.length);
						return chunk; //wrap the byte[] with an object so the receiving side knows how to handle it.
					}
				});
			}

			@Override
			public void send(Object object, SendMode mode) {
				if(mode == SendMode.tcp){
					server.sendToAllTCP(object);
				}else{
					server.sendToAllUDP(object);
				}
			}

			@Override
			public void sendTo(int id, Object object, SendMode mode) {
				if(mode == SendMode.tcp){
					server.sendToTCP(id, object);
				}else{
					server.sendToUDP(id, object);
				}
			}

			@Override
			public void sendExcept(int id, Object object, SendMode mode) {
				if(mode == SendMode.tcp){
					server.sendToAllExceptTCP(id, object);
				}else{
					server.sendToAllExceptUDP(id, object);
				}
			}

			@Override
			public void register(Class<?>... types) {
				for(Class<?> c : types){
					server.getKryo().register(c);
				}
			}

			Connection getByID(int id){
				for(Connection con : server.getConnections()){
					if(con.getID() == id){
						return con;
					}
				}

				throw new RuntimeException("Unable to find connection with ID " + id + "! Current connections: "
						+ Arrays.toString(server.getConnections()));
			}

		});
		
		new Lwjgl3Application(new Mindustry(), config);
	}
}
