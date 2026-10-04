package arc.graphics.gl;

import arc.*;
import arc.struct.Bits;
import arc.util.*;

import java.nio.*;
import java.util.*;

public class Gl{
    private static final boolean optimize = true;

    public static final int
    esVersion20 = 1,
    depthBufferBit = 0x00000100,
    stencilBufferBit = 0x00000400,
    colorBufferBit = 0x00004000,
    falseV = 0,
    trueV = 1,
    points = 0x0000,
    lines = 0x0001,
    lineLoop = 0x0002,
    lineStrip = 0x0003,
    triangles = 0x0004,
    triangleStrip = 0x0005,
    triangleFan = 0x0006,
    zero = 0,
    one = 1,
    srcColor = 0x0300,
    oneMinusSrcColor = 0x0301,
    srcAlpha = 0x0302,
    oneMinusSrcAlpha = 0x0303,
    dstAlpha = 0x0304,
    oneMinusDstAlpha = 0x0305,
    dstColor = 0x0306,
    oneMinusDstColor = 0x0307,
    srcAlphaSaturate = 0x0308,
    funcAdd = 0x8006,
    blendEquation = 0x8009,
    blendEquationRgb = 0x8009,
    blendEquationAlpha = 0x883D,
    funcSubtract = 0x800A,
    funcReverseSubtract = 0x800B,
    min = 0x8007,
    max = 0x8008,
    blendDstRgb = 0x80C8,
    blendSrcRgb = 0x80C9,
    blendDstAlpha = 0x80CA,
    blendSrcAlpha = 0x80CB,
    constantColor = 0x8001,
    oneMinusConstantColor = 0x8002,
    constantAlpha = 0x8003,
    oneMinusConstantAlpha = 0x8004,
    blendColor = 0x8005,
    arrayBuffer = 0x8892,
    elementArrayBuffer = 0x8893,
    arrayBufferBinding = 0x8894,
    elementArrayBufferBinding = 0x8895,
    streamDraw = 0x88E0,
    staticDraw = 0x88E4,
    dynamicDraw = 0x88E8,
    bufferSize = 0x8764,
    bufferUsage = 0x8765,
    currentVertexAttrib = 0x8626,
    front = 0x0404,
    back = 0x0405,
    frontAndBack = 0x0408,
    texture2d = 0x0DE1,
    cullFace = 0x0B44,
    blend = 0x0BE2,
    dither = 0x0BD0,
    stencilTest = 0x0B90,
    depthTest = 0x0B71,
    scissorTest = 0x0C11,
    polygonOffsetFill = 0x8037,
    sampleAlphaToCoverage = 0x809E,
    sampleCoverage = 0x80A0,
    noError = 0,
    invalidEnum = 0x0500,
    invalidValue = 0x0501,
    invalidOperation = 0x0502,
    outOfMemory = 0x0505,
    cw = 0x0900,
    ccw = 0x0901,
    lineWidth = 0x0B21,
    aliasedPointSizeRange = 0x846D,
    aliasedLineWidthRange = 0x846E,
    cullFaceMode = 0x0B45,
    frontFace = 0x0B46,
    depthRange = 0x0B70,
    depthWritemask = 0x0B72,
    depthClearValue = 0x0B73,
    depthFunc = 0x0B74,
    stencilClearValue = 0x0B91,
    stencilFunc = 0x0B92,
    stencilFail = 0x0B94,
    stencilPassDepthFail = 0x0B95,
    stencilPassDepthPass = 0x0B96,
    stencilRef = 0x0B97,
    stencilValueMask = 0x0B93,
    stencilWritemask = 0x0B98,
    stencilBackFunc = 0x8800,
    stencilBackFail = 0x8801,
    stencilBackPassDepthFail = 0x8802,
    stencilBackPassDepthPass = 0x8803,
    stencilBackRef = 0x8CA3,
    stencilBackValueMask = 0x8CA4,
    stencilBackWritemask = 0x8CA5,
    viewport = 0x0BA2,
    scissorBox = 0x0C10,
    colorClearValue = 0x0C22,
    colorWritemask = 0x0C23,
    unpackAlignment = 0x0CF5,
    packAlignment = 0x0D05,
    maxTextureSize = 0x0D33,
    maxTextureUnits = 0x84E2,
    maxViewportDims = 0x0D3A,
    subpixelBits = 0x0D50,
    redBits = 0x0D52,
    greenBits = 0x0D53,
    blueBits = 0x0D54,
    alphaBits = 0x0D55,
    depthBits = 0x0D56,
    stencilBits = 0x0D57,
    polygonOffsetUnits = 0x2A00,
    polygonOffsetFactor = 0x8038,
    textureBinding2d = 0x8069,
    sampleBuffers = 0x80A8,
    samples = 0x80A9,
    sampleCoverageValue = 0x80AA,
    sampleCoverageInvert = 0x80AB,
    numCompressedTextureFormats = 0x86A2,
    compressedTextureFormats = 0x86A3,
    dontCare = 0x1100,
    fastest = 0x1101,
    nicest = 0x1102,
    generateMipmap = 0x8191,
    generateMipmapHint = 0x8192,
    byteV = 0x1400,
    unsignedByte = 0x1401,
    shortV = 0x1402,
    unsignedShort = 0x1403,
    intV = 0x1404,
    unsignedInt = 0x1405,
    floatV = 0x1406,
    fixed = 0x140C,
    depthComponent = 0x1902,
    alpha = 0x1906,
    rgb = 0x1907,
    rgba = 0x1908,
    luminance = 0x1909,
    luminanceAlpha = 0x190A,
    unsignedShort4444 = 0x8033,
    unsignedShort5551 = 0x8034,
    unsignedShort565 = 0x8363,
    fragmentShader = 0x8B30,
    vertexShader = 0x8B31,
    maxVertexAttribs = 0x8869,
    maxVertexUniformVectors = 0x8DFB,
    maxVaryingVectors = 0x8DFC,
    maxCombinedTextureImageUnits = 0x8B4D,
    maxVertexTextureImageUnits = 0x8B4C,
    maxTextureImageUnits = 0x8872,
    maxFragmentUniformVectors = 0x8DFD,
    shaderType = 0x8B4F,
    deleteStatus = 0x8B80,
    linkStatus = 0x8B82,
    validateStatus = 0x8B83,
    attachedShaders = 0x8B85,
    activeUniforms = 0x8B86,
    activeUniformMaxLength = 0x8B87,
    activeAttributes = 0x8B89,
    activeAttributeMaxLength = 0x8B8A,
    shadingLanguageVersion = 0x8B8C,
    currentProgram = 0x8B8D,
    never = 0x0200,
    less = 0x0201,
    equal = 0x0202,
    lequal = 0x0203,
    greater = 0x0204,
    notequal = 0x0205,
    gequal = 0x0206,
    always = 0x0207,
    keep = 0x1E00,
    replace = 0x1E01,
    incr = 0x1E02,
    decr = 0x1E03,
    invert = 0x150A,
    incrWrap = 0x8507,
    decrWrap = 0x8508,
    vendor = 0x1F00,
    renderer = 0x1F01,
    version = 0x1F02,
    extensions = 0x1F03,
    nearest = 0x2600,
    linear = 0x2601,
    nearestMipmapNearest = 0x2700,
    linearMipmapNearest = 0x2701,
    nearestMipmapLinear = 0x2702,
    linearMipmapLinear = 0x2703,
    textureMagFilter = 0x2800,
    textureMinFilter = 0x2801,
    textureWrapS = 0x2802,
    textureWrapT = 0x2803,
    texture = 0x1702,
    textureCubeMap = 0x8513,
    textureBindingCubeMap = 0x8514,
    textureCubeMapPositiveX = 0x8515,
    textureCubeMapNegativeX = 0x8516,
    textureCubeMapPositiveY = 0x8517,
    textureCubeMapNegativeY = 0x8518,
    textureCubeMapPositiveZ = 0x8519,
    textureCubeMapNegativeZ = 0x851A,
    maxCubeMapTextureSize = 0x851C,
    texture0 = 0x84C0,
    texture1 = 0x84C1,
    texture2 = 0x84C2,
    texture3 = 0x84C3,
    texture4 = 0x84C4,
    texture5 = 0x84C5,
    texture6 = 0x84C6,
    texture7 = 0x84C7,
    texture8 = 0x84C8,
    texture9 = 0x84C9,
    texture10 = 0x84CA,
    texture11 = 0x84CB,
    texture12 = 0x84CC,
    texture13 = 0x84CD,
    texture14 = 0x84CE,
    texture15 = 0x84CF,
    texture16 = 0x84D0,
    texture17 = 0x84D1,
    texture18 = 0x84D2,
    texture19 = 0x84D3,
    texture20 = 0x84D4,
    texture21 = 0x84D5,
    texture22 = 0x84D6,
    texture23 = 0x84D7,
    texture24 = 0x84D8,
    texture25 = 0x84D9,
    texture26 = 0x84DA,
    texture27 = 0x84DB,
    texture28 = 0x84DC,
    texture29 = 0x84DD,
    texture30 = 0x84DE,
    texture31 = 0x84DF,
    activeTexture = 0x84E0,
    repeat = 0x2901,
    clampToEdge = 0x812F,
    mirroredRepeat = 0x8370,
    floatVec2 = 0x8B50,
    floatVec3 = 0x8B51,
    floatVec4 = 0x8B52,
    intVec2 = 0x8B53,
    intVec3 = 0x8B54,
    intVec4 = 0x8B55,
    bool = 0x8B56,
    boolVec2 = 0x8B57,
    boolVec3 = 0x8B58,
    boolVec4 = 0x8B59,
    floatMat2 = 0x8B5A,
    floatMat3 = 0x8B5B,
    floatMat4 = 0x8B5C,
    sampler2d = 0x8B5E,
    samplerCube = 0x8B60,
    vertexAttribArrayEnabled = 0x8622,
    vertexAttribArraySize = 0x8623,
    vertexAttribArrayStride = 0x8624,
    vertexAttribArrayType = 0x8625,
    vertexAttribArrayNormalized = 0x886A,
    vertexAttribArrayPointer = 0x8645,
    vertexAttribArrayBufferBinding = 0x889F,
    implementationColorReadType = 0x8B9A,
    implementationColorReadFormat = 0x8B9B,
    compileStatus = 0x8B81,
    infoLogLength = 0x8B84,
    shaderSourceLength = 0x8B88,
    shaderCompiler = 0x8DFA,
    shaderBinaryFormats = 0x8DF8,
    numShaderBinaryFormats = 0x8DF9,
    lowFloat = 0x8DF0,
    mediumFloat = 0x8DF1,
    highFloat = 0x8DF2,
    lowInt = 0x8DF3,
    mediumInt = 0x8DF4,
    highInt = 0x8DF5,
    framebuffer = 0x8D40,
    renderbuffer = 0x8D41,
    rgba4 = 0x8056,
    rgb5A1 = 0x8057,
    rgb565 = 0x8D62,
    depthComponent16 = 0x81A5,
    stencilIndex = 0x1901,
    stencilIndex8 = 0x8D48,
    renderbufferWidth = 0x8D42,
    renderbufferHeight = 0x8D43,
    renderbufferInternalFormat = 0x8D44,
    renderbufferRedSize = 0x8D50,
    renderbufferGreenSize = 0x8D51,
    renderbufferBlueSize = 0x8D52,
    renderbufferAlphaSize = 0x8D53,
    renderbufferDepthSize = 0x8D54,
    renderbufferStencilSize = 0x8D55,
    framebufferAttachmentObjectType = 0x8CD0,
    framebufferAttachmentObjectName = 0x8CD1,
    framebufferAttachmentTextureLevel = 0x8CD2,
    framebufferAttachmentTextureCubeMapFace = 0x8CD3,
    colorAttachment0 = 0x8CE0,
    depthAttachment = 0x8D00,
    stencilAttachment = 0x8D20,
    none = 0,
    framebufferComplete = 0x8CD5,
    framebufferIncompleteAttachment = 0x8CD6,
    framebufferIncompleteMissingAttachment = 0x8CD7,
    framebufferIncompleteDimensions = 0x8CD9,
    framebufferUnsupported = 0x8CDD,
    framebufferBinding = 0x8CA6,
    renderbufferBinding = 0x8CA7,
    maxRenderbufferSize = 0x84E8,
    invalidFramebufferOperation = 0x0506,
    programPointSize = 0x8642, //same as GL_PROGRAM_POINT_SIZE_ARB

