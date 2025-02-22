package mindustry.graphics;

import arc.*;
import arc.graphics.*;

/** Nvidia-specific utility class for querying GPU VRAM information. */
public class NvGpuInfo{
    private static final int GL_GPU_MEM_INFO_TOTAL_AVAILABLE_MEM_NVX = 0x9048;
    private static final int GL_GPU_MEM_INFO_CURRENT_AVAILABLE_MEM_NVX = 0x9049;

    private static boolean supported, initialized;

    public static int getMaxMemoryKB(){
        return hasMemoryInfo() ? Gl.getInt(GL_GPU_MEM_INFO_TOTAL_AVAILABLE_MEM_NVX) : 0;
    }

    public static int getAvailableMemoryKB(){
        return hasMemoryInfo() ? Gl.getInt(GL_GPU_MEM_INFO_CURRENT_AVAILABLE_MEM_NVX) : 0;
    }

    public static boolean hasMemoryInfo(){
        if(!initialized){
            supported = Core.graphics.supportsExtension("GL_NVX_gpu_memory_info");
            initialized = true;
        }
        return supported;
    }
}