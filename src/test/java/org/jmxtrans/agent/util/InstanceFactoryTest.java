package org.jmxtrans.agent.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class InstanceFactoryTest {

	private final InstanceFactory<String> stringInstanceFactory = new InstanceFactory<String>();
	
	@Test
	public void stringInstanceFactoryReturnsInstancesOfString() {
		assertThat(stringInstanceFactory.newInstanceOf("java.lang.String"), is(instanceOf(String.class)));
	}
	
	@Test
	public void newInstanceOfStringIsReturnedEveryTime() {
		String stringA = stringInstanceFactory.newInstanceOf("java.lang.String");
		String stringB = stringInstanceFactory.newInstanceOf("java.lang.String");
		assertThat(stringA, is(not(sameInstance(stringB))));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void illegalArgumentExceptionIsThrownForClassThatDoesNotExist() {
		stringInstanceFactory.newInstanceOf("does.not.Exist");
	}

}
