package io.anuke.mindustry;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.ucore.function.Consumer;

//TODO stub
public class Net{
	private static boolean server;
	private static boolean active;
	private static ObjectMap<Class<?>, Consumer> listeners = new ObjectMap<>();
	private static ClientProvider clientProvider;
	private static ServerProvider serverProvider;
	
	/**Connect to an address.*/
	public static void connect(String ip, String port) throws IOException{
		clientProvider.connect(ip, port);
	}

	/**Host a server at an address*/
	public static void host(String port) throws IOException{
		serverProvider.host(port);
	}
	
	/**Send an object to all connected clients, or to the server if this is a client.*/
	public static void send(Object object){
		if(server){
			serverProvider.send(object);
		}else {
			clientProvider.send(object);
		}
	}
	
	/**Sets the net clientProvider, e.g. what handles sending, recieving and connecting to a server.*/
	public static void setClientProvider(ClientProvider provider){
		Net.clientProvider = provider;
	}

	/**Sets the net serverProvider, e.g. what handles hosting a server.*/
	public static void setServerProvider(ServerProvider provider){
		Net.serverProvider = provider;
	}
	
	/**Registers a client listener for when an object is recieved.*/
	public static <T> void handle(Class<T> type, Consumer<T> listener){
		listeners.put(type, listener);
	}
	
	/**Call to handle a packet being recieved (for the client).*/
	public static void handleNetReceived(Object object){
		if(listeners.get(object.getClass()) != null){
			listeners.get(object.getClass()).accept(object);
		}else{
			Gdx.app.error("Mindustry::Net", "Unhandled packet type: '" + object.getClass() + "'!");
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

	public static void registerClasses(Class<?>... classes){
		clientProvider.register(classes);
	}
	
	public static interface ClientProvider {
		public void connect(String ip, String port) throws IOException;
		public void send(Object object);
		public void register(Class<?>... types);
	}

	public static interface ServerProvider {
		public void host(String port) throws IOException;
		public void send(Object object);
		public void register(Class<?>... types);
	}
}
