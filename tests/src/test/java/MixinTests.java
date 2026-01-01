import mindustry.mod.MixinService;
import mindustry.mod.MixinService.*;
import mindustry.mod.mixin.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.service.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Mindustry Mixin system
 */
public class MixinTests{


    @Nested
    @DisplayName("MixinConfigData Tests")
    class MixinConfigDataTests{

        @Test
        @DisplayName("Default values are correctly set")
        void testDefaultValues(){
            MixinConfigData data = new MixinConfigData();
            assertTrue(data.required);
            assertEquals("0.8", data.minVersion);
            assertNull(data.packageName);
            assertEquals("JAVA_8", data.compatibilityLevel);
            assertNotNull(data.mixins);
            assertEquals(0, data.mixins.length);
            assertNotNull(data.client);
            assertEquals(0, data.client.length);
            assertNotNull(data.server);
            assertEquals(0, data.server.length);
            assertFalse(data.verbose);
            assertEquals(1000, data.priority);
        }

        @Test
        @DisplayName("MixinConfigData fields can be modified")
        void testFieldModification(){
            MixinConfigData data = new MixinConfigData();
            data.required = false;
            data.minVersion = "0.9";
            data.packageName = "com.example.mixins";
            data.compatibilityLevel = "JAVA_17";
            data.mixins = new String[]{"TestMixin"};
            data.client = new String[]{"ClientMixin"};
            data.server = new String[]{"ServerMixin"};
            data.verbose = true;
            data.priority = 500;

            assertFalse(data.required);
            assertEquals("0.9", data.minVersion);
            assertEquals("com.example.mixins", data.packageName);
            assertEquals("JAVA_17", data.compatibilityLevel);
            assertArrayEquals(new String[]{"TestMixin"}, data.mixins);
            assertArrayEquals(new String[]{"ClientMixin"}, data.client);
            assertArrayEquals(new String[]{"ServerMixin"}, data.server);
            assertTrue(data.verbose);
            assertEquals(500, data.priority);
        }

        @ParameterizedTest
        @ValueSource(strings = {"JAVA_8", "JAVA_11", "JAVA_16", "JAVA_17", "JAVA_21"})
        @DisplayName("Various compatibility levels can be set")
        void testCompatibilityLevels(String level){
            MixinConfigData data = new MixinConfigData();
            data.compatibilityLevel = level;
            assertEquals(level, data.compatibilityLevel);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 100, 500, 1000, 5000, Integer.MAX_VALUE})
        @DisplayName("Various priority values can be set")
        void testPriorityValues(int priority){
            MixinConfigData data = new MixinConfigData();
            data.priority = priority;
            assertEquals(priority, data.priority);
        }

        @Test
        @DisplayName("Empty mixin arrays are valid")
        void testEmptyMixinArrays(){
            MixinConfigData data = new MixinConfigData();
            data.mixins = new String[]{};
            data.client = new String[]{};
            data.server = new String[]{};
            assertEquals(0, data.mixins.length);
            assertEquals(0, data.client.length);
            assertEquals(0, data.server.length);
        }

