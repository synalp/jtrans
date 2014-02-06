package fr.loria.synalp.jtrans.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Assert;

/**
 * Provides access to private members in classes.
 */
public class PrivateAccess {

	public static Object callPrivateMethod (Object o, String methodName, Object[] args) {
		/* Check we have valid arguments */
		Assert.assertNotNull(o);
		Assert.assertNotNull(methodName);
		final Method methods[] = o.getClass().getDeclaredMethods();
		for (int i=0;i<methods.length;i++) {
			if (methodName.equals(methods[i].getName())) {
				try {
					methods[i].setAccessible(true);
					Object res = methods[i].invoke(o, args);
					return res;
				} catch (IllegalAccessException ex) {
					Assert.fail ("IllegalAccessException accessing " + methodName);
				} catch (IllegalArgumentException e) {
					Assert.fail ("IllegalArgumentException accessing " + methodName);
				} catch (InvocationTargetException e) {
					Assert.fail ("InvocationTargetException accessing " + methodName);
				}
				return null;
			}
		}
		return null;
	}
	
	public static Object callPrivateStaticMethod (Class o, String methodName, Object[] args) {
		/* Check we have valid arguments */
		Assert.assertNotNull(o);
		Assert.assertNotNull(methodName);
		final Method methods[] = o.getDeclaredMethods();
		for (int i=0;i<methods.length;i++) {
			if (methodName.equals(methods[i].getName())) {
				try {
					methods[i].setAccessible(true);
					Object res = methods[i].invoke(o, args);
					return res;
				} catch (IllegalAccessException ex) {
					Assert.fail ("IllegalAccessException accessing " + methodName);
				} catch (IllegalArgumentException e) {
					Assert.fail ("IllegalArgumentException accessing " + methodName);
				} catch (InvocationTargetException e) {
					e.getCause().printStackTrace();
					Assert.fail ("InvocationTargetException accessing " + methodName);
				}
				return null;
			}
		}
		return null;
	}

	public static Object getPrivateField (Object o, String fieldName) {
		/* Check we have valid arguments */
		Assert.assertNotNull(o);
		Assert.assertNotNull(fieldName);
		/* Go and find the private field... */
		final Field fields[] = o.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; ++i) {
			if (fieldName.equals(fields[i].getName())) {
				try {
					fields[i].setAccessible(true);
					return fields[i].get(o);
				} catch (IllegalAccessException ex) {
					Assert.fail ("IllegalAccessException accessing " + fieldName);
				}
			}
		}
		Assert.fail ("Field '" + fieldName + "' not found");
		return null;
	}
}
