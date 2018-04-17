package io.anuke.mindustry.io;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.IntMap;
import io.anuke.mindustry.io.versions.Save12;
import io.anuke.mindustry.io.versions.Save13;
import io.anuke.mindustry.io.versions.Save14;
import io.anuke.mindustry.io.versions.Save15;
import io.anuke.ucore.core.Settings;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class SaveIO{
	public static final IntMap<SaveFileVersion> versions = new IntMap<>();
	public static final Array<SaveFileVersion> versionArray = Array.with(
		new Save12(),
		new Save13(),
		new Save14(),
		new Save15()
	);

	static{
		for(SaveFileVersion version : versionArray){
			versions.put(version.version, version);
		}
	}

	public static void saveToSlot(int slot){
		if(gwt){
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			write(stream);
			Settings.putString("save-"+slot+"-data", new String(Base64Coder.encode(stream.toByteArray())));
			Settings.save();
		}else{
			FileHandle file = fileFor(slot);
			boolean exists = file.exists();
			if(exists) file.moveTo(file.sibling(file.name() + "-backup." + file.extension()));
			try {
				write(fileFor(slot));
			}catch (Exception e){
				if(exists) file.sibling(file.name() + "-backup." + file.extension()).moveTo(file);
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void loadFromSlot(int slot){
		if(gwt){
			String string = Settings.getString("save-"+slot+"-data");
			ByteArrayInputStream stream = new ByteArrayInputStream(Base64Coder.decode(string));
			load(stream);
		}else{
			load(fileFor(slot));
		}
	}

	public static DataInputStream getSlotStream(int slot){
		if(gwt){
			String string = Settings.getString("save-"+slot+"-data");
			byte[] bytes = Base64Coder.decode(string);
			return new DataInputStream(new ByteArrayInputStream(bytes));
		}else{
			return new DataInputStream(fileFor(slot).read());
		}
	}
	
	public static boolean isSaveValid(int slot){
		try {
			return isSaveValid(getSlotStream(slot));
		}catch (Exception e){
			return false;
		}
	}

	public static boolean isSaveValid(FileHandle file){
		return isSaveValid(new DataInputStream(file.read()));
	}

	public static boolean isSaveValid(DataInputStream stream){

		try{
			int version = stream.readInt();
			SaveFileVersion ver = versions.get(version);
			SaveMeta meta = ver.getData(stream);
			return meta.map != null;
		}catch (Exception e){
			return false;
		}
	}

	public static SaveMeta getData(int slot){
		return getData(getSlotStream(slot));
	}
	
	public static SaveMeta getData(DataInputStream stream){
		
		try{
			int version = stream.readInt();
			SaveMeta meta = versions.get(version).getData(stream);
			stream.close();
			return meta;
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static FileHandle fileFor(int slot){
		return saveDirectory.child(slot  + ".mins");
	}

	public static void write(FileHandle file){
		write(file.write(false));
	}

	public static void write(OutputStream os){

		DataOutputStream stream;
		
		try{
			stream = new DataOutputStream(os);
			getVersion().write(stream);
			stream.close();
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public static void load(FileHandle file){
		try {
			load(file.read());
		}catch (RuntimeException e){
			e.printStackTrace();
			FileHandle backup = file.sibling(file.name() + "-backup." + file.extension());
			if(backup.exists()){
				load(backup.read());
			}
		}
	}

	public static void load(InputStream is){
		logic.reset();

		DataInputStream stream;
		
		try{
			stream = new DataInputStream(is);
			int version = stream.readInt();
			SaveFileVersion ver = versions.get(version);

			ver.read(stream);

			stream.close();
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public static SaveFileVersion getVersion(){
		return versionArray.peek();
	}
}
