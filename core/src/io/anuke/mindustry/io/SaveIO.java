package io.anuke.mindustry.io;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.versions.Save12;
import io.anuke.mindustry.io.versions.Save13;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Settings;

import java.io.*;

public class SaveIO{

	public static final OrderedMap<Integer, SaveFileVersion> versions = new OrderedMap(){{
		put(12, new Save12());
		put(13, new Save13());
	}};

	public static void saveToSlot(int slot){
		if(Vars.gwt){
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			write(stream);
			Settings.putString("save-"+slot+"-data", new String(Base64Coder.encode(stream.toByteArray())));
			Settings.save();
		}else{
			FileHandle file = fileFor(slot);
			file.moveTo(file.sibling(file.name() + "-backup." + file.extension()));
			try {
				write(fileFor(slot));
			}catch (Exception e){
				file.sibling(file.name() + "-backup." + file.extension()).moveTo(file);
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void loadFromSlot(int slot){
		if(Vars.gwt){
			String string = Settings.getString("save-"+slot+"-data");
			ByteArrayInputStream stream = new ByteArrayInputStream(Base64Coder.decode(string));
			load(stream);
		}else{
			load(fileFor(slot));
		}
	}

	public static DataInputStream readSlotMeta(int slot){
		if(Vars.gwt){
			String string = Settings.getString("save-"+slot+"-data");
			byte[] bytes = Base64Coder.decode(string);
			return new DataInputStream(new ByteArrayInputStream(bytes));
		}else{
			return new DataInputStream(fileFor(slot).read());
		}
	}
	
	public static boolean isSaveValid(int slot){
		try {
			return isSaveValid(readSlotMeta(slot));
		}catch (Exception e){
			return false;
		}
	}

	public static boolean checkConvert(int slot){

		try{
			DataInputStream stream = readSlotMeta(slot);
			int version = stream.readInt();
			stream.close();

			if(version != getVersion().version){
				UCore.log("Converting slot " + slot + ": " + version + " -> " + getVersion().version);
				stream = readSlotMeta(slot);
				SaveFileVersion target = versions.get(version);
				target.read(stream);
				stream.close();
				saveToSlot(slot);
				return true;
			}
			return false;

		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	public static boolean isSaveValid(FileHandle file){
		return isSaveValid(new DataInputStream(file.read()));
	}

	public static boolean isSaveValid(DataInputStream stream){

		try{
			SaveMeta meta = getData(stream);
			return meta.map != null;
		}catch (Exception e){
			return false;
		}
	}

	public static SaveMeta getData(int slot){
		return getData(readSlotMeta(slot));
	}
	
	public static SaveMeta getData(DataInputStream stream){
		
		try{
			SaveMeta meta =  getVersion().getData(stream);
			stream.close();
			return meta;
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static FileHandle fileFor(int slot){
		return Vars.saveDirectory.child(slot  + ".mins");
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
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	public static void load(FileHandle file){
		load(file.read());
	}
	
	//TODO GWT support
	public static void load(InputStream is){

		DataInputStream stream;
		
		try{
			stream = new DataInputStream(is);
			getVersion().read(stream);
			stream.close();
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	public static SaveFileVersion getVersion(){
		return versions.get(versions.orderedKeys().peek());
	}
}
