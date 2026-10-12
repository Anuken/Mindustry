/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package arc.backend.android.surfaceview;

import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * {@link EGLConfigChooser} implementation for GLES 2.0. Let's hope this really works for all devices. Includes MSAA/CSAA
 * config selection if requested. Taken from GLSurfaceView20, heavily modified to accommodate MSAA/CSAA.
 * @author mzechner
 */
public class ArcEglConfigChooser implements GLSurfaceView.EGLConfigChooser{
    public static final int EGL_COVERAGE_BUFFERS_NV = 0x30E0;
    public static final int EGL_COVERAGE_SAMPLES_NV = 0x30E1;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final String TAG = "ArcEglConfigChooser";
    protected final int[] mConfigAttribs;
    protected int mRedSize;
    protected int mGreenSize;
    protected int mBlueSize;
    protected int mAlphaSize;
    protected int mDepthSize;
    protected int mStencilSize;
    protected int mNumSamples;
    private int[] mValue = new int[1];

    public ArcEglConfigChooser(int r, int g, int b, int a, int depth, int stencil, int numSamples){
        mRedSize = r;
        mGreenSize = g;
        mBlueSize = b;
        mAlphaSize = a;
        mDepthSize = depth;
        mStencilSize = stencil;
        mNumSamples = numSamples;

        mConfigAttribs = new int[]{EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE};
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display){
        // get (almost) all configs available by using r=g=b=4 so we
        // can chose with big confidence :)
        int[] num_config = new int[1];
        egl.eglChooseConfig(display, mConfigAttribs, null, 0, num_config);
        int numConfigs = num_config[0];

        if(numConfigs <= 0){
            throw new IllegalArgumentException("No configs match configSpec");
        }

        // now actually read the configurations.
        EGLConfig[] configs = new EGLConfig[numConfigs];
        egl.eglChooseConfig(display, mConfigAttribs, configs, numConfigs, num_config);

        // FIXME remove this.
        // printConfigs(egl, display, configs);

        // chose the best one, taking into account multi sampling.

        // FIXME print the chosen config
        // printConfigs(egl, display, new EGLConfig[] { config });
        return chooseConfig(egl, display, configs);
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs){
        EGLConfig best = null;
        EGLConfig bestAA = null;
        EGLConfig safe = null; // default back to 565 when no exact match found

        for(EGLConfig config : configs){
            int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
            int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);

            // We need at least mDepthSize and mStencilSize bits
            if(d < mDepthSize || s < mStencilSize) continue;

            // We want an *exact* match for red/green/blue/alpha
            int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
            int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
            int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
            int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);

            // Match RGB565 as a fallback
            if(safe == null && r == 5 && g == 6 && b == 5 && a == 0){
                safe = config;
            }
            // if we have a match, we chose this as our non AA fallback if that one
            // isn't set already.
            if(best == null && r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize){
                best = config;

                // if no AA is requested we can bail out here.
                if(mNumSamples == 0){
                    break;
                }
            }

            // now check for MSAA support
            int hasSampleBuffers = findConfigAttrib(egl, display, config, EGL10.EGL_SAMPLE_BUFFERS, 0);
            int numSamples = findConfigAttrib(egl, display, config, EGL10.EGL_SAMPLES, 0);

            // We take the first sort of matching config, thank you.
            if(bestAA == null && hasSampleBuffers == 1 && numSamples >= mNumSamples && r == mRedSize && g == mGreenSize
            && b == mBlueSize && a == mAlphaSize){
                bestAA = config;
                continue;
            }

            // for this to work we need to call the extension glCoverageMaskNV which is not
            // exposed in the Android bindings. We'd have to link agains the NVidia SDK and
            // that is simply not going to happen.
// // still no luck, let's try CSAA support
            hasSampleBuffers = findConfigAttrib(egl, display, config, EGL_COVERAGE_BUFFERS_NV, 0);
            numSamples = findConfigAttrib(egl, display, config, EGL_COVERAGE_SAMPLES_NV, 0);

