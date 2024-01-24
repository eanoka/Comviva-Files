package com.grameenphone.wipro.utility.common;

import com.grameenphone.wipro.exception.CallbackException;
import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.orm.HibernateUtil;

import jakarta.persistence.EntityManager;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sample templates - This is a primitive type or String $pvalue$. You can use date type binding with default format 'yyyy-MM-dd' or you can provide custom format also $dvalue>format@dd/MM/yyyy$. There is no escape character, so you cannot use $ and other used special characters in bindings. For any map binding value, > can be used to access the value for that key $mvalue>child$. For List type of value binding > can be used to access value of specific index $avalue>0$. For any other type of object binding > can be used to access object property (getter, setter). <br>
 * Conditional block can be used as $if:ptocheck:eq:hello$Something to print only if condition is true$if$$else$This will be printed if last 'if' block condition is false$else$. The block inside $if$ and $else$ can have nested variables. In the if syntax ptocheck is the bound variable, eq (equals) is the matching operator. Value of the bound ptocheck will be matched against the value given after operator. There is another matching operator ne (not equals). <br>
 * You can have a loop using $repeat:rvalue$This part will repeat for each element in rvalue. You can get the index as $index$, current object as $value$ and size of the list as $total$ inside this repeat block$repeat$. rvalue should be a list.<br>
 * To print a '$' it should be written as $$ in template
 */
public class MacroReplacer {
    private static ThreadLocal<Supplier<EntityManager>> lazyInitializerSupplierThreadLocal = new ThreadLocal<>();

    public static class ReplacerCallback {
        public Object call(String key) throws CallbackException {
            return null;
        }
    }

    public static String replaceMacros(String input, KV<String, Object>... bindings) throws IOException {
        StringBuilder output = new StringBuilder();
        return replaceMacros(input, MapUtil.of(bindings), output);
    }

    public static String replaceMacros(String input, Map bindings) throws IOException {
        StringBuilder output = new StringBuilder();
        return replaceMacros(input, bindings, output);
    }

    public static String replaceMacros(String input, Map bindings, Supplier<EntityManager> lazyInitializerSupplier) throws IOException {
        try {
            if(lazyInitializerSupplier != null) {
                lazyInitializerSupplierThreadLocal.set(lazyInitializerSupplier);
            }
            StringBuilder output = new StringBuilder();
            return replaceMacros(input, bindings, output);
        } finally {
            lazyInitializerSupplierThreadLocal.remove();
        }
    }

    private static String replaceMacros(String input, Map bindings, StringBuilder output) throws IOException {
        Boolean lastIfConditionResult = null;
        StringReader reader = new StringReader(input);
        try {
            while (true) {
                int skipCount = skipUpTo(reader, '$', output);
                if(skipCount != 0) {
                    lastIfConditionResult = null;
                }
                String token = captureUpTo(reader, '$');
                String[] tokens;
                if (token.equals("")) {
                    output.append('$');
                } else if ((tokens = token.split(":")).length > 1 && tokens[0].equals("if")) {
                    String block = captureUpTo(reader, "if");
                    String checkValue = convertToString(convertToken(tokens[1], bindings));
                    boolean ifMatched = false;
                    if (tokens.length > 2) {
                        String op = tokens[2];
                        String matchValue = tokens[3];
                        if (op.equals("eq")) {
                            ifMatched = checkValue.equals(matchValue);
                        } else if (op.equals("ne")) {
                            ifMatched = !checkValue.equals(matchValue);
                        }
                    } else {
                        if(checkValue != null && !checkValue.equals("")) {
                            ifMatched = true;
                        }
                    }
                    lastIfConditionResult = ifMatched;
                    if (ifMatched) {
                        replaceMacros(block, bindings, output);
                    }
                } else if (tokens[0].equals("else")) {
                    String block = captureUpTo(reader, "else");
                    if (lastIfConditionResult != null && !lastIfConditionResult) {
                        replaceMacros(block, bindings, output);
                    }
                } else if (tokens[0].equals("repeat")) {
                    String block = captureUpTo(reader, "repeat");
                    Object repeatable = convertToken(tokens[1], bindings);
                    if (repeatable instanceof List) {
                        Map newScope = new HashMap(bindings);
                        int i = 0;
                        int total = ((List) repeatable).size();
                        newScope.put("total", "" + total);
                        for (Object entry : (List) repeatable) {
                            newScope.put("value", entry);
                            newScope.put("index", "" + i++);
                            replaceMacros(block, newScope, output);
                        }
                    }
                } else {
                    output.append(convertToString(convertToken(token, bindings)));
                }
                if(!token.startsWith("if:")) {
                    lastIfConditionResult = null;
                }
            }
        } catch (EOF e) {
        }
        return output.toString();
    }