    //GL3 constants
    readBuffer = 0x0C02,
    unpackRowLength = 0x0CF2,
    unpackSkipRows = 0x0CF3,
    unpackSkipPixels = 0x0CF4,
    packRowLength = 0x0D02,
    packSkipRows = 0x0D03,
    packSkipPixels = 0x0D04,
    color = 0x1800,
    depth = 0x1801,
    stencil = 0x1802,
    red = 0x1903,
    rgb8 = 0x8051,
    rgba8 = 0x8058,
    rgb10A2 = 0x8059,
    textureBinding3d = 0x806A,
    unpackSkipImages = 0x806D,
    unpackImageHeight = 0x806E,
    texture3d = 0x806F,
    textureWrapR = 0x8072,
    max3dTextureSize = 0x8073,
    unsignedInt2101010Rev = 0x8368,
    maxElementsVertices = 0x80E8,
    maxElementsIndices = 0x80E9,
    textureMinLod = 0x813A,
    textureMaxLod = 0x813B,
    textureBaseLevel = 0x813C,
    textureMaxLevel = 0x813D,
    depthComponent24 = 0x81A6,
    maxTextureLodBias = 0x84FD,
    textureCompareMode = 0x884C,
    textureCompareFunc = 0x884D,
    currentQuery = 0x8865,
    queryResult = 0x8866,
    queryResultAvailable = 0x8867,
    bufferMapped = 0x88BC,
    bufferMapPointer = 0x88BD,
    streamRead = 0x88E1,
    streamCopy = 0x88E2,
    staticRead = 0x88E5,
    staticCopy = 0x88E6,
    dynamicRead = 0x88E9,
    dynamicCopy = 0x88EA,
    maxDrawBuffers = 0x8824,
    drawBuffer0 = 0x8825,
    drawBuffer1 = 0x8826,
    drawBuffer2 = 0x8827,
    drawBuffer3 = 0x8828,
    drawBuffer4 = 0x8829,
    drawBuffer5 = 0x882A,
    drawBuffer6 = 0x882B,
    drawBuffer7 = 0x882C,
    drawBuffer8 = 0x882D,
    drawBuffer9 = 0x882E,
    drawBuffer10 = 0x882F,
    drawBuffer11 = 0x8830,
    drawBuffer12 = 0x8831,
    drawBuffer13 = 0x8832,
    drawBuffer14 = 0x8833,
    drawBuffer15 = 0x8834,
    maxFragmentUniformComponents = 0x8B49,
    maxVertexUniformComponents = 0x8B4A,
    sampler3d = 0x8B5F,
    sampler2dShadow = 0x8B62,
    fragmentShaderDerivativeHint = 0x8B8B,
    pixelPackBuffer = 0x88EB,
    pixelUnpackBuffer = 0x88EC,
    pixelPackBufferBinding = 0x88ED,
    pixelUnpackBufferBinding = 0x88EF,
    floatMat2x3 = 0x8B65,
    floatMat2x4 = 0x8B66,
    floatMat3x2 = 0x8B67,
    floatMat3x4 = 0x8B68,
    floatMat4x2 = 0x8B69,
    floatMat4x3 = 0x8B6A,
    srgb = 0x8C40,
    srgb8 = 0x8C41,
    srgb8Alpha8 = 0x8C43,
    compareRefToTexture = 0x884E,
    majorVersion = 0x821B,
    minorVersion = 0x821C,
    numExtensions = 0x821D,
    rgba32f = 0x8814,
    rgb32f = 0x8815,
    rgba16f = 0x881A,
    rgb16f = 0x881B,
    vertexAttribArrayInteger = 0x88FD,
    maxArrayTextureLayers = 0x88FF,
    minProgramTexelOffset = 0x8904,
    maxProgramTexelOffset = 0x8905,
    maxVaryingComponents = 0x8B4B,
    texture2dArray = 0x8C1A,
    textureBinding2dArray = 0x8C1D,
    r11fG11fB10f = 0x8C3A,
    unsignedInt10f11f11fRev = 0x8C3B,
    rgb9E5 = 0x8C3D,
    unsignedInt5999Rev = 0x8C3E,
    transformFeedbackVaryingMaxLength = 0x8C76,
    transformFeedbackBufferMode = 0x8C7F,
    maxTransformFeedbackSeparateComponents = 0x8C80,
    transformFeedbackVaryings = 0x8C83,
    transformFeedbackBufferStart = 0x8C84,
    transformFeedbackBufferSize = 0x8C85,
    transformFeedbackPrimitivesWritten = 0x8C88,
    rasterizerDiscard = 0x8C89,
    maxTransformFeedbackInterleavedComponents = 0x8C8A,
    maxTransformFeedbackSeparateAttribs = 0x8C8B,
    interleavedAttribs = 0x8C8C,
    separateAttribs = 0x8C8D,
    transformFeedbackBuffer = 0x8C8E,
    transformFeedbackBufferBinding = 0x8C8F,
    rgba32ui = 0x8D70,
    rgb32ui = 0x8D71,
    rgba16ui = 0x8D76,
    rgb16ui = 0x8D77,
    rgba8ui = 0x8D7C,
    rgb8ui = 0x8D7D,
    rgba32i = 0x8D82,
    rgb32i = 0x8D83,
    rgba16i = 0x8D88,
    rgb16i = 0x8D89,
    rgba8i = 0x8D8E,
    rgb8i = 0x8D8F,
    redInteger = 0x8D94,
    rgbInteger = 0x8D98,
    rgbaInteger = 0x8D99,
    sampler2dArray = 0x8DC1,
    sampler2dArrayShadow = 0x8DC4,
    samplerCubeShadow = 0x8DC5,
    unsignedIntVec2 = 0x8DC6,
    unsignedIntVec3 = 0x8DC7,
    unsignedIntVec4 = 0x8DC8,
    intSampler2d = 0x8DCA,
    intSampler3d = 0x8DCB,
    intSamplerCube = 0x8DCC,
    intSampler2dArray = 0x8DCF,
    unsignedIntSampler2d = 0x8DD2,
    unsignedIntSampler3d = 0x8DD3,
    unsignedIntSamplerCube = 0x8DD4,
    unsignedIntSampler2dArray = 0x8DD7,
    bufferAccessFlags = 0x911F,
    bufferMapLength = 0x9120,
    bufferMapOffset = 0x9121,
    depthComponent32f = 0x8CAC,
    depth32fStencil8 = 0x8CAD,
    float32UnsignedInt248Rev = 0x8DAD,
    framebufferAttachmentColorEncoding = 0x8210,
    framebufferAttachmentComponentType = 0x8211,
    framebufferAttachmentRedSize = 0x8212,
    framebufferAttachmentGreenSize = 0x8213,
    framebufferAttachmentBlueSize = 0x8214,
    framebufferAttachmentAlphaSize = 0x8215,
    framebufferAttachmentDepthSize = 0x8216,
    framebufferAttachmentStencilSize = 0x8217,
    framebufferDefault = 0x8218,
    framebufferUndefined = 0x8219,
    depthStencilAttachment = 0x821A,
    depthStencil = 0x84F9,
    unsignedInt248 = 0x84FA,
    depth24Stencil8 = 0x88F0,
    unsignedNormalized = 0x8C17,
    drawFramebufferBinding = framebufferBinding,
    readFramebuffer = 0x8CA8,
    drawFramebuffer = 0x8CA9,
    readFramebufferBinding = 0x8CAA,
    renderbufferSamples = 0x8CAB,
    framebufferAttachmentTextureLayer = 0x8CD4,
    maxColorAttachments = 0x8CDF,
    colorAttachment1 = 0x8CE1,
    colorAttachment2 = 0x8CE2,
    colorAttachment3 = 0x8CE3,
    colorAttachment4 = 0x8CE4,
    colorAttachment5 = 0x8CE5,
    colorAttachment6 = 0x8CE6,
    colorAttachment7 = 0x8CE7,
    colorAttachment8 = 0x8CE8,
    colorAttachment9 = 0x8CE9,
    colorAttachment10 = 0x8CEA,
    colorAttachment11 = 0x8CEB,
    colorAttachment12 = 0x8CEC,
    colorAttachment13 = 0x8CED,
    colorAttachment14 = 0x8CEE,
    colorAttachment15 = 0x8CEF,
    framebufferIncompleteMultisample = 0x8D56,
    maxSamples = 0x8D57,
    halfFloat = 0x140B,
    mapReadBit = 0x0001,
    mapWriteBit = 0x0002,
    mapInvalidateRangeBit = 0x0004,
    mapInvalidateBufferBit = 0x0008,
    mapFlushExplicitBit = 0x0010,
    mapUnsynchronizedBit = 0x0020,
    rg = 0x8227,
    rgInteger = 0x8228,
    r8 = 0x8229,
    rg8 = 0x822B,
    r16f = 0x822D,
    r32f = 0x822E,
    rg16f = 0x822F,
    rg32f = 0x8230,
    r8i = 0x8231,
    r8ui = 0x8232,
    r16i = 0x8233,
    r16ui = 0x8234,
    r32i = 0x8235,
    r32ui = 0x8236,
    rg8i = 0x8237,
    rg8ui = 0x8238,
    rg16i = 0x8239,
    rg16ui = 0x823A,
    rg32i = 0x823B,
    rg32ui = 0x823C,
    vertexArrayBinding = 0x85B5,
    r8Snorm = 0x8F94,
    rg8Snorm = 0x8F95,
    rgb8Snorm = 0x8F96,
    rgba8Snorm = 0x8F97,
    signedNormalized = 0x8F9C,
    primitiveRestartFixedIndex = 0x8D69,
    copyReadBuffer = 0x8F36,
    copyWriteBuffer = 0x8F37,
    copyReadBufferBinding = copyReadBuffer,
    copyWriteBufferBinding = copyWriteBuffer,
    uniformBuffer = 0x8A11,
    uniformBufferBinding = 0x8A28,
    uniformBufferStart = 0x8A29,
    uniformBufferSize = 0x8A2A,
    maxVertexUniformBlocks = 0x8A2B,
    maxFragmentUniformBlocks = 0x8A2D,
    maxCombinedUniformBlocks = 0x8A2E,
    maxUniformBufferBindings = 0x8A2F,
    maxUniformBlockSize = 0x8A30,
    maxCombinedVertexUniformComponents = 0x8A31,
    maxCombinedFragmentUniformComponents = 0x8A33,
    uniformBufferOffsetAlignment = 0x8A34,
    activeUniformBlockMaxNameLength = 0x8A35,
    activeUniformBlocks = 0x8A36,
    uniformType = 0x8A37,
    uniformSize = 0x8A38,
    uniformNameLength = 0x8A39,
    uniformBlockIndex = 0x8A3A,
    uniformOffset = 0x8A3B,
    uniformArrayStride = 0x8A3C,
    uniformMatrixStride = 0x8A3D,
    uniformIsRowMajor = 0x8A3E,
    uniformBlockBinding = 0x8A3F,
    uniformBlockDataSize = 0x8A40,
    uniformBlockNameLength = 0x8A41,
    uniformBlockActiveUniforms = 0x8A42,
    uniformBlockActiveUniformIndices = 0x8A43,
    uniformBlockReferencedByVertexShader = 0x8A44,
    uniformBlockReferencedByFragmentShader = 0x8A46,
    invalidIndex = -1,
    maxVertexOutputComponents = 0x9122,
    maxFragmentInputComponents = 0x9125,
    maxServerWaitTimeout = 0x9111,
    objectType = 0x9112,
    syncCondition = 0x9113,
    syncStatus = 0x9114,
    syncFlags = 0x9115,
    syncFence = 0x9116,
    syncGpuCommandsComplete = 0x9117,
    unsignaled = 0x9118,
    signaled = 0x9119,
    alreadySignaled = 0x911A,
    timeoutExpired = 0x911B,
    conditionSatisfied = 0x911C,
    waitFailed = 0x911D,
    syncFlushCommandsBit = 0x00000001,
    vertexAttribArrayDivisor = 0x88FE,
    anySamplesPassed = 0x8C2F,
    anySamplesPassedConservative = 0x8D6A,
    samplerBinding = 0x8919,
    rgb10A2ui = 0x906F,
    textureSwizzleR = 0x8E42,
    textureSwizzleG = 0x8E43,
    textureSwizzleB = 0x8E44,
    textureSwizzleA = 0x8E45,
    green = 0x1904,
    blue = 0x1905,
    int2101010Rev = 0x8D9F,
    transformFeedback = 0x8E22,
    transformFeedbackPaused = 0x8E23,
    transformFeedbackActive = 0x8E24,
    transformFeedbackBinding = 0x8E25,
    programBinaryRetrievableHint = 0x8257,
    programBinaryLength = 0x8741,
    numProgramBinaryFormats = 0x87FE,
    programBinaryFormats = 0x87FF,
    compressedR11Eac = 0x9270,
    compressedSignedR11Eac = 0x9271,
    compressedRg11Eac = 0x9272,
    compressedSignedRg11Eac = 0x9273,
    compressedRgb8Etc2 = 0x9274,
    compressedSrgb8Etc2 = 0x9275,
    compressedRgb8PunchthroughAlpha1Etc2 = 0x9276,
    compressedSrgb8PunchthroughAlpha1Etc2 = 0x9277,
    compressedRgba8Etc2Eac = 0x9278,
    compressedSrgb8Alpha8Etc2Eac = 0x9279,
    textureImmutableFormat = 0x912F,
    maxElementIndex = 0x8D6B,
    numSampleCounts = 0x9380,
    textureImmutableLevels = 0x82DF;

