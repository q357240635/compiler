package com.itranswarp.compiler;

import com.google.gson.Gson;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import sun.reflect.ConstructorAccessor;
import sun.reflect.FieldAccessor;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @version 1.5.4.1-RELEASE
 * @Description: TODO
 * @Auther: jinsong
 * @Date: 2019/3/14 15:48
 */
public class DynamicEnumUtil {
    public static void addField(String classname, String[] fields) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        // pool.appendClassPath(new LoaderClassPath(ByteUtil.class.getClassLoader()));
        //		pool.insertClassPath(Thread.currentThread().getContextClassLoader().getResource(".").getFile());
        CtClass cc = pool.get(classname);
        for (String field : fields) {
            field = field.trim().toUpperCase();
            CtField ctf = new CtField(cc, field, cc);
            ctf.setModifiers(Modifier.PUBLIC);
            cc.addField(ctf);
        }
        Class c = cc.toClass();
        addEnum(c, fields, new Class<?>[] {}, new Object[] {});
    }

    public static <T> void addEnum(Class enumType, String[] enumValues , Class<?>[] paramClass, Object[] paramValue) {
        // 0. Sanity checks
        if (!Enum.class.isAssignableFrom(enumType)) {
            throw new RuntimeException("class " + enumType + " is not an instance of Enum");
        }
        // 1. Lookup "$VALUES" holder in enum class and get previous enum instances
        Field valuesField = null;
        Field[] fields = enumType.getDeclaredFields();
        Method[] methods = enumType.getDeclaredMethods();
        for (Field field : fields) {
            if (field.getName().contains("$VALUES")) {
                valuesField = field;
                break;
            }
        }
        AccessibleObject.setAccessible(new Field[] { valuesField }, true);
        try {
            for (String enumValue : enumValues) {
                enumValue = enumValue.trim().toUpperCase();
                // 2. Copy it
                T[] previousValues = (T[]) valuesField.get(enumType);
                List<T> values = new ArrayList<T>(Arrays.asList(previousValues));
                // 3. build new enum
                T newValue = (T) makeEnum(enumType, // The target enum class
                        enumValue, // THE NEW ENUM INSTANCE TO BE DYNAMICALLY ADDED
                        values.size(),
                        // new Class<?>[] {}, // could be used to pass values to the enum constuctor if needed
                        paramClass,
                        // new Object[] {}
                        paramValue); // could be used to pass values to the enum constuctor if needed

                // 4. add new value
                values.add(newValue);
                Object object = values.toArray((T[]) Array.newInstance(enumType, 0));
                // 5. Set new values field
                setFailsafeFieldValue(valuesField, null, object);
                // 6. Clean enum cache
                cleanEnumCache(enumType);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<?>> void addEnum(Class<T> enumType, String enumValue,Class<?>[] paramClass,Object[] paramValue) {

        // 0. Sanity checks
        if (!Enum.class.isAssignableFrom(enumType)) {
            throw new RuntimeException("class " + enumType + " is not an instance of Enum");
        }

        // 1. Lookup "$VALUES" holder in enum class and get previous enum instances
        Field valuesField = null;
        Field[] fields = CodeInfoEnum.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().contains("$VALUES")) {
                valuesField = field;
                break;
            }
        }
        AccessibleObject.setAccessible(new Field[] { valuesField }, true);

        try {

            // 2. Copy it
            T[] previousValues = (T[]) valuesField.get(enumType);
            List<T> values = new ArrayList<T>(Arrays.asList(previousValues));

            // 3. build new enum
            T newValue = (T) makeEnum(enumType, // The target enum class
                    enumValue, // THE NEW ENUM INSTANCE TO BE DYNAMICALLY ADDED
                    values.size(),
                    //new Class<?>[] {}, // could be used to pass values to the enum constuctor if needed
                    paramClass,
                    //new Object[] {}
                    paramValue
            ); // could be used to pass values to the enum constuctor if needed

            // 4. add new value
            values.add(newValue);
            Object object=values.toArray((T[]) Array.newInstance(enumType, 0));
            // 5. Set new values field
            setFailsafeFieldValue(valuesField, null, object);

            // 6. Clean enum cache
            cleanEnumCache(enumType);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static Object makeEnum(Class<?> enumClass, String value, int ordinal, Class<?>[] additionalTypes, Object[] additionalValues) throws Exception {
        Object[] parms = new Object[additionalValues.length + 2];
        parms[0] = value;
        parms[1] = Integer.valueOf(ordinal);
        System.arraycopy(additionalValues, 0, parms, 2, additionalValues.length);
        return enumClass.cast(getConstructorAccessor(enumClass, additionalTypes).newInstance(parms));
    }

    private static ConstructorAccessor getConstructorAccessor(Class<?> enumClass, Class<?>[] additionalParameterTypes) throws NoSuchMethodException {
        Class<?>[] parameterTypes = new Class[additionalParameterTypes.length + 2];
        parameterTypes[0] = String.class;
        parameterTypes[1] = int.class;
        System.arraycopy(additionalParameterTypes, 0, parameterTypes, 2, additionalParameterTypes.length);
        return reflectionFactory.newConstructorAccessor(enumClass.getDeclaredConstructor(parameterTypes));
    }

    private static ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();

    private static void setFailsafeFieldValue(Field field, Object target, Object value) throws NoSuchFieldException, IllegalAccessException {

        // let's make the field accessible
        field.setAccessible(true);
        // next we change the modifier in the Field instance to
        // not be final anymore, thus tricking reflection into
        // letting us modify the static final field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        int modifiers = modifiersField.getInt(field);

        // blank out the final bit in the modifiers int
        modifiers &= ~Modifier.FINAL;
        modifiersField.setInt(field, modifiers);

        FieldAccessor fa = reflectionFactory.newFieldAccessor(field, false);
        fa.set(target, value);
    }

    private static void blankField(Class<?> enumClass, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        for (Field field : Class.class.getDeclaredFields()) {
            if (field.getName().contains(fieldName)) {
                AccessibleObject.setAccessible(new Field[] { field }, true);
                setFailsafeFieldValue(field, enumClass, null);
                break;
            }
        }
    }

    private static void cleanEnumCache(Class<?> enumClass) throws NoSuchFieldException, IllegalAccessException {
        blankField(enumClass, "enumConstantDirectory"); // Sun (Oracle?!?) JDK 1.5/6
        blankField(enumClass, "enumConstants"); // IBM JDK
    }

    public static void main(String[] args) throws Exception {
        /*addField("com.itranswarp.compiler.Env", new String[] { "mj1", "mj2" });

        Gson gson = new Gson();
        System.out.println(Env.valueOf("MJ1"));
        System.out.println(gson.toJson(Env.values()));
        System.out.println(gson.toJson(Env.valueOf("MJ1")));
        System.out.println(gson.toJson(Env.valueOf("MJ2")));*/


        //        addEnum(Env.class, "testmj", new Class<?>[]{}, new Object[]{});
        //        for(Env codeInfo:Env.values()){
        //            System.out.println(codeInfo.toString());
        //        }
        //        String test="{\"test1\":\"123\",\"test2\":\"456\"}";
        //        Gson gson = new Gson();
        //        Map<String,String> map = gson.fromJson(test, Map.class);
        //        System.out.println(gson.toJson(Env.valueOf("testmj")));

        synchronized (CodeInfoEnum.class) {
            addEnum(CodeInfoEnum.class, "3", new Class<?>[]{Long.class, Long.class, String.class, String.class}, new Object[]{2L, 3L, "ActiveStatus", "Active"});
            addEnum(CodeInfoEnum.class, "4", new Class<?>[]{Long.class, Long.class, String.class, String.class}, new Object[]{2L, 4L, "ActiveStatus", "Inactive"});
            addEnum(CodeInfoEnum.class, "5", new Class<?>[]{Long.class, Long.class, String.class, String.class}, new Object[]{3L, 5L, "Optype", "OP1"});
            addEnum(CodeInfoEnum.class, "6", new Class<?>[]{Long.class, Long.class, String.class, String.class}, new Object[]{3L, 6L, "Optype", "OP2"});
            addEnum(CodeInfoEnum.class, "7", new Class<?>[]{Long.class, Long.class, String.class, String.class}, new Object[]{3L, 7L, "Optype", "OP3"});
            addEnum(CodeInfoEnum.class, "8", new Class<?>[]{Long.class, Long.class, String.class, String.class}, new Object[]{3L, 8L, "Optype", "OP4"});
        }
        CodeInfoEnum codeInfoEnum =CodeInfoEnum.valueOf("5");
        System.out.println(codeInfoEnum);
        // Run a few tests just to show it works OK.
        System.out.println(Arrays.deepToString(CodeInfoEnum.values()));
        System.out.println("============================打印所有枚举（包括固定的和动态的），可以将数据库中保存的CIC以枚举的形式加载到JVM");
        for(CodeInfoEnum codeInfo:CodeInfoEnum.values()){
            System.out.println(codeInfo.toString());
        }

        System.out.println("============================通过codeId找到的枚举，用于PO转VO的处理");
        CodeInfoEnum activeStatus_Active = CodeInfoEnum.getByInfoId(3L);
        System.out.println(activeStatus_Active);

        System.out.println("============================通过ClassId找到的枚举列表");
        List<CodeInfoEnum> activeStatusEnumList = CodeInfoEnum.getByClassId(3L);
        for(CodeInfoEnum codeInfo : activeStatusEnumList){
            System.out.println(codeInfo);
        }

        System.out.println("============================通过ClassCode和InfoCode获取枚举，用于导入验证CIC合法性");
        CodeInfoEnum toGetActiveStatus_Active = CodeInfoEnum.getByClassCodeAndInfoCode("ActiveStatus","Active");
        System.out.println(toGetActiveStatus_Active);

        System.out.println("============================通过ClassCode和InfoCode获取枚举，输入不存在的Code，则返回NULL");
        CodeInfoEnum toGetActiveStatus_miss = CodeInfoEnum.getByClassCodeAndInfoCode("ActiveStatus","MISS");
        System.out.println(toGetActiveStatus_miss);


    }
}
