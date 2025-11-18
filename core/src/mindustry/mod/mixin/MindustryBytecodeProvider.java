package mindustry.mod.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MindustryBytecodeProvider implements IClassBytecodeProvider{

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException{
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException{
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException{
        String path = name.replace('.', '/') + ".class";
        URL resource = Thread.currentThread().getContextClassLoader().getResource(path);

        if(resource == null){
            throw new ClassNotFoundException("Could not find class " + name);
        }

        byte[] bytes = readClassBytes(resource);

        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, readerFlags != 0 ? readerFlags : ClassReader.EXPAND_FRAMES);

        return classNode;
    }

    private byte[] readClassBytes(URL url) throws IOException{
        try(InputStream in = url.openStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream()){

            byte[] buffer = new byte[8192];
            int bytesRead;
            while((bytesRead = in.read(buffer)) != -1){
                out.write(buffer, 0, bytesRead);
            }

            return out.toByteArray();
        }
    }
}