    public static final long timeoutIgnored = -1;

    //STATE - optimizes GL calls

    private static IntBuffer ibuf = Buffers.newIntBuffer(1);
    private static FloatBuffer fbuf = Buffers.newFloatBuffer(1);
    //last active texture unit
    private static int lastActiveTexture = -1;
    //last bound texture2ds, mapping from texture unit to texture handle
    private static int[] lastBoundTextures = new int[32], lastBoundTexturesArray = new int[32];
    //last useProgram call
    private static int lastUsedProgram = -1;
    /** enabled bits, from glEnable/disable */
    private static Bits enabled = new Bits();
    private static boolean wasDepthMask = true;
    //blend func separate state
    private static int lastBlendSrc = -1, lastBlendDst = -1, lastBlendSrcAlpha = -1, lastBlendDstAlpha = -1;

    static{
        reset();
    }

    /** Reset optimization cache. */
    public static void reset(){
        lastActiveTexture = -1;
        Arrays.fill(lastBoundTextures, -1);
        Arrays.fill(lastBoundTexturesArray, -1);
        lastUsedProgram = -1;
        enabled.clear();
        wasDepthMask = true;
    }

    public static void activeTexture(int texture){
        if(optimize && lastActiveTexture == texture) return;

        Core.glProvider.glActiveTexture(texture);
        lastActiveTexture = texture;
    }

