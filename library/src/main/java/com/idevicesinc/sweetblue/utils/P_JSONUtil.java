package com.idevicesinc.sweetblue.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class P_JSONUtil
{
    static final Class s_allowedClasses[] =
    {
        boolean.class,
        Boolean.class,
        short.class,
        Short.class,
        int.class,
        Integer.class,
        long.class,
        Long.class,
        float.class,
        Float.class,
        String.class,
        Interval.class
    };

    static final Set<Class> s_allowedClassesSet = new HashSet<>(Arrays.asList(s_allowedClasses));

    // Interface for a class that can extract a field from an object and make it into a JSON compatible object
    public interface JSONExtractor
    {
        Object extract(Object o, Field f);
    }

    // Interface for a class that can take a JSON compatible object and apply it to a field of an object
    public interface JSONApplier
    {
        void apply(Object o, Field f, Object json);
    }

    // Utility methods
    public static JSONObject objectToJSON(Object settingsObject) throws IllegalAccessException, JSONException
    {
        JSONObject jo = new JSONObject();

        //TODO:  Take as input a mapping table of class to json handler

        //TODO:  Allow this to be passed in
        Set<Class> allowedClasses = s_allowedClassesSet;

        Class c = settingsObject.getClass();
        Field[] fields = c.getFields();
        for (Field f : fields)
        {
            int modifiers = f.getModifiers();

            // Skip anything static or final, we only care about mutable instance fields
            if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))
                continue;

            Type t = f.getGenericType();

            // See if the type is supported?
            if (!allowedClasses.contains(t))
                continue;

            if (t == Interval.class)
            {  // Special handling of interval, save it as a double
                Interval i = (Interval)f.get(settingsObject);
                jo.put(f.getName(), i.secs());
            }
            else
                jo.put(f.getName(), f.get(settingsObject));
        }

        return jo;
    }

    public static void applyJSONToObject(Object settingsObject, JSONObject jo) throws IllegalAccessException, JSONException
    {
        //TODO:  Allow this to be passed in
        Set<Class> allowedClasses = s_allowedClassesSet;

        Class c = settingsObject.getClass();
        Field[] fields = c.getFields();
        for (Field f : fields)
        {
            int modifiers = f.getModifiers();

            // Skip anything static or final, we only care about mutable instance fields
            if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))
                continue;

            Type t = f.getGenericType();

            // See if the type is supported?
            if (!allowedClasses.contains(t))
            {
                continue;
            }

            if (t == Interval.class)
            {  // Special handling of interval, save it as a double
                Double val = jo.opt(f.getName()) != null ? jo.getDouble(f.getName()) : null;
                if (val != null)
                {
                    Interval i = Interval.secs(val);
                    f.set(settingsObject, i);
                }
            }
            else
            {
                Object val = jo.opt(f.getName());
                if (val != null)
                    f.set(settingsObject, val);
            }
        }
    }

    /*
    This function determines the difference between two JSON objects
    The return value is a new JSON object
     */
    public static JSONObject shallowDiffJSONObjects(JSONObject jo1, JSONObject jo2) throws JSONException
    {
        JSONObject result = new JSONObject();
        Iterator<String> it = jo2.keys();
        while (it.hasNext())
        {
            String key = it.next();

            // See if the key is missing or the value is different in the other object
            Object val1 = jo1.opt(key);
            Object val2 = jo2.get(key);

            if (val1 == null || !val2.equals(val1))
                result.put(key, val2);
        }

        return result;
    }
}
