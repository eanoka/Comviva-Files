package com.grameenphone.wipro.utility.common;

import com.grameenphone.wipro.constants.EventScopes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * To make some inter modular bridge this class will act as a event listener and trigger
 * one module will fire an event while other module that should be pre registered will be in listening and those will be triggered accordingly
 * Event also have namespace support. namespace is useful when it is required to unregister all event for a specific category. e.g. some session specific event and when a session will be invalidated then all the event related to that session can be unregistered if using a common namespace like session id
 */
public class Event {
	private static Map<String, List<EventEntry>> events = new ConcurrentHashMap();
	private final static Logger logger = LoggerFactory.getLogger(Event.class);
	
	private static class EventEntry {
		public int count;
		public Consumer handler; 
		public List<String> namespaces;
		
		public EventEntry(int count, Consumer handler, List<String> namespaces) {
			this.count = count;
			this.handler = handler;
			this.namespaces = namespaces;
		}
	}
	
	private static class RunnableConsumer implements Consumer {
		private Runnable runnable;
		
		public RunnableConsumer(Runnable runnable) {
			this.runnable = runnable;
		}
		
		@Override
		public void accept(Object o) {
			runnable.run();
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof RunnableConsumer ? runnable.equals(((RunnableConsumer) obj).runnable) : super.equals(obj);
		}
	}

	public static void off(String name) {
		off(name, null, (Consumer) null);
	}

	public static void on(String name, Consumer handler) {
		on(name, -1, handler);
	}

	public static void async(String name, Consumer handler) {
		on(name, -1, (c) -> {
			try {
				Thread.sleep(5000); //Let the object be persist in db
			} catch (InterruptedException ignored) {
			}
			new Thread(() -> {
				try {
					handler.accept(c);
				} catch (Throwable p) {
					logger.error("Event Execution Failed: " + name + " (" + c + ")", p);
				}
			}).start();
		});
	}

	public static void one(String name, Consumer handler) {
		on(name, 1, handler);
	}

	public static void off(String name, Consumer handler) {
		off(name, null, handler);
	}

	public static void on(String name, Runnable handler) {
		on(name, -1, new RunnableConsumer(handler));
	}

	public static void one(String name, Runnable handler) {
		on(name, 1, new RunnableConsumer(handler));
	}

	public static void off(String name, Runnable handler) {
		off(name, null, new RunnableConsumer(handler));
	}

	public static void on(String name, String namespace, Consumer handler) {
		on(name, new ArrayList<String>() {{
			add(namespace);
		}}, -1, handler);
	}

	public static void one(String name, String namespace, Consumer handler) {
		on(name, new ArrayList<String>() {{
			add(namespace);
		}}, 1, handler);
	}

	public static void on(String name, String namespace, Runnable handler) {
		on(name, new ArrayList<String>() {{
			add(namespace);
		}}, -1, new RunnableConsumer(handler));
	}

	public static void one(String name, String namespace, Runnable handler) {
		on(name, new ArrayList<String>() {{
			add(namespace);
		}}, 1, new RunnableConsumer(handler));
	}

	public static void on(String name, List<String> namespaces, Consumer handler) {
		on(name, namespaces, -1, handler);
	}

	public static void one(String name, List<String> namespaces, Consumer handler) {
		on(name, namespaces, 1, handler);
	}

	public static void on(String name, List<String> namespaces, Runnable handler) {
		on(name, namespaces, -1, new RunnableConsumer(handler));
	}

	public static void one(String name, List<String> namespaces, Runnable handler) {
		on(name, namespaces, 1, new RunnableConsumer(handler));
	}

	public static void on(String name, int repeatCount, Consumer handler) {
		on(name, new ArrayList<>(), repeatCount, handler);
	}

	public static void on(String name, int repeatCount, Runnable handler) {
		on(name, new ArrayList<>(), repeatCount, new RunnableConsumer(handler));
	}