    public static void bindTexture(int target, int texture){
        //TODO optimize 3d array
        if(optimize && target == texture2d){
            //current bound texture unit
            int index = lastActiveTexture - texture0;
            //make sure it's valid
            if(index >= 0 && index < lastBoundTextures.length){
                if(lastBoundTextures[index] == texture){
                    //skip double bindings
                    return;
                }
                lastBoundTextures[index] = texture;
            }
        }else if(optimize && target == Gl.texture2dArray){
            //current bound texture unit
            int index = lastActiveTexture - texture0;
            //make sure it's valid
            if(index >= 0 && index < lastBoundTexturesArray.length){
                if(lastBoundTexturesArray[index] == texture){
                    //skip double bindings
                    return;
                }
                lastBoundTexturesArray[index] = texture;
            }
        }

        Core.glProvider.glBindTexture(target, texture);
    }

    public static void blendFunc(int sfactor, int dfactor){
        if(optimize && lastBlendSrc == sfactor && lastBlendDst == dfactor && lastBlendSrcAlpha == sfactor && lastBlendDstAlpha == dfactor) return;

        Core.glProvider.glBlendFunc(lastBlendSrc = lastBlendSrcAlpha = sfactor, lastBlendDst = lastBlendDstAlpha = dfactor);
    }

    public static void clear(int mask){
        Core.glProvider.glClear(mask);
    }

    public static void clearColor(float red, float green, float blue, float alpha){
        Core.glProvider.glClearColor(red, green, blue, alpha);
    }

    public static void clearDepthf(float depth){
        Core.glProvider.glClearDepthf(depth);
    }

    public static void clearStencil(int s){
        Core.glProvider.glClearStencil(s);
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha){
        Core.glProvider.glColorMask(red, green, blue, alpha);
    }

