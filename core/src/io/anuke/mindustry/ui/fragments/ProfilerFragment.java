package io.anuke.mindustry.ui.fragments;

import static io.anuke.mindustry.Vars.debug;

import io.anuke.mindustry.Profiler;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.function.StringSupplier;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;

public class ProfilerFragment implements Fragment{
	
	public void build(){
		if(debug){
			new table(){{
				abottom();
				aleft();
				new label((StringSupplier)()->"[purple]entities: " + Entities.amount()).left();
				row();
				new label("[red]DEBUG MODE").scale(0.5f).left();
			}}.end();
			
			new table(){{
				atop();
				new table("button"){{
					defaults().left().growX();
					atop();
					aleft();
					new label((StringSupplier)()->"[red]total: " 
					+ String.format("%.1f", (float)Profiler.total/Profiler.total*100f)+ "% - " + Profiler.total).left();
					row();
					new label((StringSupplier)()->"[yellow]draw: " 
					+ String.format("%.1f", (float)Profiler.draw/Profiler.total*100f)+ "% - " + Profiler.draw).left();
					row();
					new label((StringSupplier)()->"[green]blockDraw: " 
					+ String.format("%.1f", (float)Profiler.blockDraw/Profiler.total*100f)+ "% - " + Profiler.blockDraw).left();
					row();
					new label((StringSupplier)()->"[blue]entityDraw: " 
					+ String.format("%.1f", (float)Profiler.entityDraw/Profiler.total*100f)+ "% - " + Profiler.entityDraw).left();
					row();
					new label((StringSupplier)()->"[purple]entityUpdate: " 
					+ String.format("%.1f", (float)Profiler.entityUpdate/Profiler.total*100f)+ "% - " + Profiler.entityUpdate).left();
					row();
				}}.width(400f).end();
			}}.end();
		}
	}
}
