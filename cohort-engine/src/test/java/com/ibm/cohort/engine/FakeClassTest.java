package com.ibm.cohort.engine;


import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

public class FakeClassTest {
	
	@Test
	public void testGettingTheInt() {
		FakeClass fc = new FakeClass();
		assertEquals(1, fc.getAnInt());
	}
}