    public static void compressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data){
        Core.glProvider.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
    }

    public static void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer data){
        Core.glProvider.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data);
    }

    public static void copyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border){
        Core.glProvider.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    public static void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height){
        Core.glProvider.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    public static void cullFace(int mode){
        Core.glProvider.glCullFace(mode);
    }

    public static void deleteTexture(int texture){
        //clear deleted texture, as it may be reused later
        for(int i = 0; i < lastBoundTextures.length; i++){
            if(lastBoundTextures[i] == texture){
                lastBoundTextures[i] = -1;
            }
        }
        for(int i = 0; i < lastBoundTexturesArray.length; i++){
            if(lastBoundTexturesArray[i] == texture){
                lastBoundTexturesArray[i] = -1;
            }
        }
        Core.glProvider.glDeleteTexture(texture);
    }

    public static void depthFunc(int func){
        Core.glProvider.glDepthFunc(func);
    }

    public static void depthMask(boolean flag){
        //TODO might be buggy, test it. may not clear the depth buffer?
        if(optimize && flag == wasDepthMask) return;
        wasDepthMask = flag;

        Core.glProvider.glDepthMask(flag);
    }

    public static void depthRangef(float zNear, float zFar){
        Core.glProvider.glDepthRangef(zNear, zFar);
    }

    public static void disable(int cap){
        if(optimize && !enabled.get(cap)){
            return;
        }
        Core.glProvider.glDisable(cap);
        enabled.clear(cap);
    }

    public static void drawArrays(int mode, int first, int count){
        Core.glProvider.glDrawArrays(mode, first, count);
    }

    public static void drawElements(int mode, int count, int type, Buffer indices){
        Core.glProvider.glDrawElements(mode, count, type, indices);
    }

    public static void enable(int cap){
        if(optimize && enabled.get(cap)){
            return;
        }
        Core.glProvider.glEnable(cap);
        enabled.set(cap);
    }

    public static void finish(){
        Core.glProvider.glFinish();
    }

    public static void flush(){
        Core.glProvider.glFlush();
    }

    public static void frontFace(int mode){
        Core.glProvider.glFrontFace(mode);
    }

    public static int genTexture(){
        return Core.glProvider.glGenTexture();
    }

    public static int getError(){
        return Core.glProvider.glGetError();
    }

    public static void getIntegerv(int pname, IntBuffer params){
        Core.glProvider.glGetIntegerv(pname, params);
    }

    public static int getInt(int name){
        ibuf.position(0);
        getIntegerv(name, ibuf);
        return ibuf.get(0);
    }

    public static float getFloat(int name){
        fbuf.position(0);
        getFloatv(name, fbuf);
        return fbuf.get(0);
    }

    public static String getString(int name){
        return Core.glProvider.glGetString(name);
    }

    public static void hint(int target, int mode){
        Core.glProvider.glHint(target, mode);
    }

    public static void pixelStorei(int pname, int param){
        Core.glProvider.glPixelStorei(pname, param);
    }

    public static void polygonOffset(float factor, float units){
        Core.glProvider.glPolygonOffset(factor, units);
    }

    public static void readPixels(int x, int y, int width, int height, int format, int type, Buffer pixels){
        Core.glProvider.glReadPixels(x, y, width, height, format, type, pixels);
    }

    public static void scissor(int x, int y, int width, int height){
        Core.glProvider.glScissor(x, y, width, height);
    }

    public static void stencilFunc(int func, int ref, int mask){
        Core.glProvider.glStencilFunc(func, ref, mask);
    }

    public static void stencilMask(int mask){
        Core.glProvider.glStencilMask(mask);
    }

    public static void stencilOp(int fail, int zfail, int zpass){
        Core.glProvider.glStencilOp(fail, zfail, zpass);
    }

    public static void texImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels){
        Core.glProvider.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public static void texParameterf(int target, int pname, float param){
        Core.glProvider.glTexParameterf(target, pname, param);
    }

    public static void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels){
        Core.glProvider.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public static void viewport(int x, int y, int width, int height){
        Core.glProvider.glViewport(x, y, width, height);
    }

    public static void attachShader(int program, int shader){
        Core.glProvider.glAttachShader(program, shader);
    }

    public static void bindAttribLocation(int program, int index, String name){
        Core.glProvider.glBindAttribLocation(program, index, name);
    }

    public static void bindBuffer(int target, int buffer){
        Core.glProvider.glBindBuffer(target, buffer);
    }

    public static void bindFramebuffer(int target, int framebuffer){
        Core.glProvider.glBindFramebuffer(target, framebuffer);
    }

    public static void bindRenderbuffer(int target, int renderbuffer){
        Core.glProvider.glBindRenderbuffer(target, renderbuffer);
    }

    public static void blendColor(float red, float green, float blue, float alpha){
        Core.glProvider.glBlendColor(red, green, blue, alpha);
    }

    public static void blendEquation(int mode){
        Core.glProvider.glBlendEquation(mode);
    }

    public static void blendEquationSeparate(int modeRGB, int modeAlpha){
        Core.glProvider.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha){
        if(optimize && srcRGB == lastBlendSrc && dstRGB == lastBlendDst && srcAlpha == lastBlendSrcAlpha && dstAlpha == lastBlendDstAlpha){
            return;
        }

        Core.glProvider.glBlendFuncSeparate(lastBlendSrc = srcRGB, lastBlendDst = dstRGB, lastBlendSrcAlpha = srcAlpha, lastBlendDstAlpha = dstAlpha);
    }

    public static void bufferData(int target, int size, Buffer data, int usage){
        Core.glProvider.glBufferData(target, size, data, usage);
    }

    public static void bufferSubData(int target, int offset, int size, Buffer data){
        Core.glProvider.glBufferSubData(target, offset, size, data);
    }

    public static int checkFramebufferStatus(int target){
        return Core.glProvider.glCheckFramebufferStatus(target);
    }

    public static void compileShader(int shader){
        Core.glProvider.glCompileShader(shader);
    }

    public static int createProgram(){
        return Core.glProvider.glCreateProgram();
    }

    public static int createShader(int type){
        return Core.glProvider.glCreateShader(type);
    }

    public static void deleteBuffer(int buffer){
        Core.glProvider.glDeleteBuffer(buffer);
    }

    public static void deleteFramebuffer(int framebuffer){
        Core.glProvider.glDeleteFramebuffer(framebuffer);
    }

    public static void deleteProgram(int program){
        if(lastUsedProgram == program) lastUsedProgram = -1;
        Core.glProvider.glDeleteProgram(program);
    }

    public static void deleteRenderbuffer(int renderbuffer){
        Core.glProvider.glDeleteRenderbuffer(renderbuffer);
    }

    public static void deleteShader(int shader){
        Core.glProvider.glDeleteShader(shader);
    }

    public static void detachShader(int program, int shader){
        Core.glProvider.glDetachShader(program, shader);
    }

    public static void disableVertexAttribArray(int index){
        Core.glProvider.glDisableVertexAttribArray(index);
    }

    public static void drawElements(int mode, int count, int type, int indices){
        Core.glProvider.glDrawElements(mode, count, type, indices);
    }

    public static void enableVertexAttribArray(int index){
        Core.glProvider.glEnableVertexAttribArray(index);
    }

    public static void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer){
        Core.glProvider.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    public static void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level){
        Core.glProvider.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public static int genBuffer(){
        return Core.glProvider.glGenBuffer();
    }

    public static void generateMipmap(int target){
        Core.glProvider.glGenerateMipmap(target);
    }

    public static int genFramebuffer(){
        return Core.glProvider.glGenFramebuffer();
    }

    public static int genRenderbuffer(){
        return Core.glProvider.glGenRenderbuffer();
    }

    public static String getActiveAttrib(int program, int index, IntBuffer size, IntBuffer type){
        return Core.glProvider.glGetActiveAttrib(program, index, size, type);
    }

    public static String getActiveUniform(int program, int index, IntBuffer size, IntBuffer type){
        return Core.glProvider.glGetActiveUniform(program, index, size, type);
    }

    public static int getAttribLocation(int program, String name){
        return Core.glProvider.glGetAttribLocation(program, name);
    }

    public static void getBooleanv(int pname, Buffer params){
        Core.glProvider.glGetBooleanv(pname, params);
    }

    public static void getBufferParameteriv(int target, int pname, IntBuffer params){
        Core.glProvider.glGetBufferParameteriv(target, pname, params);
    }

    public static void getFloatv(int pname, FloatBuffer params){
        Core.glProvider.glGetFloatv(pname, params);
    }

    public static void getFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params){
        Core.glProvider.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
    }

    public static void getProgramiv(int program, int pname, IntBuffer params){
        Core.glProvider.glGetProgramiv(program, pname, params);
    }

    public static String getProgramInfoLog(int program){
        return Core.glProvider.glGetProgramInfoLog(program);
    }

    public static void getRenderbufferParameteriv(int target, int pname, IntBuffer params){
        Core.glProvider.glGetRenderbufferParameteriv(target, pname, params);
    }

    public static void getShaderiv(int shader, int pname, IntBuffer params){
        Core.glProvider.glGetShaderiv(shader, pname, params);
    }

    public static String getShaderInfoLog(int shader){
        return Core.glProvider.glGetShaderInfoLog(shader);
    }

    public static void getShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision){
        Core.glProvider.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision);
    }

    public static void getTexParameterfv(int target, int pname, FloatBuffer params){
        Core.glProvider.glGetTexParameterfv(target, pname, params);
    }

    public static void getTexParameteriv(int target, int pname, IntBuffer params){
        Core.glProvider.glGetTexParameteriv(target, pname, params);
    }

    public static void getUniformfv(int program, int location, FloatBuffer params){
        Core.glProvider.glGetUniformfv(program, location, params);
    }

    public static void getUniformiv(int program, int location, IntBuffer params){
        Core.glProvider.glGetUniformiv(program, location, params);
    }

    public static int getUniformLocation(int program, String name){
        return Core.glProvider.glGetUniformLocation(program, name);
    }

    public static void getVertexAttribfv(int index, int pname, FloatBuffer params){
        Core.glProvider.glGetVertexAttribfv(index, pname, params);
    }

    public static void getVertexAttribiv(int index, int pname, IntBuffer params){
        Core.glProvider.glGetVertexAttribiv(index, pname, params);
    }

    public static boolean isBuffer(int buffer){
        return Core.glProvider.glIsBuffer(buffer);
    }

    public static boolean isEnabled(int cap){
        return Core.glProvider.glIsEnabled(cap);
    }

    public static boolean isFramebuffer(int framebuffer){
        return Core.glProvider.glIsFramebuffer(framebuffer);
    }

    public static boolean isProgram(int program){
        return Core.glProvider.glIsProgram(program);
    }

    public static boolean isRenderbuffer(int renderbuffer){
        return Core.glProvider.glIsRenderbuffer(renderbuffer);
    }

    public static boolean isShader(int shader){
        return Core.glProvider.glIsShader(shader);
    }

    public static boolean isTexture(int texture){
        return Core.glProvider.glIsTexture(texture);
    }

    public static void linkProgram(int program){
        Core.glProvider.glLinkProgram(program);
    }

    public static void releaseShaderCompiler(){
        Core.glProvider.glReleaseShaderCompiler();
    }

    public static void renderbufferStorage(int target, int internalformat, int width, int height){
        Core.glProvider.glRenderbufferStorage(target, internalformat, width, height);
    }

    public static void sampleCoverage(float value, boolean invert){
        Core.glProvider.glSampleCoverage(value, invert);
    }

    public static void shaderSource(int shader, String string){
        Core.glProvider.glShaderSource(shader, string);
    }

    public static void stencilFuncSeparate(int face, int func, int ref, int mask){
        Core.glProvider.glStencilFuncSeparate(face, func, ref, mask);
    }

    public static void stencilMaskSeparate(int face, int mask){
        Core.glProvider.glStencilMaskSeparate(face, mask);
    }

    public static void stencilOpSeparate(int face, int fail, int zfail, int zpass){
        Core.glProvider.glStencilOpSeparate(face, fail, zfail, zpass);
    }

    public static void texParameterfv(int target, int pname, FloatBuffer params){
        Core.glProvider.glTexParameterfv(target, pname, params);
    }

    public static void texParameteri(int target, int pname, int param){
        Core.glProvider.glTexParameteri(target, pname, param);
    }

    public static void texParameteriv(int target, int pname, IntBuffer params){
        Core.glProvider.glTexParameteriv(target, pname, params);
    }

    public static void uniform1f(int location, float x){
        Core.glProvider.glUniform1f(location, x);
    }

    public static void uniform1fv(int location, int count, FloatBuffer v){
        Core.glProvider.glUniform1fv(location, count, v);
    }

    public static void uniform1fv(int location, int count, float[] v, int offset){
        Core.glProvider.glUniform1fv(location, count, v, offset);
    }

    public static void uniform1i(int location, int x){
        Core.glProvider.glUniform1i(location, x);
    }

    public static void uniform1iv(int location, int count, IntBuffer v){
        Core.glProvider.glUniform1iv(location, count, v);
    }

    public static void uniform1iv(int location, int count, int[] v, int offset){
        Core.glProvider.glUniform1iv(location, count, v, offset);
    }

    public static void uniform2f(int location, float x, float y){
        Core.glProvider.glUniform2f(location, x, y);
    }

    public static void uniform2fv(int location, int count, FloatBuffer v){
        Core.glProvider.glUniform2fv(location, count, v);
    }

    public static void uniform2fv(int location, int count, float[] v, int offset){
        Core.glProvider.glUniform2fv(location, count, v, offset);
    }

    public static void uniform2i(int location, int x, int y){
        Core.glProvider.glUniform2i(location, x, y);
    }

    public static void uniform2iv(int location, int count, IntBuffer v){
        Core.glProvider.glUniform2iv(location, count, v);
    }

    public static void uniform2iv(int location, int count, int[] v, int offset){
        Core.glProvider.glUniform2iv(location, count, v, offset);
    }

    public static void uniform3f(int location, float x, float y, float z){
        Core.glProvider.glUniform3f(location, x, y, z);
    }

    public static void uniform3fv(int location, int count, FloatBuffer v){
        Core.glProvider.glUniform3fv(location, count, v);
    }

    public static void uniform3fv(int location, int count, float[] v, int offset){
        Core.glProvider.glUniform3fv(location, count, v, offset);
    }

    public static void uniform3i(int location, int x, int y, int z){
        Core.glProvider.glUniform3i(location, x, y, z);
    }

    public static void uniform3iv(int location, int count, IntBuffer v){
        Core.glProvider.glUniform3iv(location, count, v);
    }

    public static void uniform3iv(int location, int count, int[] v, int offset){
        Core.glProvider.glUniform3iv(location, count, v, offset);
    }

    public static void uniform4f(int location, float x, float y, float z, float w){
        Core.glProvider.glUniform4f(location, x, y, z, w);
    }

    public static void uniform4fv(int location, int count, FloatBuffer v){
        Core.glProvider.glUniform4fv(location, count, v);
    }

    public static void uniform4fv(int location, int count, float[] v, int offset){
        Core.glProvider.glUniform4fv(location, count, v, offset);
    }

    public static void uniform4i(int location, int x, int y, int z, int w){
        Core.glProvider.glUniform4i(location, x, y, z, w);
    }

    public static void uniform4iv(int location, int count, IntBuffer v){
        Core.glProvider.glUniform4iv(location, count, v);
    }

    public static void uniform4iv(int location, int count, int[] v, int offset){
        Core.glProvider.glUniform4iv(location, count, v, offset);
    }

    public static void uniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value){
        Core.glProvider.glUniformMatrix2fv(location, count, transpose, value);
    }

    public static void uniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset){
        Core.glProvider.glUniformMatrix2fv(location, count, transpose, value, offset);
    }

    public static void uniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value){
        Core.glProvider.glUniformMatrix3fv(location, count, transpose, value);
    }

    public static void uniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset){
        Core.glProvider.glUniformMatrix3fv(location, count, transpose, value, offset);
    }

    public static void uniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value){
        Core.glProvider.glUniformMatrix4fv(location, count, transpose, value);
    }

    public static void uniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset){
        Core.glProvider.glUniformMatrix4fv(location, count, transpose, value, offset);
    }

    public static void useProgram(int program){
        if(optimize && lastUsedProgram == program){
            return;
        }
        Core.glProvider.glUseProgram(program);
        lastUsedProgram = program;
    }

    public static void validateProgram(int program){
        Core.glProvider.glValidateProgram(program);
    }

    public static void vertexAttrib1f(int indx, float x){
        Core.glProvider.glVertexAttrib1f(indx, x);
    }

    public static void vertexAttrib1fv(int indx, FloatBuffer values){
        Core.glProvider.glVertexAttrib1fv(indx, values);
    }

    public static void vertexAttrib2f(int indx, float x, float y){
        Core.glProvider.glVertexAttrib2f(indx, x, y);
    }

    public static void vertexAttrib2fv(int indx, FloatBuffer values){
        Core.glProvider.glVertexAttrib2fv(indx, values);
    }

    public static void vertexAttrib3f(int indx, float x, float y, float z){
        Core.glProvider.glVertexAttrib3f(indx, x, y, z);
    }

    public static void vertexAttrib3fv(int indx, FloatBuffer values){
        Core.glProvider.glVertexAttrib3fv(indx, values);
    }

    public static void vertexAttrib4f(int indx, float x, float y, float z, float w){
        Core.glProvider.glVertexAttrib4f(indx, x, y, z, w);
    }

    public static void vertexAttrib4fv(int indx, FloatBuffer values){
        Core.glProvider.glVertexAttrib4fv(indx, values);
    }

    public static void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr){
        Core.glProvider.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }

    //GL30 methods below

    public static void readBuffer(int mode){
        Core.glProvider.glReadBuffer(mode);
    }

    public static void drawRangeElements(int mode, int start, int end, int count, int type, java.nio.Buffer indices){
        Core.glProvider.glDrawRangeElements(mode, start, end, count, type, indices);
    }

    public static void drawRangeElements(int mode, int start, int end, int count, int type, int offset){
        Core.glProvider.glDrawRangeElements(mode, start, end, count, type, offset);
    }

    public static void texImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, java.nio.Buffer pixels){
        Core.glProvider.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }

    public static void texImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, int offset){
        Core.glProvider.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, offset);
    }

    public static void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, java.nio.Buffer pixels){
        Core.glProvider.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels);
    }

    public static void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, int offset){
        Core.glProvider.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, offset);
    }

    public static void copyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height){
        Core.glProvider.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
    }

    public static void genQueries(int n, java.nio.IntBuffer ids){
        Core.glProvider.glGenQueries(n, ids);
    }

    public static void deleteQueries(int n, java.nio.IntBuffer ids){
        Core.glProvider.glDeleteQueries(n, ids);
    }

    public static boolean isQuery(int id){
        return Core.glProvider.glIsQuery(id);
    }

    public static void beginQuery(int target, int id){
        Core.glProvider.glBeginQuery(target, id);
    }

    public static void endQuery(int target){
        Core.glProvider.glEndQuery(target);
    }

    public static void getQueryiv(int target, int pname, java.nio.IntBuffer params){
        Core.glProvider.glGetQueryiv(target, pname, params);
    }

    public static void getQueryObjectuiv(int id, int pname, java.nio.IntBuffer params){
        Core.glProvider.glGetQueryObjectuiv(id, pname, params);
    }

    public static boolean unmapBuffer(int target){
        return Core.glProvider.glUnmapBuffer(target);
    }

    public static java.nio.Buffer getBufferPointerv(int target, int pname){
        return Core.glProvider.glGetBufferPointerv(target, pname);
    }

    public static void drawBuffers(int n, java.nio.IntBuffer bufs){
        Core.glProvider.glDrawBuffers(n, bufs);
    }

    public static void uniformMatrix2x3fv(int location, int count, boolean transpose, java.nio.FloatBuffer value){
        Core.glProvider.glUniformMatrix2x3fv(location, count, transpose, value);
    }

    public static void uniformMatrix3x2fv(int location, int count, boolean transpose, java.nio.FloatBuffer value){
        Core.glProvider.glUniformMatrix3x2fv(location, count, transpose, value);
    }

    public static void uniformMatrix2x4fv(int location, int count, boolean transpose, java.nio.FloatBuffer value){
        Core.glProvider.glUniformMatrix2x4fv(location, count, transpose, value);
    }

    public static void uniformMatrix4x2fv(int location, int count, boolean transpose, java.nio.FloatBuffer value){
        Core.glProvider.glUniformMatrix4x2fv(location, count, transpose, value);
    }

    public static void uniformMatrix3x4fv(int location, int count, boolean transpose, java.nio.FloatBuffer value){
        Core.glProvider.glUniformMatrix3x4fv(location, count, transpose, value);
    }

    public static void uniformMatrix4x3fv(int location, int count, boolean transpose, java.nio.FloatBuffer value){
        Core.glProvider.glUniformMatrix4x3fv(location, count, transpose, value);
    }

    public static void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter){
        Core.glProvider.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    public static void renderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height){
        Core.glProvider.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    public static void framebufferTextureLayer(int target, int attachment, int texture, int level, int layer){
        Core.glProvider.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    public static void flushMappedBufferRange(int target, int offset, int length){
        Core.glProvider.glFlushMappedBufferRange(target, offset, length);
    }

    public static void bindVertexArray(int array){
        Core.glProvider.glBindVertexArray(array);
    }

    public static void deleteVertexArrays(int n, java.nio.IntBuffer arrays){
        Core.glProvider.glDeleteVertexArrays(n, arrays);
    }

    public static void genVertexArrays(int n, java.nio.IntBuffer arrays){
        Core.glProvider.glGenVertexArrays(n, arrays);
    }

    public static boolean isVertexArray(int array){
        return Core.glProvider.glIsVertexArray(array);
    }

    public static void beginTransformFeedback(int primitiveMode){
        Core.glProvider.glBeginTransformFeedback(primitiveMode);
    }

    public static void endTransformFeedback(){
        Core.glProvider.glEndTransformFeedback();
    }

    public static void bindBufferRange(int target, int index, int buffer, int offset, int size){
        Core.glProvider.glBindBufferRange(target, index, buffer, offset, size);
    }

    public static void bindBufferBase(int target, int index, int buffer){
        Core.glProvider.glBindBufferBase(target, index, buffer);
    }

    public static void transformFeedbackVaryings(int program, String[] varyings, int bufferMode){
        Core.glProvider.glTransformFeedbackVaryings(program, varyings, bufferMode);
    }

    public static void vertexAttribIPointer(int index, int size, int type, int stride, int offset){
        Core.glProvider.glVertexAttribIPointer(index, size, type, stride, offset);
    }

    public static void getVertexAttribIiv(int index, int pname, java.nio.IntBuffer params){
        Core.glProvider.glGetVertexAttribIiv(index, pname, params);
    }

    public static void getVertexAttribIuiv(int index, int pname, java.nio.IntBuffer params){
        Core.glProvider.glGetVertexAttribIuiv(index, pname, params);
    }

    public static void vertexAttribI4i(int index, int x, int y, int z, int w){
        Core.glProvider.glVertexAttribI4i(index, x, y, z, w);
    }

    public static void vertexAttribI4ui(int index, int x, int y, int z, int w){
        Core.glProvider.glVertexAttribI4ui(index, x, y, z, w);
    }

    public static void getUniformuiv(int program, int location, java.nio.IntBuffer params){
        Core.glProvider.glGetUniformuiv(program, location, params);
    }

    public static int getFragDataLocation(int program, String name){
        return Core.glProvider.glGetFragDataLocation(program, name);
    }

    public static void uniform1uiv(int location, int count, java.nio.IntBuffer value){
        Core.glProvider.glUniform1uiv(location, count, value);
    }

    public static void uniform3uiv(int location, int count, java.nio.IntBuffer value){
        Core.glProvider.glUniform3uiv(location, count, value);
    }

    public static void uniform4uiv(int location, int count, java.nio.IntBuffer value){
        Core.glProvider.glUniform4uiv(location, count, value);
    }

    public static void clearBufferiv(int buffer, int drawbuffer, java.nio.IntBuffer value){
        Core.glProvider.glClearBufferiv(buffer, drawbuffer, value);
    }

    public static void clearBufferuiv(int buffer, int drawbuffer, java.nio.IntBuffer value){
        Core.glProvider.glClearBufferuiv(buffer, drawbuffer, value);
    }

    public static void clearBufferfv(int buffer, int drawbuffer, java.nio.FloatBuffer value){
        Core.glProvider.glClearBufferfv(buffer, drawbuffer, value);
    }

    public static void clearBufferfi(int buffer, int drawbuffer, float depth, int stencil){
        Core.glProvider.glClearBufferfi(buffer, drawbuffer, depth, stencil);
    }

    public static String getStringi(int name, int index){
        return Core.glProvider.glGetStringi(name, index);
    }

    public static void copyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size){
        Core.glProvider.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    public static void getUniformIndices(int program, String[] uniformNames, java.nio.IntBuffer uniformIndices){
        Core.glProvider.glGetUniformIndices(program, uniformNames, uniformIndices);
    }

    public static void getActiveUniformsiv(int program, int uniformCount, java.nio.IntBuffer uniformIndices, int pname, java.nio.IntBuffer params){
        Core.glProvider.glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params);
    }

    public static int getUniformBlockIndex(int program, String uniformBlockName){
        return Core.glProvider.glGetUniformBlockIndex(program, uniformBlockName);
    }

    public static void getActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, java.nio.IntBuffer params){
        Core.glProvider.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
    }

    public static void getActiveUniformBlockName(int program, int uniformBlockIndex, java.nio.Buffer length, java.nio.Buffer uniformBlockName){
        Core.glProvider.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName);
    }

    public static void uniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding){
        Core.glProvider.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    public static void drawArraysInstanced(int mode, int first, int count, int instanceCount){
        Core.glProvider.glDrawArraysInstanced(mode, first, count, instanceCount);
    }

    public static void drawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount){
        Core.glProvider.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount);
    }

    public static void getInteger64v(int pname, java.nio.LongBuffer params){
        Core.glProvider.glGetInteger64v(pname, params);
    }

    public static void getBufferParameteri64v(int target, int pname, java.nio.LongBuffer params){
        Core.glProvider.glGetBufferParameteri64v(target, pname, params);
    }

    public static void genSamplers(int count, java.nio.IntBuffer samplers){
        Core.glProvider.glGenSamplers(count, samplers);
    }

    public static void deleteSamplers(int count, java.nio.IntBuffer samplers){
        Core.glProvider.glDeleteSamplers(count, samplers);
    }

    public static boolean isSampler(int sampler){
        return Core.glProvider.glIsSampler(sampler);
    }

    public static void bindSampler(int unit, int sampler){
        Core.glProvider.glBindSampler(unit, sampler);
    }

    public static void samplerParameteri(int sampler, int pname, int param){
        Core.glProvider.glSamplerParameteri(sampler, pname, param);
    }

    public static void samplerParameteriv(int sampler, int pname, java.nio.IntBuffer param){
        Core.glProvider.glSamplerParameteriv(sampler, pname, param);
    }

    public static void samplerParameterf(int sampler, int pname, float param){
        Core.glProvider.glSamplerParameterf(sampler, pname, param);
    }

    public static void samplerParameterfv(int sampler, int pname, java.nio.FloatBuffer param){
        Core.glProvider.glSamplerParameterfv(sampler, pname, param);
    }

    public static void getSamplerParameteriv(int sampler, int pname, java.nio.IntBuffer params){
        Core.glProvider.glGetSamplerParameteriv(sampler, pname, params);
    }

    public static void getSamplerParameterfv(int sampler, int pname, java.nio.FloatBuffer params){
        Core.glProvider.glGetSamplerParameterfv(sampler, pname, params);
    }

    public static void vertexAttribDivisor(int index, int divisor){
        Core.glProvider.glVertexAttribDivisor(index, divisor);
    }

    public static void bindTransformFeedback(int target, int id){
        Core.glProvider.glBindTransformFeedback(target, id);
    }

    public static void deleteTransformFeedbacks(int n, java.nio.IntBuffer ids){
        Core.glProvider.glDeleteTransformFeedbacks(n, ids);
    }

    public static void genTransformFeedbacks(int n, java.nio.IntBuffer ids){
        Core.glProvider.glGenTransformFeedbacks(n, ids);
    }

    public static boolean isTransformFeedback(int id){
        return Core.glProvider.glIsTransformFeedback(id);
    }

    public static void pauseTransformFeedback(){
        Core.glProvider.glPauseTransformFeedback();
    }

    public static void resumeTransformFeedback(){
        Core.glProvider.glResumeTransformFeedback();
    }

    public static void programParameteri(int program, int pname, int value){
        Core.glProvider.glProgramParameteri(program, pname, value);
    }

    public static void invalidateFramebuffer(int target, int numAttachments, java.nio.IntBuffer attachments){
        Core.glProvider.glInvalidateFramebuffer(target, numAttachments, attachments);
    }

    public static void invalidateSubFramebuffer(int target, int numAttachments, java.nio.IntBuffer attachments, int x, int y, int width, int height){
        Core.glProvider.glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height);
    }
}