            // We take the first sort of matching config, thank you.
            if(bestAA == null && hasSampleBuffers == 1 && numSamples >= mNumSamples && r == mRedSize && g == mGreenSize
            && b == mBlueSize && a == mAlphaSize){
                bestAA = config;
            }
        }

        if(bestAA != null)
            return bestAA;
        else if(best != null)
            return best;
        else
            return safe;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue){
        if(egl.eglGetConfigAttrib(display, config, attribute, mValue)){
            return mValue[0];
        }
        return defaultValue;
    }

    private void printConfigs(EGL10 egl, EGLDisplay display, EGLConfig[] configs){
        int numConfigs = configs.length;
        Log.w(TAG, String.format("%d configurations", numConfigs));
        for(int i = 0; i < numConfigs; i++){
            Log.w(TAG, String.format("Configuration %d:\n", i));
            printConfig(egl, display, configs[i]);
        }
    }

    private void printConfig(EGL10 egl, EGLDisplay display, EGLConfig config){
        int[] attributes = {EGL10.EGL_BUFFER_SIZE, EGL10.EGL_ALPHA_SIZE, EGL10.EGL_BLUE_SIZE, EGL10.EGL_GREEN_SIZE,
        EGL10.EGL_RED_SIZE, EGL10.EGL_DEPTH_SIZE, EGL10.EGL_STENCIL_SIZE, EGL10.EGL_CONFIG_CAVEAT, EGL10.EGL_CONFIG_ID,
        EGL10.EGL_LEVEL, EGL10.EGL_MAX_PBUFFER_HEIGHT, EGL10.EGL_MAX_PBUFFER_PIXELS, EGL10.EGL_MAX_PBUFFER_WIDTH,
        EGL10.EGL_NATIVE_RENDERABLE, EGL10.EGL_NATIVE_VISUAL_ID, EGL10.EGL_NATIVE_VISUAL_TYPE,
        0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
        EGL10.EGL_SAMPLES, EGL10.EGL_SAMPLE_BUFFERS, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_TRANSPARENT_TYPE,
        EGL10.EGL_TRANSPARENT_RED_VALUE, EGL10.EGL_TRANSPARENT_GREEN_VALUE, EGL10.EGL_TRANSPARENT_BLUE_VALUE, 0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
        0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
        0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
        0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
        EGL10.EGL_LUMINANCE_SIZE, EGL10.EGL_ALPHA_MASK_SIZE, EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RENDERABLE_TYPE, 0x3042,
        // EGL10.EGL_CONFORMANT
        EGL_COVERAGE_BUFFERS_NV, /* true */
        EGL_COVERAGE_SAMPLES_NV};
        String[] names = {"EGL_BUFFER_SIZE", "EGL_ALPHA_SIZE", "EGL_BLUE_SIZE", "EGL_GREEN_SIZE", "EGL_RED_SIZE", "EGL_DEPTH_SIZE",
        "EGL_STENCIL_SIZE", "EGL_CONFIG_CAVEAT", "EGL_CONFIG_ID", "EGL_LEVEL", "EGL_MAX_PBUFFER_HEIGHT",
        "EGL_MAX_PBUFFER_PIXELS", "EGL_MAX_PBUFFER_WIDTH", "EGL_NATIVE_RENDERABLE", "EGL_NATIVE_VISUAL_ID",
        "EGL_NATIVE_VISUAL_TYPE", "EGL_PRESERVED_RESOURCES", "EGL_SAMPLES", "EGL_SAMPLE_BUFFERS", "EGL_SURFACE_TYPE",
        "EGL_TRANSPARENT_TYPE", "EGL_TRANSPARENT_RED_VALUE", "EGL_TRANSPARENT_GREEN_VALUE", "EGL_TRANSPARENT_BLUE_VALUE",
        "EGL_BIND_TO_TEXTURE_RGB", "EGL_BIND_TO_TEXTURE_RGBA", "EGL_MIN_SWAP_INTERVAL", "EGL_MAX_SWAP_INTERVAL",
        "EGL_LUMINANCE_SIZE", "EGL_ALPHA_MASK_SIZE", "EGL_COLOR_BUFFER_TYPE", "EGL_RENDERABLE_TYPE", "EGL_CONFORMANT",
        "EGL_COVERAGE_BUFFERS_NV", "EGL_COVERAGE_SAMPLES_NV"};
        int[] value = new int[1];
        for(int i = 0; i < attributes.length; i++){
            int attribute = attributes[i];
            String name = names[i];
            if(egl.eglGetConfigAttrib(display, config, attribute, value)){
                Log.w(TAG, String.format("  %s: %d\n", name, value[0]));
            }else{
                // Log.w(TAG, String.format("  %s: failed\n", name));
                egl.eglGetError();
// while (egl.eglGetError() != EGL10.EGL_SUCCESS)
// ;
            }
        }
    }
}