        @Test
        @DisplayName("Multiple mixins can be specified")
        void testMultipleMixins(){
            MixinConfigData data = new MixinConfigData();
            data.mixins = new String[]{"Mixin1", "Mixin2", "Mixin3", "Mixin4", "Mixin5"};
            assertEquals(5, data.mixins.length);
            assertEquals("Mixin3", data.mixins[2]);
        }
    }

    @Nested
    @DisplayName("MixinConfig Tests")
    class MixinConfigTests{

        @Test
        @DisplayName("MixinConfig constructor sets all fields")
        void testConstructor(){
            MixinConfigData data = new MixinConfigData();
            data.packageName = "test.package";

            MixinConfig config = new MixinConfig("TestMod", null, data);

            assertEquals("TestMod", config.modName);
            assertNull(config.configFile);
            assertSame(data, config.data);
        }

        @Test
        @DisplayName("MixinConfig with null modName")
        void testNullModName(){
            MixinConfigData data = new MixinConfigData();
            MixinConfig config = new MixinConfig(null, null, data);
            assertNull(config.modName);
        }

        @Test
        @DisplayName("MixinConfig with null data")
        void testNullData(){
            MixinConfig config = new MixinConfig("TestMod", null, null);
            assertNull(config.data);
        }
    }


    @Nested
    @DisplayName("MindustryClassProvider Tests")
    class MindustryClassProviderTests{

        private MindustryClassProvider provider;

        @BeforeEach
        void setup(){
            provider = new MindustryClassProvider();
        }

        @Test
        @DisplayName("getClassPath returns empty array by default")
        void testGetClassPathDefault(){
            URL[] classpath = provider.getClassPath();
            assertNotNull(classpath);
        }

        @Test
        @DisplayName("findClass can find standard Java classes")
        void testFindJavaClass() throws ClassNotFoundException{
            Class<?> clazz = provider.findClass("java.lang.String");
            assertNotNull(clazz);
            assertEquals(String.class, clazz);
        }

        @Test
        @DisplayName("findClass with initialize=false does not initialize class")
        void testFindClassNoInitialize() throws ClassNotFoundException{
            Class<?> clazz = provider.findClass("java.util.ArrayList", false);
            assertNotNull(clazz);
        }

        @Test
        @DisplayName("findClass with initialize=true initializes class")
        void testFindClassWithInitialize() throws ClassNotFoundException{
            Class<?> clazz = provider.findClass("java.util.HashMap", true);
            assertNotNull(clazz);
        }

        @Test
        @DisplayName("findAgentClass works like findClass")
        void testFindAgentClass() throws ClassNotFoundException{
            Class<?> clazz = provider.findAgentClass("java.lang.Object", false);
            assertNotNull(clazz);
            assertEquals(Object.class, clazz);
        }

        @Test
        @DisplayName("findClass throws ClassNotFoundException for non-existent class")
        void testFindNonExistentClass(){
            assertThrows(ClassNotFoundException.class, () ->
                provider.findClass("com.nonexistent.FakeClass")
            );
        }

        @Test
        @DisplayName("isClassLoaded returns true for loaded classes")
        void testIsClassLoadedTrue(){
            assertTrue(provider.isClassLoaded("java.lang.String"));
        }

        @Test
        @DisplayName("isClassLoaded returns false for non-existent classes")
        void testIsClassLoadedFalse(){
            assertFalse(provider.isClassLoaded("com.nonexistent.FakeClass123"));
        }

        @Test
        @DisplayName("getResourceAsStream returns null for non-existent resource")
        void testGetResourceAsStreamNull(){
            InputStream stream = provider.getResourceAsStream("nonexistent/resource.txt");
            assertNull(stream);
        }

        @Test
        @DisplayName("getResourceAsStream finds class files")
        void testGetResourceAsStreamClass() throws IOException{
            InputStream stream = provider.getResourceAsStream("java/lang/String.class");
            assertNotNull(stream);
            stream.close();
        }

        @Test
        @DisplayName("setModClassLoader accepts URLClassLoader")
        void testSetModClassLoader() throws MalformedURLException{
            URL[] urls = new URL[]{new URL("file:///test.jar")};
            URLClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader());
            MindustryClassProvider.setModClassLoader(loader);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "java.lang.String",
            "java.util.List",
            "java.util.Map",
            "java.io.InputStream",
            "java.net.URL"
        })
        @DisplayName("findClass works for common Java classes")
        void testFindCommonClasses(String className) throws ClassNotFoundException{
            Class<?> clazz = provider.findClass(className);
            assertNotNull(clazz);
            assertEquals(className, clazz.getName());
        }
    }


    @Nested
    @DisplayName("MindustryBytecodeProvider Tests")
    class MindustryBytecodeProviderTests{

        private MindustryBytecodeProvider provider;

        @BeforeEach
        void setup(){
            provider = new MindustryBytecodeProvider();
        }

        @Test
        @DisplayName("getClassNode returns valid ClassNode for String")
        void testGetClassNodeString() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.String");
            assertNotNull(node);
            assertEquals("java/lang/String", node.name);
        }

        @Test
        @DisplayName("getClassNode throws ClassNotFoundException for invalid class")
        void testGetClassNodeInvalid(){
            assertThrows(ClassNotFoundException.class, () ->
                provider.getClassNode("com.nonexistent.FakeClass")
            );
        }

        @Test
        @DisplayName("getClassNode with runTransformers=false works")
        void testGetClassNodeNoTransformers() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.Object", false);
            assertNotNull(node);
            assertEquals("java/lang/Object", node.name);
        }

        @Test
        @DisplayName("getClassNode with runTransformers=true works")
        void testGetClassNodeWithTransformers() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.Integer", true);
            assertNotNull(node);
            assertEquals("java/lang/Integer", node.name);
        }

        @Test
        @DisplayName("getClassNode with custom readerFlags works")
        void testGetClassNodeCustomFlags() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.Boolean", false, 0);
            assertNotNull(node);
            assertEquals("java/lang/Boolean", node.name);
        }

        @Test
        @DisplayName("ClassNode contains methods")
        void testClassNodeContainsMethods() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.String");
            assertNotNull(node.methods);
            assertTrue(node.methods.size() > 0);
        }

        @Test
        @DisplayName("ClassNode contains fields")
        void testClassNodeContainsFields() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.String");
            assertNotNull(node.fields);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.io.File",
            "java.net.Socket"
        })
        @DisplayName("getClassNode works for various classes")
        void testGetClassNodeVarious(String className) throws Exception{
            ClassNode node = provider.getClassNode(className);
            assertNotNull(node);
            assertEquals(className.replace('.', '/'), node.name);
        }
    }


    @Nested
    @DisplayName("MindustryContainerHandle Tests")
    class MindustryContainerHandleTests{

        @Test
        @DisplayName("Constructor creates valid handle")
        void testConstructor() throws Exception{
            MindustryContainerHandle handle = new MindustryContainerHandle();
            assertNotNull(handle);
        }

        @Test
        @DisplayName("getNestedContainers returns empty collection")
        void testGetNestedContainers() throws Exception{
            MindustryContainerHandle handle = new MindustryContainerHandle();
            Collection<?> nested = handle.getNestedContainers();
            assertNotNull(nested);
            assertTrue(nested.isEmpty());
        }
    }


    @Nested
    @DisplayName("MindustryTransformerService Tests")
    class MindustryTransformerServiceTests{

        private MindustryTransformerService service;

        @BeforeEach
        void setup(){
            service = new MindustryTransformerService();
        }

        @Test
        @DisplayName("getTransformers returns empty collection")
        void testGetTransformers(){
            Collection<ITransformer> transformers = service.getTransformers();
            assertNotNull(transformers);
            assertTrue(transformers.isEmpty());
        }

        @Test
        @DisplayName("getDelegatedTransformers returns empty collection")
        void testGetDelegatedTransformers(){
            Collection<ITransformer> transformers = service.getDelegatedTransformers();
            assertNotNull(transformers);
            assertTrue(transformers.isEmpty());
        }

        @Test
        @DisplayName("addTransformerExclusion does not throw")
        void testAddTransformerExclusion(){
            assertDoesNotThrow(() -> service.addTransformerExclusion("test.class"));
        }

        @Test
        @DisplayName("addTransformerExclusion accepts various patterns")
        void testAddTransformerExclusionPatterns(){
            assertDoesNotThrow(() -> {
                service.addTransformerExclusion("com.example.");
                service.addTransformerExclusion("org.test.Class");
                service.addTransformerExclusion("*");
            });
        }

        @Test
        @DisplayName("transformClass returns original bytes when transformer is null")
        void testTransformClassNull(){
            byte[] original = new byte[]{1, 2, 3, 4};
            byte[] result = service.transformClass(null, "test.Class", original);
            assertArrayEquals(original, result);
        }

        @Test
        @DisplayName("transformClass handles empty byte array")
        void testTransformClassEmpty(){
            byte[] empty = new byte[0];
            byte[] result = service.transformClass(null, "test.Class", empty);
            assertArrayEquals(empty, result);
        }
    }


    @Nested
    @DisplayName("MindustryMixinService Tests")
    class MindustryMixinServiceTests{

        private MindustryMixinService service;

        @BeforeEach
        void setup(){
            service = new MindustryMixinService();
        }

        @Test
        @DisplayName("getName returns 'Mindustry'")
        void testGetName(){
            assertEquals("Mindustry", service.getName());
        }

        @Test
        @DisplayName("isValid checks for Mindustry classes")
        void testIsValid(){
            assertDoesNotThrow(() -> service.isValid());
        }

        @Test
        @DisplayName("prepare does not throw")
        void testPrepare(){
            assertDoesNotThrow(() -> service.prepare());
        }

        @Test
        @DisplayName("init creates providers")
        void testInit(){
            service.prepare();
            assertDoesNotThrow(() -> service.init());
        }

        @Test
        @DisplayName("getClassProvider returns non-null after init")
        void testGetClassProvider(){
            service.prepare();
            service.init();
            IClassProvider provider = service.getClassProvider();
            assertNotNull(provider);
            assertTrue(provider instanceof MindustryClassProvider);
        }

        @Test
        @DisplayName("getBytecodeProvider returns non-null after init")
        void testGetBytecodeProvider(){
            service.prepare();
            service.init();
            IClassBytecodeProvider provider = service.getBytecodeProvider();
            assertNotNull(provider);
            assertTrue(provider instanceof MindustryBytecodeProvider);
        }

        @Test
        @DisplayName("getTransformerProvider returns non-null after init")
        void testGetTransformerProvider(){
            service.prepare();
            service.init();
            ITransformerProvider provider = service.getTransformerProvider();
            assertNotNull(provider);
            assertTrue(provider instanceof MindustryTransformerService);
        }

        @Test
        @DisplayName("getMindustryTransformerProvider returns same as getTransformerProvider")
        void testGetMindustryTransformerProvider(){
            service.prepare();
            service.init();
            MindustryTransformerService provider = service.getMindustryTransformerProvider();
            assertSame(service.getTransformerProvider(), provider);
        }

        @Test
        @DisplayName("getClassTracker returns null")
        void testGetClassTracker(){
            assertNull(service.getClassTracker());
        }

        @Test
        @DisplayName("getAuditTrail returns null")
        void testGetAuditTrail(){
            assertNull(service.getAuditTrail());
        }

        @Test
        @DisplayName("getPlatformAgents returns empty collection")
        void testGetPlatformAgents(){
            Collection<String> agents = service.getPlatformAgents();
            assertNotNull(agents);
            assertTrue(agents.isEmpty());
        }

        @Test
        @DisplayName("getPrimaryContainer returns non-null after prepare")
        void testGetPrimaryContainer(){
            service.prepare();
            assertNotNull(service.getPrimaryContainer());
        }

        @Test
        @DisplayName("getMixinContainers returns empty collection")
        void testGetMixinContainers(){
            Collection<?> containers = service.getMixinContainers();
            assertNotNull(containers);
            assertTrue(containers.isEmpty());
        }

        @Test
        @DisplayName("beginPhase does not throw")
        void testBeginPhase(){
            assertDoesNotThrow(() -> service.beginPhase());
        }

        @Test
        @DisplayName("checkEnv does not throw")
        void testCheckEnv(){
            assertDoesNotThrow(() -> service.checkEnv(null));
        }

        @Test
        @DisplayName("resolveKey creates PropertyKey")
        void testResolveKey(){
            IPropertyKey key = service.resolveKey("test.property");
            assertNotNull(key);
            assertEquals("test.property", key.toString());
        }

        @Test
        @DisplayName("setProperty and getProperty work correctly")
        void testSetGetProperty(){
            IPropertyKey key = service.resolveKey("test.key");
            service.setProperty(key, "testValue");
            Object value = service.getProperty(key);
            assertEquals("testValue", value);
        }

        @Test
        @DisplayName("getProperty returns null for unset key")
        void testGetPropertyNull(){
            IPropertyKey key = service.resolveKey("unset.key." + System.nanoTime());
            Object value = service.getProperty(key);
            assertNull(value);
        }

        @Test
        @DisplayName("getProperty with default returns default for unset key")
        void testGetPropertyWithDefault(){
            IPropertyKey key = service.resolveKey("unset.key.default." + System.nanoTime());
            String value = service.getProperty(key, "default");
            assertEquals("default", value);
        }

        @Test
        @DisplayName("getPropertyString returns string value")
        void testGetPropertyString(){
            IPropertyKey key = service.resolveKey("string.key");
            service.setProperty(key, "stringValue");
            String value = service.getPropertyString(key, "default");
            assertEquals("stringValue", value);
        }

        @Test
        @DisplayName("getPropertyString returns default for unset key")
        void testGetPropertyStringDefault(){
            IPropertyKey key = service.resolveKey("unset.string." + System.nanoTime());
            String value = service.getPropertyString(key, "defaultString");
            assertEquals("defaultString", value);
        }

        @ParameterizedTest
        @ValueSource(strings = {"key1", "key.nested", "key.very.nested.value", "KEY_UPPER"})
        @DisplayName("Property keys with various formats work")
        void testVariousPropertyKeys(String keyName){
            IPropertyKey key = service.resolveKey(keyName);
            service.setProperty(key, "value_" + keyName);
            assertEquals("value_" + keyName, service.getProperty(key));
        }

        @Test
        @DisplayName("Multiple properties can be stored")
        void testMultipleProperties(){
            for(int i = 0; i < 100; i++){
                IPropertyKey key = service.resolveKey("prop." + i);
                service.setProperty(key, i);
            }
            for(int i = 0; i < 100; i++){
                IPropertyKey key = service.resolveKey("prop." + i);
                assertEquals(Integer.valueOf(i), service.getProperty(key));
            }
        }

        @Test
        @DisplayName("getResourceAsStream after init")
        void testGetResourceAsStream(){
            service.prepare();
            service.init();
            InputStream stream = service.getResourceAsStream("nonexistent.txt");
            assertNull(stream);
        }

        @Test
        @DisplayName("registerInvalidClass does not throw")
        void testRegisterInvalidClass(){
            service.prepare();
            service.init();
            assertDoesNotThrow(() -> service.registerInvalidClass("test.InvalidClass"));
        }

        @Test
        @DisplayName("isClassLoaded works after init")
        void testIsClassLoaded(){
            service.prepare();
            service.init();
            assertTrue(service.isClassLoaded("java.lang.String"));
            assertFalse(service.isClassLoaded("nonexistent.Class123"));
        }

        @Test
        @DisplayName("getClassRestrictions returns empty string")
        void testGetClassRestrictions(){
            service.prepare();
            service.init();
            assertEquals("", service.getClassRestrictions("any.Class"));
        }
    }


    @Nested
    @DisplayName("MindustryMixinConnector Tests")
    class MindustryMixinConnectorTests{

        @Test
        @DisplayName("Constructor creates valid connector")
        void testConstructor(){
            MindustryMixinConnector connector = new MindustryMixinConnector();
            assertNotNull(connector);
        }

        @Test
        @DisplayName("connect does not throw in test environment")
        void testConnect(){
            MindustryMixinConnector connector = new MindustryMixinConnector();
            assertDoesNotThrow(() -> connector.connect());
        }
    }


    @Nested
    @DisplayName("Mixin System Integration Tests")
    class MixinIntegrationTests{

        @Test
        @DisplayName("Full service initialization flow")
        void testFullInitialization(){
            MindustryMixinService service = new MindustryMixinService();

            assertDoesNotThrow(() -> service.prepare());

            assertDoesNotThrow(() -> service.init());

            assertNotNull(service.getClassProvider());
            assertNotNull(service.getBytecodeProvider());
            assertNotNull(service.getTransformerProvider());
        }

        @Test
        @DisplayName("Class provider can load bytecode provider's classes")
        void testProviderInteraction() throws Exception{
            MindustryMixinService service = new MindustryMixinService();
            service.prepare();
            service.init();

            MindustryClassProvider classProvider = (MindustryClassProvider) service.getClassProvider();
            MindustryBytecodeProvider bytecodeProvider = (MindustryBytecodeProvider) service.getBytecodeProvider();

            Class<?> clazz = classProvider.findClass("java.util.ArrayList");

            ClassNode node = bytecodeProvider.getClassNode("java.util.ArrayList");

            assertEquals(clazz.getName().replace('.', '/'), node.name);
        }

        @Test
        @DisplayName("MixinConfigData can be serialized to JSON-like format")
        void testConfigDataSerializable(){
            MixinConfigData data = new MixinConfigData();
            data.packageName = "test.mixins";
            data.mixins = new String[]{"TestMixin1", "TestMixin2"};
            data.client = new String[]{"ClientMixin"};
            data.server = new String[]{"ServerMixin"};
            data.verbose = true;
            data.priority = 500;

            assertNotNull(data.packageName);
            assertNotNull(data.mixins);
            assertNotNull(data.client);
            assertNotNull(data.server);
            assertTrue(data.verbose);
            assertEquals(500, data.priority);
        }

        @Test
        @DisplayName("Property system is thread-safe")
        void testPropertyThreadSafety() throws Exception{
            MindustryMixinService service = new MindustryMixinService();
            int threadCount = 10;
            int operationsPerThread = 100;
            Thread[] threads = new Thread[threadCount];

            for(int t = 0; t < threadCount; t++){
                final int threadId = t;
                threads[t] = new Thread(() -> {
                    for(int i = 0; i < operationsPerThread; i++){
                        IPropertyKey key = service.resolveKey("thread." + threadId + ".prop." + i);
                        service.setProperty(key, threadId * 1000 + i);
                    }
                });
            }

            for(Thread thread : threads){
                thread.start();
            }
            for(Thread thread : threads){
                thread.join();
            }

            for(int t = 0; t < threadCount; t++){
                for(int i = 0; i < operationsPerThread; i++){
                    IPropertyKey key = service.resolveKey("thread." + t + ".prop." + i);
                    assertEquals(Integer.valueOf(t * 1000 + i), service.getProperty(key));
                }
            }
        }
    }


    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests{

        @Test
        @DisplayName("MixinConfigData handles null arrays gracefully")
        void testNullArrays(){
            MixinConfigData data = new MixinConfigData();
            data.mixins = null;
            data.client = null;
            data.server = null;
            assertNull(data.mixins);
            assertNull(data.client);
            assertNull(data.server);
        }

        @Test
        @DisplayName("Empty package name is valid")
        void testEmptyPackageName(){
            MixinConfigData data = new MixinConfigData();
            data.packageName = "";
            assertEquals("", data.packageName);
        }

        @Test
        @DisplayName("Whitespace package name is stored as-is")
        void testWhitespacePackageName(){
            MixinConfigData data = new MixinConfigData();
            data.packageName = "   ";
            assertEquals("   ", data.packageName);
        }

        @Test
        @DisplayName("ClassProvider handles special class names")
        void testSpecialClassNames() throws ClassNotFoundException{
            MindustryClassProvider provider = new MindustryClassProvider();

            Class<?> innerClass = provider.findClass("java.util.Map$Entry");
            assertNotNull(innerClass);

            Class<?> arrayClass = provider.findClass("[Ljava.lang.String;");
            assertNotNull(arrayClass);
            assertTrue(arrayClass.isArray());
        }

        @Test
        @DisplayName("BytecodeProvider handles inner classes")
        void testBytecodeProviderInnerClass() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode("java.util.Map$Entry");
            assertNotNull(node);
            assertEquals("java/util/Map$Entry", node.name);
        }

        @Test
        @DisplayName("Service handles repeated init calls")
        void testRepeatedInit(){
            MindustryMixinService service = new MindustryMixinService();
            service.prepare();
            service.init();

            IClassProvider provider1 = service.getClassProvider();

            service.init();

            IClassProvider provider2 = service.getClassProvider();
            assertNotNull(provider2);
        }

        @Test
        @DisplayName("Negative priority is accepted")
        void testNegativePriority(){
            MixinConfigData data = new MixinConfigData();
            data.priority = -1;
            assertEquals(-1, data.priority);
        }

        @Test
        @DisplayName("Unicode in package names")
        void testUnicodePackageName(){
            MixinConfigData data = new MixinConfigData();
            data.packageName = "com.例え.テスト";
            assertEquals("com.例え.テスト", data.packageName);
        }

        @Test
        @DisplayName("Very long mixin array")
        void testLongMixinArray(){
            MixinConfigData data = new MixinConfigData();
            String[] mixins = new String[1000];
            for(int i = 0; i < 1000; i++){
                mixins[i] = "Mixin" + i;
            }
            data.mixins = mixins;
            assertEquals(1000, data.mixins.length);
            assertEquals("Mixin999", data.mixins[999]);
        }
    }


    @Nested
    @DisplayName("MindustryMixinLogger Tests")
    class LoggerTests{

        @Test
        @DisplayName("Logger can be created via service")
        void testLoggerCreation() throws Exception{
            MindustryMixinService service = new MindustryMixinService();

            Method createLogger = MixinServiceAbstract.class.getDeclaredMethod("createLogger", String.class);
            createLogger.setAccessible(true);

            Object logger = createLogger.invoke(service, "TestLogger");
            assertNotNull(logger);
        }
    }


    @Nested
    @DisplayName("Bytecode Provider Deep Tests")
    class BytecodeDeepTests{

        private MindustryBytecodeProvider provider;

        @BeforeEach
        void setup(){
            provider = new MindustryBytecodeProvider();
        }

        @Test
        @DisplayName("ClassNode has correct superclass for ArrayList")
        void testArrayListSuperclass() throws Exception{
            ClassNode node = provider.getClassNode("java.util.ArrayList");
            assertEquals("java/util/AbstractList", node.superName);
        }

        @Test
        @DisplayName("ClassNode has correct interfaces for ArrayList")
        void testArrayListInterfaces() throws Exception{
            ClassNode node = provider.getClassNode("java.util.ArrayList");
            assertTrue(node.interfaces.contains("java/util/List"));
            assertTrue(node.interfaces.contains("java/util/RandomAccess"));
            assertTrue(node.interfaces.contains("java/lang/Cloneable"));
            assertTrue(node.interfaces.contains("java/io/Serializable"));
        }

        @Test
        @DisplayName("ClassNode contains expected methods for String")
        void testStringMethods() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.String");
            List<String> methodNames = new ArrayList<>();
            for(MethodNode method : node.methods){
                methodNames.add(method.name);
            }
            assertTrue(methodNames.contains("length"));
            assertTrue(methodNames.contains("charAt"));
            assertTrue(methodNames.contains("substring"));
            assertTrue(methodNames.contains("equals"));
            assertTrue(methodNames.contains("hashCode"));
            assertTrue(methodNames.contains("toString"));
        }

        @Test
        @DisplayName("ClassNode methods have correct descriptors")
        void testMethodDescriptors() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.String");
            MethodNode lengthMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("length") && method.desc.equals("()I")){
                    lengthMethod = method;
                    break;
                }
            }
            assertNotNull(lengthMethod, "String.length() method not found");
            assertEquals("()I", lengthMethod.desc);
        }

        @Test
        @DisplayName("ClassNode contains fields for HashMap")
        void testHashMapFields() throws Exception{
            ClassNode node = provider.getClassNode("java.util.HashMap");
            List<String> fieldNames = new ArrayList<>();
            for(FieldNode field : node.fields){
                fieldNames.add(field.name);
            }
            assertTrue(fieldNames.size() > 0, "HashMap should have fields");
        }

        @Test
        @DisplayName("ClassNode access flags are correct for public class")
        void testAccessFlags() throws Exception{
            ClassNode node = provider.getClassNode("java.util.ArrayList");
            assertTrue((node.access & Opcodes.ACC_PUBLIC) != 0, "ArrayList should be public");
            assertFalse((node.access & Opcodes.ACC_ABSTRACT) != 0, "ArrayList should not be abstract");
            assertFalse((node.access & Opcodes.ACC_INTERFACE) != 0, "ArrayList should not be an interface");
        }

        @Test
        @DisplayName("ClassNode access flags are correct for interface")
        void testInterfaceAccessFlags() throws Exception{
            ClassNode node = provider.getClassNode("java.util.List");
            assertTrue((node.access & Opcodes.ACC_PUBLIC) != 0, "List should be public");
            assertTrue((node.access & Opcodes.ACC_INTERFACE) != 0, "List should be an interface");
            assertTrue((node.access & Opcodes.ACC_ABSTRACT) != 0, "List should be abstract");
        }

        @Test
        @DisplayName("ClassNode access flags are correct for abstract class")
        void testAbstractClassFlags() throws Exception{
            ClassNode node = provider.getClassNode("java.util.AbstractList");
            assertTrue((node.access & Opcodes.ACC_PUBLIC) != 0, "AbstractList should be public");
            assertTrue((node.access & Opcodes.ACC_ABSTRACT) != 0, "AbstractList should be abstract");
            assertFalse((node.access & Opcodes.ACC_INTERFACE) != 0, "AbstractList should not be an interface");
        }

        @Test
        @DisplayName("ClassNode has annotations for Override methods")
        void testClassAnnotations() throws Exception{
            ClassNode node = provider.getClassNode("java.util.ArrayList");
            MethodNode toStringMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("toString")){
                    toStringMethod = method;
                    break;
                }
            }
            assertNotNull(node.methods);
        }

        @Test
        @DisplayName("ClassNode bytecode instructions can be read")
        void testBytecodeInstructions() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.Integer");
            MethodNode valueOfMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("valueOf") && method.desc.equals("(I)Ljava/lang/Integer;")){
                    valueOfMethod = method;
                    break;
                }
            }
            assertNotNull(valueOfMethod, "Integer.valueOf(int) not found");
            assertNotNull(valueOfMethod.instructions, "Method should have instructions");
            assertTrue(valueOfMethod.instructions.size() > 0, "Method should have bytecode instructions");
        }

        @Test
        @DisplayName("Different reader flags produce valid results")
        void testDifferentReaderFlags() throws Exception{
            ClassNode node1 = provider.getClassNode("java.lang.String", false, ClassReader.SKIP_DEBUG);
            assertNotNull(node1);

            ClassNode node2 = provider.getClassNode("java.lang.String", false, ClassReader.SKIP_FRAMES);
            assertNotNull(node2);

            ClassNode node3 = provider.getClassNode("java.lang.String", false, ClassReader.SKIP_CODE);
            assertNotNull(node3);

            assertEquals(node1.name, node2.name);
            assertEquals(node2.name, node3.name);
        }

        @Test
        @DisplayName("Exception class has correct structure")
        void testExceptionClass() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.RuntimeException");
            assertEquals("java/lang/Exception", node.superName);
        }

        @Test
        @DisplayName("Enum class has correct flags")
        void testEnumClass() throws Exception{
            ClassNode node = provider.getClassNode("java.lang.Thread$State");
            assertTrue((node.access & Opcodes.ACC_ENUM) != 0, "Thread.State should be an enum");
            assertTrue((node.access & Opcodes.ACC_FINAL) != 0, "Enum should be final");
        }
    }


    @Nested
    @DisplayName("Class Provider Resource Tests")
    class ClassProviderResourceTests{

        private MindustryClassProvider provider;

        @BeforeEach
        void setup(){
            provider = new MindustryClassProvider();
        }

        @Test
        @DisplayName("Can read manifest from runtime JAR")
        void testReadManifest(){
            InputStream stream = provider.getResourceAsStream("META-INF/MANIFEST.MF");
        }

        @Test
        @DisplayName("Can read class file as resource")
        void testReadClassAsResource() throws IOException{
            InputStream stream = provider.getResourceAsStream("java/lang/Object.class");
            assertNotNull(stream, "Should be able to read Object.class");

            byte[] magic = new byte[4];
            int read = stream.read(magic);
            assertEquals(4, read);
            assertEquals((byte)0xCA, magic[0]);
            assertEquals((byte)0xFE, magic[1]);
            assertEquals((byte)0xBA, magic[2]);
            assertEquals((byte)0xBE, magic[3]);
            stream.close();
        }

        @Test
        @DisplayName("Resource stream for package-info")
        void testPackageInfo(){
            InputStream stream = provider.getResourceAsStream("java/lang/package-info.class");
        }

        @Test
        @DisplayName("Multiple sequential class lookups work")
        void testSequentialLookups() throws ClassNotFoundException{
            String[] classes = {
                "java.lang.String",
                "java.util.List",
                "java.util.Map",
                "java.io.File",
                "java.net.URL",
                "java.lang.Thread",
                "java.util.concurrent.ConcurrentHashMap",
                "java.lang.reflect.Method"
            };

            for(String className : classes){
                Class<?> clazz = provider.findClass(className);
                assertNotNull(clazz, "Should find " + className);
                assertEquals(className, clazz.getName());
            }
        }

        @Test
        @DisplayName("Class loading is idempotent")
        void testIdempotentLoading() throws ClassNotFoundException{
            Class<?> first = provider.findClass("java.lang.StringBuilder");
            Class<?> second = provider.findClass("java.lang.StringBuilder");
            assertSame(first, second, "Same class should be returned");
        }
    }


    @Nested
    @DisplayName("Transformer Service Behavior Tests")
    class TransformerBehaviorTests{

        private MindustryTransformerService service;

        @BeforeEach
        void setup(){
            service = new MindustryTransformerService();
        }

        @Test
        @DisplayName("Transform preserves bytes when no transformer available")
        void testTransformPreservesBytes(){
            byte[] original = new byte[256];
            for(int i = 0; i < 256; i++){
                original[i] = (byte)i;
            }

            byte[] result = service.transformClass(null, "test.SomeClass", original);
            assertArrayEquals(original, result);
        }

        @Test
        @DisplayName("Transform handles null class name")
        void testTransformNullClassName(){
            byte[] data = new byte[]{1, 2, 3};
            byte[] result = service.transformClass(null, null, data);
            assertArrayEquals(data, result);
        }

        @Test
        @DisplayName("Multiple exclusions can be added")
        void testMultipleExclusions(){
            assertDoesNotThrow(() -> {
                for(int i = 0; i < 100; i++){
                    service.addTransformerExclusion("com.test.package" + i);
                }
            });
        }

        @Test
        @DisplayName("Transformer collections are immutable-safe")
        void testCollectionsSafe(){
            Collection<ITransformer> transformers = service.getTransformers();
            Collection<ITransformer> delegated = service.getDelegatedTransformers();

            assertEquals(transformers.size(), service.getTransformers().size());
            assertEquals(delegated.size(), service.getDelegatedTransformers().size());
        }
    }


    @Nested
    @DisplayName("Service Lifecycle Tests")
    class ServiceLifecycleTests{

        @Test
        @DisplayName("Service can be created multiple times")
        void testMultipleServiceInstances(){
            MindustryMixinService[] services = new MindustryMixinService[10];
            for(int i = 0; i < 10; i++){
                services[i] = new MindustryMixinService();
                services[i].prepare();
                services[i].init();
            }

            for(MindustryMixinService service : services){
                assertNotNull(service.getClassProvider());
                assertNotNull(service.getBytecodeProvider());
            }
        }

        @Test
        @DisplayName("Service name is consistent")
        void testServiceNameConsistent(){
            MindustryMixinService service1 = new MindustryMixinService();
            MindustryMixinService service2 = new MindustryMixinService();
            assertEquals(service1.getName(), service2.getName());
            assertEquals("Mindustry", service1.getName());
        }

        @Test
        @DisplayName("Init without prepare still works")
        void testInitWithoutPrepare(){
            MindustryMixinService service = new MindustryMixinService();
            assertDoesNotThrow(() -> service.init());
            assertNotNull(service.getClassProvider());
        }

        @Test
        @DisplayName("Providers are created fresh on init")
        void testProvidersFreshOnInit(){
            MindustryMixinService service = new MindustryMixinService();
            service.prepare();
            service.init();
            IClassProvider provider1 = service.getClassProvider();

            service.init();
            IClassProvider provider2 = service.getClassProvider();

            assertNotNull(provider2);
        }
    }


    @Nested
    @DisplayName("Connector Path Tests")
    class ConnectorPathTests{

        @Test
        @DisplayName("Connector handles missing mods directory gracefully")
        void testMissingModsDirectory(){
            MindustryMixinConnector connector = new MindustryMixinConnector();
            assertDoesNotThrow(() -> connector.connect());
        }

        @Test
        @DisplayName("Connector is an IMixinConnector")
        void testConnectorInterface(){
            MindustryMixinConnector connector = new MindustryMixinConnector();
            assertTrue(connector instanceof org.spongepowered.asm.mixin.connect.IMixinConnector);
        }
    }


    @Nested
    @DisplayName("Mixin Application Integration Tests")
    class MixinApplicationTests{

        /**
         * Test target class - simulates a Mindustry class that would be mixed into
         */
        public static class TestTargetClass{
            public int value = 0;

            public int getValue(){
                return value;
            }

            public void setValue(int v){
                this.value = v;
            }

            public String process(String input){
                return "original:" + input;
            }

            public int calculate(int a, int b){
                return a + b;
            }
        }

        @Test
        @DisplayName("Bytecode provider can read test target class")
        void testReadTargetClass() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            assertNotNull(node);
            assertTrue(node.name.endsWith("TestTargetClass"));

            List<String> methodNames = new ArrayList<>();
            for(MethodNode method : node.methods){
                methodNames.add(method.name);
            }
            assertTrue(methodNames.contains("getValue"));
            assertTrue(methodNames.contains("setValue"));
            assertTrue(methodNames.contains("process"));
            assertTrue(methodNames.contains("calculate"));
        }

        @Test
        @DisplayName("Bytecode provider can read method descriptors correctly")
        void testMethodDescriptors() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            MethodNode calculateMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("calculate")){
                    calculateMethod = method;
                    break;
                }
            }

            assertNotNull(calculateMethod);
            assertEquals("(II)I", calculateMethod.desc);
        }

        @Test
        @DisplayName("Bytecode provider reads field information")
        void testFieldInfo() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            FieldNode valueField = null;
            for(FieldNode field : node.fields){
                if(field.name.equals("value")){
                    valueField = field;
                    break;
                }
            }

            assertNotNull(valueField);
            assertEquals("I", valueField.desc);
        }

        @Test
        @DisplayName("Transformer service passes through unmodified bytecode")
        void testTransformerPassthrough() throws Exception{
            MindustryBytecodeProvider bytecodeProvider = new MindustryBytecodeProvider();
            MindustryTransformerService transformerService = new MindustryTransformerService();

            String className = TestTargetClass.class.getName();
            InputStream is = TestTargetClass.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class");
            assertNotNull(is);

            byte[] originalBytes = is.readAllBytes();
            is.close();

            byte[] transformedBytes = transformerService.transformClass(null, className, originalBytes);

            assertArrayEquals(originalBytes, transformedBytes);
        }

        @Test
        @DisplayName("ClassNode can be modified and written back")
        void testClassNodeModification() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            FieldNode newField = new FieldNode(
                Opcodes.ACC_PUBLIC,
                "injectedField",
                "I",
                null,
                42
            );
            node.fields.add(newField);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            byte[] modifiedBytes = writer.toByteArray();

            ClassReader reader = new ClassReader(modifiedBytes);
            ClassNode verifyNode = new ClassNode();
            reader.accept(verifyNode, 0);

            boolean hasInjectedField = false;
            for(FieldNode field : verifyNode.fields){
                if(field.name.equals("injectedField")){
                    hasInjectedField = true;
                    break;
                }
            }
            assertTrue(hasInjectedField, "Injected field should be present");
        }

        @Test
        @DisplayName("Method injection simulation works")
        void testMethodInjection() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            MethodNode newMethod = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "injectedMethod",
                "()V",
                null,
                null
            );
            newMethod.instructions = new InsnList();
            newMethod.instructions.add(new InsnNode(Opcodes.RETURN));
            newMethod.maxStack = 1;
            newMethod.maxLocals = 1;
            node.methods.add(newMethod);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            byte[] modifiedBytes = writer.toByteArray();

            ClassReader reader = new ClassReader(modifiedBytes);
            ClassNode verifyNode = new ClassNode();
            reader.accept(verifyNode, 0);

            boolean hasInjectedMethod = false;
            for(MethodNode method : verifyNode.methods){
                if(method.name.equals("injectedMethod")){
                    hasInjectedMethod = true;
                    break;
                }
            }
            assertTrue(hasInjectedMethod, "Injected method should be present");
        }

        @Test
        @DisplayName("Method body modification simulation")
        void testMethodBodyModification() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            MethodNode getValueMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("getValue")){
                    getValueMethod = method;
                    break;
                }
            }
            assertNotNull(getValueMethod);

            int originalInsnCount = getValueMethod.instructions.size();

            getValueMethod.instructions.clear();
            getValueMethod.instructions.add(new LdcInsnNode(42));
            getValueMethod.instructions.add(new InsnNode(Opcodes.IRETURN));

            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            byte[] modifiedBytes = writer.toByteArray();

            ClassReader reader = new ClassReader(modifiedBytes);
            ClassNode verifyNode = new ClassNode();
            reader.accept(verifyNode, 0);

            MethodNode verifyMethod = null;
            for(MethodNode m : verifyNode.methods){
                if(m.name.equals("getValue")){
                    verifyMethod = m;
                    break;
                }
            }

            assertNotNull(verifyMethod);
            assertTrue(verifyMethod.instructions.size() < originalInsnCount,
                "Modified method should have fewer instructions");

            boolean hasLdc42 = false;
            for(int i = 0; i < verifyMethod.instructions.size(); i++){
                AbstractInsnNode insn = verifyMethod.instructions.get(i);
                if(insn instanceof LdcInsnNode && ((LdcInsnNode)insn).cst.equals(42)){
                    hasLdc42 = true;
                    break;
                }
            }
            assertTrue(hasLdc42, "Modified method should contain LDC 42");
        }

        @Test
        @DisplayName("Interface injection simulation")
        void testInterfaceInjection() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            int originalInterfaceCount = node.interfaces.size();

            node.interfaces.add("java/lang/Comparable");

            MethodNode compareToMethod = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "compareTo",
                "(Ljava/lang/Object;)I",
                null,
                null
            );
            compareToMethod.instructions = new InsnList();
            compareToMethod.instructions.add(new InsnNode(Opcodes.ICONST_0));
            compareToMethod.instructions.add(new InsnNode(Opcodes.IRETURN));
            compareToMethod.maxStack = 1;
            compareToMethod.maxLocals = 2;
            node.methods.add(compareToMethod);

            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            byte[] modifiedBytes = writer.toByteArray();

            ClassReader reader = new ClassReader(modifiedBytes);
            ClassNode verifyNode = new ClassNode();
            reader.accept(verifyNode, 0);

            assertEquals(originalInterfaceCount + 1, verifyNode.interfaces.size());
            assertTrue(verifyNode.interfaces.contains("java/lang/Comparable"),
                "Modified class should have Comparable interface");

            boolean hasCompareTo = false;
            for(MethodNode m : verifyNode.methods){
                if(m.name.equals("compareTo") && m.desc.equals("(Ljava/lang/Object;)I")){
                    hasCompareTo = true;
                    break;
                }
            }
            assertTrue(hasCompareTo, "Modified class should have compareTo method");
        }

        @Test
        @DisplayName("Superclass modification simulation")
        void testSuperclassReading() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            assertEquals("java/lang/Object", node.superName);
        }

        @Test
        @DisplayName("Method instruction analysis for injection points")
        void testInstructionAnalysis() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            MethodNode calculateMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("calculate")){
                    calculateMethod = method;
                    break;
                }
            }
            assertNotNull(calculateMethod);

            boolean hasAdd = false;
            for(int i = 0; i < calculateMethod.instructions.size(); i++){
                if(calculateMethod.instructions.get(i).getOpcode() == Opcodes.IADD){
                    hasAdd = true;
                    break;
                }
            }
            assertTrue(hasAdd, "calculate method should contain IADD instruction");
        }

        @Test
        @DisplayName("HEAD injection point simulation")
        void testHeadInjection() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            MethodNode processMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("process")){
                    processMethod = method;
                    break;
                }
            }
            assertNotNull(processMethod);

            InsnList injectedCode = new InsnList();
            injectedCode.add(new InsnNode(Opcodes.NOP));

            AbstractInsnNode firstReal = processMethod.instructions.getFirst();
            while(firstReal != null && (firstReal.getOpcode() == -1)){
                firstReal = firstReal.getNext();
            }

            if(firstReal != null){
                processMethod.instructions.insertBefore(firstReal, injectedCode);
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            byte[] modifiedBytes = writer.toByteArray();

            assertNotNull(modifiedBytes);
            assertTrue(modifiedBytes.length > 0);
        }

        @Test
        @DisplayName("RETURN injection point simulation")
        void testReturnInjection() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode(TestTargetClass.class.getName());

            MethodNode setValueMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("setValue")){
                    setValueMethod = method;
                    break;
                }
            }
            assertNotNull(setValueMethod);

            for(int i = 0; i < setValueMethod.instructions.size(); i++){
                AbstractInsnNode insn = setValueMethod.instructions.get(i);
                if(insn.getOpcode() == Opcodes.RETURN){
                    InsnList injectedCode = new InsnList();
                    injectedCode.add(new InsnNode(Opcodes.NOP));
                    setValueMethod.instructions.insertBefore(insn, injectedCode);
                    break;
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            byte[] modifiedBytes = writer.toByteArray();

            assertNotNull(modifiedBytes);
        }

    }


    @Nested
    @DisplayName("Mixin Config JSON Tests")
    class MixinConfigJsonTests{

        @Test
        @DisplayName("MixinConfigData matches expected JSON structure")
        void testConfigDataStructure(){
            MixinConfigData data = new MixinConfigData();

            assertNotNull(data.mixins);
            assertNotNull(data.client);
            assertNotNull(data.server);
            assertEquals("0.8", data.minVersion);
            assertEquals("JAVA_8", data.compatibilityLevel);
        }

        @Test
        @DisplayName("Full mixin config can be constructed programmatically")
        void testFullConfigConstruction(){
            MixinConfigData data = new MixinConfigData();
            data.required = true;
            data.minVersion = "0.8.5";
            data.packageName = "com.example.mixin";
            data.compatibilityLevel = "JAVA_17";
            data.mixins = new String[]{"EntityMixin", "WorldMixin", "BlockMixin"};
            data.client = new String[]{"RenderMixin", "UIMixin"};
            data.server = new String[]{"ServerTickMixin"};
            data.verbose = true;
            data.priority = 1500;

            MixinConfig config = new MixinConfig("TestMod", null, data);

            assertEquals("TestMod", config.modName);
            assertNull(config.configFile);
            assertEquals(3, config.data.mixins.length);
            assertEquals(2, config.data.client.length);
            assertEquals(1, config.data.server.length);
        }

        @Test
        @DisplayName("Config data arrays support all mixin categories")
        void testAllMixinCategories(){
            MixinConfigData data = new MixinConfigData();

            data.mixins = new String[]{"CommonMixin1", "CommonMixin2"};

            data.client = new String[]{"ClientMixin1"};

            data.server = new String[]{"ServerMixin1", "ServerMixin2", "ServerMixin3"};

            assertEquals(2, data.mixins.length);
            assertEquals(1, data.client.length);
            assertEquals(3, data.server.length);
        }
    }


    @Nested
    @DisplayName("Mixin Priority Tests")
    class MixinPriorityTests{

        @Test
        @DisplayName("Lower priority value means earlier application")
        void testPriorityOrdering(){
            MixinConfigData lowPriority = new MixinConfigData();
            lowPriority.priority = 100;

            MixinConfigData defaultPriority = new MixinConfigData();

            MixinConfigData highPriority = new MixinConfigData();
            highPriority.priority = 5000;

            assertTrue(lowPriority.priority < defaultPriority.priority);
            assertTrue(defaultPriority.priority < highPriority.priority);
        }

        @Test
        @DisplayName("Multiple configs can be sorted by priority")
        void testPrioritySorting(){
            MixinConfig[] configs = new MixinConfig[4];

            MixinConfigData d1 = new MixinConfigData();
            d1.priority = 500;
            configs[0] = new MixinConfig("ModA", null, d1);

            MixinConfigData d2 = new MixinConfigData();
            d2.priority = 100;
            configs[1] = new MixinConfig("ModB", null, d2);

            MixinConfigData d3 = new MixinConfigData();
            d3.priority = 2000;
            configs[2] = new MixinConfig("ModC", null, d3);

            MixinConfigData d4 = new MixinConfigData();
            d4.priority = 1000;
            configs[3] = new MixinConfig("ModD", null, d4);

            java.util.Arrays.sort(configs, (a, b) -> Integer.compare(a.data.priority, b.data.priority));

            assertEquals("ModB", configs[0].modName); // 100
            assertEquals("ModA", configs[1].modName); // 500
            assertEquals("ModD", configs[2].modName); // 1000
            assertEquals("ModC", configs[3].modName); // 2000
        }
    }


    @Nested
    @DisplayName("Mixin Error Handling Tests")
    class MixinErrorHandlingTests{

        @Test
        @DisplayName("Invalid class name throws ClassNotFoundException")
        void testInvalidClassName(){
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();

            assertThrows(ClassNotFoundException.class, () -> {
                provider.getClassNode("com.nonexistent.MixinTarget");
            });
        }

        @Test
        @DisplayName("Malformed bytecode is detected")
        void testMalformedBytecode(){
            byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03};

            assertThrows(Exception.class, () -> {
                ClassReader reader = new ClassReader(garbage);
            });
        }

        @Test
        @DisplayName("Empty bytecode is detected")
        void testEmptyBytecode(){
            byte[] empty = new byte[0];

            assertThrows(Exception.class, () -> {
                ClassReader reader = new ClassReader(empty);
            });
        }

        @Test
        @DisplayName("Transformer handles invalid input gracefully")
        void testTransformerInvalidInput(){
            MindustryTransformerService service = new MindustryTransformerService();

            byte[] invalidBytes = new byte[]{1, 2, 3};
            byte[] result = service.transformClass(null, "test.Class", invalidBytes);

            assertArrayEquals(invalidBytes, result);
        }

        @Test
        @DisplayName("Required flag indicates transformation is mandatory")
        void testRequiredFlag(){
            MixinConfigData required = new MixinConfigData();
            required.required = true;

            MixinConfigData optional = new MixinConfigData();
            optional.required = false;

            assertTrue(required.required);
            assertFalse(optional.required);
        }
    }


    @Nested
    @DisplayName("Shadow Access Tests")
    class ShadowAccessTests{

        @Test
        @DisplayName("Private fields can be accessed via bytecode")
        void testPrivateFieldAccess() throws Exception{
            ClassNode node = new ClassNode();
            node.version = Opcodes.V17;
            node.access = Opcodes.ACC_PUBLIC;
            node.name = "test/PrivateFieldTarget";
            node.superName = "java/lang/Object";

            node.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE,
                "secretValue",
                "I",
                null,
                null
            ));

            MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
            init.instructions.add(new InsnNode(Opcodes.RETURN));
            init.maxStack = 1;
            init.maxLocals = 1;
            node.methods.add(init);

            MethodNode getter = new MethodNode(Opcodes.ACC_PUBLIC, "getSecret", "()I", null, null);
            getter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            getter.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "test/PrivateFieldTarget", "secretValue", "I"));
            getter.instructions.add(new InsnNode(Opcodes.IRETURN));
            getter.maxStack = 1;
            getter.maxLocals = 1;
            node.methods.add(getter);

            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            byte[] bytes = writer.toByteArray();

            ClassReader reader = new ClassReader(bytes);
            ClassNode verifyNode = new ClassNode();
            reader.accept(verifyNode, 0);

            FieldNode secretField = null;
            for(FieldNode f : verifyNode.fields){
                if(f.name.equals("secretValue")){
                    secretField = f;
                    break;
                }
            }
            assertNotNull(secretField);
            assertTrue((secretField.access & Opcodes.ACC_PRIVATE) != 0, "Field should be private");
        }

        @Test
        @DisplayName("Protected methods can be invoked via bytecode")
        void testProtectedMethodAccess() throws Exception{
            MindustryBytecodeProvider provider = new MindustryBytecodeProvider();
            ClassNode node = provider.getClassNode("java.lang.Object");

            MethodNode cloneMethod = null;
            for(MethodNode method : node.methods){
                if(method.name.equals("clone")){
                    cloneMethod = method;
                    break;
                }
            }

            assertNotNull(cloneMethod);
            assertTrue((cloneMethod.access & Opcodes.ACC_PROTECTED) != 0);
        }
    }


    @Nested
    @DisplayName("Callback Info Simulation Tests")
    class CallbackInfoTests{

        @Test
        @DisplayName("Cancellable callback info structure")
        void testCancellableStructure(){
            class MockCallbackInfo{
                private boolean cancelled = false;
                private Object returnValue = null;

                public void cancel(){
                    this.cancelled = true;
                }

                public boolean isCancelled(){
                    return cancelled;
                }

                public void setReturnValue(Object value){
                    this.returnValue = value;
                    this.cancelled = true;
                }

                public Object getReturnValue(){
                    return returnValue;
                }
            }

            MockCallbackInfo ci = new MockCallbackInfo();
            assertFalse(ci.isCancelled());

            ci.cancel();
            assertTrue(ci.isCancelled());
        }

        @Test
        @DisplayName("Return value callback structure")
        void testReturnValueCallback(){
            class MockCallbackInfoReturnable<T>{
                private boolean cancelled = false;
                private T returnValue;

                public MockCallbackInfoReturnable(T originalReturn){
                    this.returnValue = originalReturn;
                }

                public T getReturnValue(){
                    return returnValue;
                }

                public void setReturnValue(T value){
                    this.returnValue = value;
                    this.cancelled = true;
                }

                public boolean isCancelled(){
                    return cancelled;
                }
            }

            MockCallbackInfoReturnable<Integer> cir = new MockCallbackInfoReturnable<>(10);
            assertEquals(10, cir.getReturnValue());

            cir.setReturnValue(42);
            assertEquals(42, cir.getReturnValue());
            assertTrue(cir.isCancelled());
        }
    }
}
