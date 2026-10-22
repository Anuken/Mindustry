package arc.backend.robovm;

import arc.Files.FileType;
import arc.files.Fi;
import arc.util.ArcRuntimeException;

import java.io.File;

public class IOSFi extends Fi{
    protected IOSFi(String fileName, FileType type){
        super(fileName, type);
    }

    protected IOSFi(File file, FileType type){
        super(file, type);
    }

    @Override
    public Fi child(String name){
        if(file.getPath().length() == 0) return new IOSFi(new File(name), type);
        return new IOSFi(new File(file, name), type);
    }

    @Override
    public Fi parent(){
        File parent = file.getParentFile();
        if(parent == null){
            if(type == FileType.absolute)
                parent = new File("/");
            else
                parent = new File("");
        }
        return new IOSFi(parent, type);
    }

    @Override
    public Fi sibling(String name){
        if(file.getPath().length() == 0) throw new ArcRuntimeException("Cannot get the sibling of the root.");
        return new IOSFi(new File(file.getParent(), name), type);
    }

    @Override
    public File file(){
        if(type == FileType.internal) return new File(IOSFiles.internalPath, file.getPath());
        if(type == FileType.external) return new File(IOSFiles.externalPath, file.getPath());
        if(type == FileType.local) return new File(IOSFiles.localPath, file.getPath());
        return file;
    }

}