	public static void off(String name, String namespace) {
		off(name, namespace, (Consumer) null);
	}

	/**
	 * Having * as name will remove all event as per given namespace and handler
	 * @param name
	 * @param namespace
	 * @param handler
	 */
	public static void off(String name, String namespace, Consumer handler) {
		if(namespace != null && namespace.equals(EventScopes.SESSION)) {
			if((namespace = SessionAttributes.current().ID) == null) {
				return;
			}
		}
		if (name == "*") {
			Collection<String> names = events.keySet();
			for (String ename : names) {
				off(ename, namespace, handler);
			}
			return;
		}
		List<EventEntry> registereds = events.get(name);
		if (registereds == null) {
			return;
		}
		if (namespace == null) {
			if (handler != null) {
				registereds.removeIf(it -> it.handler.equals(handler));
			} else {
				events.remove(name);
				return;
			}
		} else {
			String _namespace = namespace;
			registereds.removeIf(it -> it.namespaces.contains(_namespace) && (handler == null || it.handler.equals(handler)));
		}
		if(registereds.size() == 0) {
			events.remove(name);
		}
	}

	public static void off(String name, String namespace, Runnable handler) {
		off(name, namespace, new RunnableConsumer(handler));
	}

	public static void on(String name, List<String> namespaces, int fireCount, Consumer handler) {
		if(namespaces.size() > 0 && namespaces.contains(EventScopes.SESSION)) {
			int index = namespaces.indexOf(EventScopes.SESSION);
			namespaces.set(index, SessionAttributes.current().ID);
		}
		name = name.trim();
		if (name == "") {
			return;
		}
		String[] names = name.split(" ");
		if (names.length > 1) {
			int _fireCount = fireCount;
			Arrays.stream(names).forEach(it -> on(it, namespaces, _fireCount, handler));
			return;
		}
		if (fireCount < 1) {
			fireCount = -1;
		}
		List registereds = events.get(name);
		if (registereds == null) {
			events.put(name, registereds = new CopyOnWriteArrayList());
		}
		EventEntry event = new EventEntry(fireCount, handler, namespaces);
		registereds.add(event);
	}

	public static void on(String name, String namespace, int fireCount, Consumer handler) {
		on(name, new ArrayList<>() {{
			if(namespace != null) {
				add(namespace);
			}
		}}, fireCount, handler);
	}

	public static void on(String name, List namespaces, int fireCount, Runnable handler) {
		on(name, namespaces, fireCount, new RunnableConsumer(handler));
	}

	public static void on(String name, String namespace, int fireCount, Runnable handler) {
		on(name, new ArrayList<>() {{
			if(namespace != null) {
				add(namespace);
			}
		}}, fireCount, new RunnableConsumer(handler));
	}

	public static void fire(String name, String namespace, Object... eventData) {
		if(namespace != null && namespace.equals(EventScopes.SESSION)) {
			if((namespace = SessionAttributes.current().ID) == null) {
				return;
			}
		}
		List<EventEntry> registereds = events.get(name);
		if (registereds == null) {
			return;
		}
		List<EventEntry> eventsToRemove = new ArrayList<>();
		String _namespace = namespace;
		registereds.stream().filter(it -> _namespace == null ? true : it.namespaces.contains(_namespace)).forEach(event -> {
			if (event.count > -1) {
				event.count--;
				if (event.count == 0) {
					eventsToRemove.add(event);
				}
			}
			Object sendData = eventData.length == 0 ? null : (eventData.length == 1 ? eventData[0] : eventData);
			event.handler.accept(sendData);
		});
		registereds.removeAll(eventsToRemove); // All events that reached to count 0
		if(registereds.size() == 0) {
			events.remove(name);
		}
	}

	public static void fire(String name, Object... eventData) {
		fire(name, null, eventData);
	}

	public static void after(long ms, String name, Runnable handler) {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				fire(name);
			}
		};
		off(name);
		one(name, handler);
		new Timer(false).schedule(task, ms);
	}
}