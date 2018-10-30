package io.anuke.ucore.scene.builders;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;

public class table extends builder<table, Table>{
	Table previous = null;
	
	{
		element = new Table();
		
		if(build.context == null || build.context == element){
			element.setFillParent(true);
			build.getScene().add(element);
		}else{
			cell = build.context.add(element);
		}
		
		previous = build.context;
		build.context = element;
	}
	
	public table(){
		
	}
	
	public table(String background){
		element.background(background);
	}
	
	public <T extends Element> Cell<T> add(T t){
		return element.add(t);
	}
	
	public Cell add(){
		return element.add();
	}
	
	public Cell defaults(){
		return element.defaults();
	}
	
	public Cell row(){
		return element.row();
	}
	
	public table aleft(){
		element.left();
		return this;
	}
	
	public table aright(){
		element.right();
		return this;
	}
	
	public table atop(){
		element.top();
		return this;
	}
	
	public table abottom(){
		element.bottom();
		return this;
	}
	
	public table end(){
		build.context = previous;
		return this;
	}
}
