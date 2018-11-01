package io.anuke.ucore.modules;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;

public abstract class ModuleCore extends ApplicationAdapter{
	private static ModuleCore instance;
	protected ObjectMap<Class<? extends Module>, Module> modules = new ObjectMap<>();
	protected Array<Module> modulearray = new Array<Module>();
	
	{
		instance=this; 
	}
	
	abstract public void init();
	public void preInit(){}
	public void postInit(){}
	public void update(){
		Inputs.update();
	}
	
	/**Adds a module to the list.*/
	protected <N extends Module> void module(N t){
		try{
			modules.put((Class<? extends Module>) t.getClass(), t);
			modulearray.add(t);
			t.preInit();
		}catch (RuntimeException e){
			throw e;
		}
	}
	
	@Override
	public void resize(int width, int height){
		Graphics.resize();
		for(Module module : modulearray){
			module.resize(width, height);
		}
	}
	
	@Override
	public final void create(){
		init();
		preInit();
		for(Module module : modulearray){
			module.init();
		}
		postInit();
	}
	
	@Override
	public void render(){
		for(Module module : modulearray){
			if(module.doUpdate) module.update();
		}
		
		update();
	}
	
	@Override
	public void pause(){
		for(Module module : modulearray)
			module.pause();
	}
	
	@Override
	public void resume(){
		for(Module module : modulearray)
			module.resume();
	}
	
	@Override
	public void dispose(){
		for(Module module : modulearray)
			module.dispose();
		
		Core.dispose();
	}
}
