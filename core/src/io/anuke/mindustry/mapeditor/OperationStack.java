package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.utils.Array;

public class OperationStack{
	private final static int maxSize = 10;
	private Array<DrawOperation> stack = new Array<>();
	private int index = 0;
	
	public OperationStack(){
		
	}
	
	public void clear(){
		for(DrawOperation op : stack){
			op.dispose();
		}
		stack.clear();
		index = 0;
	}
	
	public void add(DrawOperation action){
		stack.truncate(stack.size + index);
		index = 0;
		stack.add(action);

		if(stack.size > maxSize){
            stack.get(0).disposeFrom();
            stack.removeIndex(0);
        }
	}
	
	public boolean canUndo(){
		return !(stack.size - 1 + index < 0);
	}
	
	public boolean canRedo(){
		return !(index > -1 || stack.size + index < 0);
	}

	public void undo(){
		if(!canUndo()) return;

		stack.get(stack.size - 1 + index).undo();
		index --;
	}

	public void redo(){
		if(!canRedo()) return;
		
		index ++;
		stack.get(stack.size - 1 + index).redo();
		
	}
}
