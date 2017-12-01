package io.anuke.mindustry.editor;

import io.anuke.ucore.modules.ModuleCore;

public class Editor extends ModuleCore{
	public static EditorControl control;
	public static EditorUI ui;

	@Override
	public void init(){
		module(control = new EditorControl());
		module(ui = new EditorUI());
	}

}
