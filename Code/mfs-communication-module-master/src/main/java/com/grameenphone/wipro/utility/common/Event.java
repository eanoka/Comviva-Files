package com.grameenphone.wipro.utility.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Event {
	private static ConcurrentHashMap<String, ConcurrentLinkedQueue<Runnable>> eventRegister = new ConcurrentHashMap();

	public static Runnable on(String str, Runnable e) {
		ConcurrentLinkedQueue<Runnable> runnables = eventRegister.get(str);
		if (runnables == null) {
			runnables = new ConcurrentLinkedQueue<>();
			eventRegister.put(str, runnables);
		}
		runnables.add(e);
		return e;
	}

	public static void fire(String str) {
		ConcurrentLinkedQueue<Runnable> runnables = eventRegister.get(str);
		if (runnables != null) {
			for (Runnable run : runnables) {
				run.run();
			}
		}
	}
}