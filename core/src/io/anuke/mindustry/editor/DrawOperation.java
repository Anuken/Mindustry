package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.Disposable;
import io.anuke.mindustry.io.MapTileData;

import java.nio.ByteBuffer;

public class DrawOperation implements Disposable{
	/**Data to apply operation to.*/
	MapTileData data;
	/**Format:
	 *  position (int)
	 *  packed data FROM (use TileDataMarker's read/write methods)
	 *  packed data TO (use TileDataMarker's read/write methods)
	 */
	ByteBuffer operation;
	//TileDataMarker writer = new TileDataMarker();

	public DrawOperation(MapTileData data){
		this.data = data;
	}

	public void set(ByteBuffer operation) {
		this.operation = operation;
	}

	public void undo() {
		//TODO implement
	}

	public void redo() {
		//TODO implement
	}

	@Override
	public void dispose() {}

}
