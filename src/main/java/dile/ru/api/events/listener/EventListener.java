package dile.ru.api.events.listener;

import dile.ru.api.events.Event;

@FunctionalInterface
public interface EventListener<T extends Event> {
    void invoke(T event) throws Exception;
}
