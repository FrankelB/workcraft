package org.workcraft.plugins.fst;

import org.workcraft.exceptions.ArgumentException;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.fsm.Symbol;

public class Signal extends Symbol {

	public enum Type {
		INPUT("input"),
		OUTPUT("output"),
		INTERNAL("internal"),
		DUMMY("dummy");

		private final String name;

		private Type(String name) {
			this.name = name;
		}

		public static Type fromString(String s) {
			for (Type item : Type.values()) {
				if ((s != null) && (s.equals(item.name))) {
					return item;
				}
			}
			throw new ArgumentException ("Unexpected string: " + s);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private Type type = Type.DUMMY;

	public Signal() {
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
		sendNotification(new PropertyChangedEvent(this, "type"));
	}

	public boolean hasDirection() {
		return (getType() != Type.DUMMY);
	}

}
