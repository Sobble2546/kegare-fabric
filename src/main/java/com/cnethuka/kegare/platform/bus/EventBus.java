package com.cnethuka.kegare.platform.bus;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Thread-safe listener registry. Listeners run on the posting thread. */
public final class EventBus implements IEventBus {
    private record Entry(Class<? extends Event> type, EventPriority priority, boolean receiveCanceled, Consumer<Event> listener, Object owner) {}

    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, List<Entry>> dispatch = new ConcurrentHashMap<>();

    @Override
    public <T extends Event> void addListener(Listener<T> listener) {
        addListener(EventPriority.NORMAL, false, listener);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, Listener<T> listener) {
        addListener(priority, false, listener);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Listener<T> listener) {
        add(eventTypeOf(listener), priority, receiveCanceled, listener, listener);
    }

    @Override
    public <T extends Event> void addListener(Class<T> type, Consumer<T> listener) {
        add(type, EventPriority.NORMAL, false, listener, listener);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> listener) {
        add(type, priority, receiveCanceled, listener, listener);
    }

    @SuppressWarnings("unchecked")
    private void add(Class<? extends Event> type, EventPriority priority, boolean receiveCanceled, Consumer<?> listener, Object owner) {
        entries.add(new Entry(type, priority, receiveCanceled, (Consumer<Event>) listener, owner));
        dispatch.clear();
    }

    @Override
    public void register(Object target) {
        boolean staticOnly = target instanceof Class<?>;
        Class<?> cls = staticOnly ? (Class<?>) target : target.getClass();
        for (Method method : cls.getDeclaredMethods()) {
            SubscribeEvent sub = method.getAnnotation(SubscribeEvent.class);
            if (sub == null) continue;
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            if (staticOnly && !isStatic) continue;
            if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new IllegalArgumentException("@SubscribeEvent method must take one Event: " + method);
            }
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<? extends Event> type = (Class<? extends Event>) method.getParameterTypes()[0];
            Consumer<Event> call = event -> {
                try {
                    method.invoke(isStatic ? null : target, event);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw sneaky(e.getCause());
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            };
            entries.add(new Entry(type, sub.priority(), sub.receiveCanceled(), call, target));
        }
        dispatch.clear();
    }

    @Override
    public void unregister(Object target) {
        entries.removeIf(e -> e.owner == target);
        dispatch.clear();
    }

    @Override
    public <T extends Event> T post(T event) {
        boolean cancellable = event instanceof ICancellableEvent;
        for (Entry entry : listenersFor(event.getClass())) {
            if (cancellable && event.canceledFlag() && !entry.receiveCanceled) continue;
            entry.listener.accept(event);
        }
        return event;
    }

    private List<Entry> listenersFor(Class<?> eventClass) {
        return dispatch.computeIfAbsent(eventClass, c -> {
            List<Entry> out = new ArrayList<>();
            for (Entry e : entries) if (e.type.isAssignableFrom(c)) out.add(e);
            out.sort(Comparator.comparing(Entry::priority)); // stable: registration order within a priority
            return out;
        });
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneaky(Throwable t) throws E {
        throw (E) t;
    }

    /** The event type a lambda or method reference handles, read from its (instantiated) method type. */
    @SuppressWarnings("unchecked")
    static Class<? extends Event> eventTypeOf(Listener<?> listener) {
        try {
            Method writeReplace = listener.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(listener);
            String desc = lambda.getInstantiatedMethodType(); // (Lcom/x/SomeEvent;)V
            String name = desc.substring(2, desc.indexOf(';')).replace('/', '.');
            ClassLoader loader = listener.getClass().getClassLoader();
            return (Class<? extends Event>) Class.forName(name, false, loader);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalArgumentException("Can't tell which event this listener handles; pass the event class: " + listener, e);
        }
    }
}
