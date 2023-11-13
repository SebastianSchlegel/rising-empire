package com.scheible.risingempire.util.jdk;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sj
 */
class Objects2Test {

	private static class TestValueObject {

		private final String stringValue;

		private final int intValue;

		public TestValueObject(String stringValue, int intValue) {
			this.stringValue = stringValue;
			this.intValue = intValue;
		}

		@Override
		public boolean equals(Object obj) {
			return Objects2.equals(this, obj, other -> Objects.equals(stringValue, other.stringValue)
					&& Objects.equals(intValue, other.intValue));
		}

		@Override
		public int hashCode() {
			return Objects.hash(stringValue, intValue);
		}

	}

	@Test
	void testEqualsAndHashCode() {
		assertThat(new TestValueObject(":-)", 42)).isEqualTo(new TestValueObject(":-)", 42));
		assertThat(new TestValueObject(":-)", 42).hashCode()).isEqualTo(new TestValueObject(":-)", 42).hashCode());
	}

}
