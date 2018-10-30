package io.anuke.ucore.scene.builders;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;

public class dialog extends builder<dialog, Dialog>{
	Table previous = null;
	
	public dialog(String title){
		element = new Dialog(title);
		
		previous = build.context;
		build.context = element.content();
	}
	
	public <T extends Element> Cell<T> add(T t){
		return element.content().add(t);
	}
	
	public Table content(){
		return element.content();
	}
	
	public Label title(){
		return element.title();
	}
	
	public Table buttons(){
		return element.getButtonTable();
	}
	
	public Cell add(){
		return element.content().add();
	}
	
	public void row(){
		element.content().row();
	}
	
	public dialog aleft(){
		element.content().left();
		return this;
	}
	
	public dialog aright(){
		element.content().right();
		return this;
	}
	
	public dialog atop(){
		element.content().top();
		return this;
	}
	
	public dialog abottom(){
		element.content().bottom();
		return this;
	}
	
	public void end(){
		build.context = previous;
	}
}
