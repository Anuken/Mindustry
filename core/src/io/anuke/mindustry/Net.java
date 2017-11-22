package io.anuke.mindustry;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.ucore.function.Consumer;

//TODO stub
public class Net{
	private static boolean server;
	private static boolean active;
	private static ObjectMap<Class<?>, Consumer<?>> listeners = new ObjectMap<>();
	private static NetProvider provider;
	
	/**Connect to an address.*/
	public static void connect(String ip, String port) throws IOException{
		provider.connect(ip, port);
	}
	
	/**Send an object to all connected clients.*/
	public static void send(Object object){
		provider.send(object);
	}
	
	/**Sets the net provider, e.g. what handles sending, recieving and connecting.*/
	public static void setProvider(NetProvider provider){
		Net.provider = provider;
	}
	
	/**Registers a listener for when an object is recieved.*/
	public static <T> void handle(Class<T> type, Consumer<T> listener){
		listeners.put(type, listener);
	}
	
	/**Call to handle a packet being recieved.*/
	public static void handleNetReceived(Object object){
		if(listeners.get(object.getClass()) != null){
			
		}else{
			Gdx.app.error("Net", "Unhandled packet type: '" + object.getClass() + "'!");
		}
	}
	
	/**Whether the net is active, e.g. whether this is a multiplayer game.*/
	public static boolean active(){
		return active;
	}
	
	/**Whether this is a server or not.*/
	public static boolean server(){
		return server;
	}
	
	public static interface NetProvider{
		public void connect(String ip, String port) throws IOException;
		public void send(Object object);
	}
}
