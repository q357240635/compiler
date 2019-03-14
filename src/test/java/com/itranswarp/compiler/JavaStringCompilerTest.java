package com.itranswarp.compiler;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.itranswarp.on.the.fly.BeanProxy;
import com.itranswarp.on.the.fly.User;

public class JavaStringCompilerTest {

	JavaStringCompiler compiler;

	@Before
	public void setUp() throws Exception {
		compiler = new JavaStringCompiler();
	}

	static final String SINGLE_JAVA = "/* a single java class to one file */  "
			+ "package on.the.fly;                                            "
			+ "import com.itranswarp.on.the.fly.*;                            "
			+ "public class UserProxy extends User implements BeanProxy {     "
			+ "    boolean _dirty = false;                                    "
			+ "    public void setId(String id) {                             "
			+ "        super.setId(id);                                       "
			+ "        setDirty(true);                                        "
			+ "    }                                                          "
			+ "    public void setName(String name) {                         "
			+ "        super.setName(name);                                   "
			+ "        setDirty(true);                                        "
			+ "    }                                                          "
			+ "    public void setCreated(long created) {                     "
			+ "        super.setCreated(created);                             "
			+ "        setDirty(true);                                        "
			+ "    }                                                          "
			+ "    public void setDirty(boolean dirty) {                      "
			+ "        this._dirty = dirty;                                   "
			+ "    }                                                          "
			+ "    public boolean isDirty() {                                 "
			+ "        return this._dirty;                                    "
			+ "    }                                                          "
			+ "}                                                              ";

	@Test
	public void testCompileSingleClass() throws Exception {
		Map<String, byte[]> results = compiler.compile("UserProxy.java", SINGLE_JAVA);
		assertEquals(1, results.size());
		assertTrue(results.containsKey("on.the.fly.UserProxy"));
		Class<?> clazz = compiler.loadClass("on.the.fly.UserProxy", results);
		// get method:
		Method setId = clazz.getMethod("setId", String.class);
		Method setName = clazz.getMethod("setName", String.class);
		Method setCreated = clazz.getMethod("setCreated", long.class);
		// try instance:
		Object obj = clazz.newInstance();
		// get as proxy:
		BeanProxy proxy = (BeanProxy) obj;
		assertFalse(proxy.isDirty());
		// set:
		setId.invoke(obj, "A-123");
		setName.invoke(obj, "Fly");
		setCreated.invoke(obj, 123000999);
		// get as user:
		User user = (User) obj;
		assertEquals("A-123", user.getId());
		assertEquals("Fly", user.getName());
		assertEquals(123000999, user.getCreated());
		assertTrue(proxy.isDirty());
	}

	static final String MULTIPLE_JAVA = "/* a single class to many files */   "
			+ "package on.the.fly;                                            "
			+ "import java.util.*;                                            "
			+ "public class Multiple {                                        "
			+ "    List<Bird> list = new ArrayList<Bird>();                   "
			+ "    public void add(String name) {                             "
			+ "        Bird bird = new Bird();                                "
			+ "        bird.name = name;                                      "
			+ "        this.list.add(bird);                                   "
			+ "    }                                                          "
			+ "    public Bird getFirstBird() {                               "
			+ "        return this.list.get(0);                               "
			+ "    }                                                          "
			+ "    public static class StaticBird {                           "
			+ "        public int weight = 100;                               "
			+ "    }                                                          "
			+ "    class NestedBird {                                         "
			+ "        NestedBird() {                                         "
			+ "            System.out.println(list.size() + \" birds...\");   "
			+ "        }                                                      "
			+ "    }                                                          "
			+ "}                                                              "
			+ "/* package level */                                            "
			+ "class Bird {                                                   "
			+ "    String name = null;                                        "
			+ "}                                                              ";

	@Test
	public void testCompileMultipleClasses() throws Exception {
		Map<String, byte[]> results = compiler.compile("Multiple.java", MULTIPLE_JAVA);
		assertEquals(4, results.size());
		assertTrue(results.containsKey("on.the.fly.Multiple"));
		assertTrue(results.containsKey("on.the.fly.Multiple$StaticBird"));
		assertTrue(results.containsKey("on.the.fly.Multiple$NestedBird"));
		assertTrue(results.containsKey("on.the.fly.Bird"));
		Class<?> clzMul = compiler.loadClass("on.the.fly.Multiple", results);
		// try instance:
		Object obj = clzMul.newInstance();
		assertNotNull(obj);
	}
	static final String ENUM_JAVA = "package com.itranswarp.compiler;\n" +
			"\n" +
			"public enum WeekDay {\n" +
			"\n" +
			"    Mon(\"Monday\"), Tue(\"Tuesday\"), Wed(\"Wednesday\"), Thu(\"Thursday\"), Fri( \"Friday\"), Sat(\"Saturday\"), Sun(\"Sunday\");\n" +
			"    private final String day;\n" +
			"    private WeekDay(String day) {\n" +
			"        this.day = day;\n" +
			"    }\n" +
			"    public static void printDay(int i){\n" +
			"        switch(i){\n" +
			"            case 1: System.out.println(WeekDay.Mon); break;\n" +
			"            case 2: System.out.println(WeekDay.Tue);break;\n" +
			"            case 3: System.out.println(WeekDay.Wed);break;\n" +
			"            case 4: System.out.println(WeekDay.Thu);break;\n" +
			"            case 5: System.out.println(WeekDay.Fri);break;\n" +
			"            case 6: System.out.println(WeekDay.Sat);break;\n" +
			"            case 7: System.out.println(WeekDay.Sun);break;\n" +
			"            default:System.out.println(\"wrong number!\");\n" +
			"        }\n" +
			"    }\n" +
			"    public String getDay() {\n" +
			"        return day;\n" +
			"    }\n" +
			"}\n";

	@Test
	public void testCompileENUM() throws Exception {
		Map<String, byte[]> results = compiler.compile("WeekDay.java", ENUM_JAVA);
		assertEquals(1, results.size());
		assertTrue(results.containsKey("com.itranswarp.compiler.WeekDay"));
		Class<?> clzMul = compiler.loadClass("com.itranswarp.compiler.WeekDay", results);
		// try instance:
		//Object obj = clzMul.newInstance();
		//assertNotNull(obj);
		System.out.print("=================================");
		WeekDay.printDay(7);
	}
}
