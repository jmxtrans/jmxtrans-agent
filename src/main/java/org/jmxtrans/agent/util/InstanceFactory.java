package org.jmxtrans.agent.util;

public class InstanceFactory<T> {

	@SuppressWarnings("unchecked")
	public T newInstanceOf(String className) {
		T instance = null;
		try {
		    instance = (T) Class.forName(className).newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Exception instantiating " + className, e);
		}
		return instance;
	}
}