    static class EOF extends Error {
        public EOF() {
        }
    }

    private static Object accessObjectUsingReflection(Object source, String part) {
        try {
            HibernateUtil.initializeProxy(source);
            try {
                PropertyDescriptor descriptor = new PropertyDescriptor(part, source.getClass(), "read", null);
                Method getter = descriptor.getReadMethod();
                if(getter != null) {
                    return getter.invoke(source);
                }
            } catch (IntrospectionException | InvocationTargetException e) {
            }
            Field field = source.getClass().getDeclaredField(part);
            return field.get(source);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private static String formatDates(Object date, String format) {
        if(date instanceof Date) {
            return new SimpleDateFormat(format).format((Date) date);
        }
        if(date instanceof TemporalAccessor) {
            return DateTimeFormatter.ofPattern(format).format((TemporalAccessor) date);
        }
        return null;
    }

    private static String convertToString(Object value) {
        if(value instanceof Date || value instanceof TemporalAccessor) {
            return formatDates(value, "yyyy-MM-dd");
        }
        return value.toString();
    }
    private static Object convertToken(String token, Object binding) {
        String[] mapParts = token.split(">");
        Object value = binding;
        for (String part : mapParts) {
            if(value == null) {
                break;
            }
            if(value instanceof ReplacerCallback) {
                try {
                    value = ((ReplacerCallback)value).call(part);
                } catch (CallbackException e) {
                    throw new RuntimeException(e);
                }
            }
            if(value instanceof Date || value instanceof TemporalAccessor) {
                if(part.startsWith("format@")) {
                    String format = part.substring(7);
                    value = formatDates(value, format);
                } else {
                    value = null;
                }
            } else if (value instanceof Map) {
                value = ((Map)value).get(part);
            } else if(value.getClass().isPrimitive() || value.getClass().getPackageName().startsWith("java.lang")) {
                value = null;
            } else if(value.getClass().isArray()) {
                int index = Integer.parseInt(part);
                Object[] arrayValue = (Object[]) value;
                if(arrayValue.length <= index) {
                    value = null;
                } else {
                    value = arrayValue[index];
                }
            } else if(Collection.class.isAssignableFrom(value.getClass())) {
                int index = Integer.parseInt(part);
                Object[] arrayValue = ((Collection)value).toArray();
                if(arrayValue.length <= index) {
                    value = null;
                } else {
                    value = arrayValue[index];
                }
            } else {
                value = accessObjectUsingReflection(value, part);
            }
        }
        return value == null ? "" : value;
    }

    private static int skipUpTo(Reader reader, char lookup, StringBuilder out) throws IOException {
        int c;
        int skipCount = 0;
        while (true) {
            c = reader.read();
            if (c == -1) {
                throw new EOF();
            }
            if (c == (int) lookup) {
                return skipCount;
            }
            skipCount++;
            out.append((char) c);
        }
    }

    static String captureUpTo(Reader reader, char lookup) throws IOException {
        StringBuilder builder = new StringBuilder();
        int c;
        while (true) {
            c = reader.read();
            if (c == -1) {
                throw new EOF();
            }
            if (c == (int) lookup) {
                return builder.toString();
            }
            builder.append((char) c);
        }
    }

    static String captureUpTo(Reader reader, String endTag) throws IOException {
        StringBuilder block = new StringBuilder();
        Integer startTagCount = 0;
        while (true) {
            skipUpTo(reader, '$', block);
            String token = captureUpTo(reader, '$');
            String[] tokens;
            if (token.equals(endTag) && startTagCount == 0) {
                break;
            } else if (token.equals(endTag)) {
                startTagCount--;
            } else if ((tokens = token.split(":")).length > 1 && tokens[0].equals(endTag)) {
                startTagCount++;
            }
            block.append("$");
            block.append(token);
            block.append("$");
        }
        return block.toString();
    }
}