package io.anuke.ucore.scene.builders;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.ui.layout.Table;

public class build{
	protected static Scene scene;
	protected static Table context;
	
	public static void begin(Scene scene){
		build.scene = scene;
	}
	
	public static void begin(){
		build.scene = Core.scene;
	}
	
	public static void begin(Table table){
		context = table;
	}
	
	public static void end(){
		context = null;
	}
	
	public static Scene getScene(){
		if(scene == null) throw new IllegalArgumentException("Scene context is null, set a table with begin() first!");
		return scene;
	}
	
	public static Table getTable(){
		if(context == null) throw new IllegalArgumentException("Table context is null, set a table with begin() first!");
		return context;
	}
}
