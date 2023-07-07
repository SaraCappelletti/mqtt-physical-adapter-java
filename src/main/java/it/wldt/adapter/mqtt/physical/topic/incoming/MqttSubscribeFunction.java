package it.wldt.adapter.mqtt.physical.topic.incoming;


import it.wldt.core.event.WldtEvent;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
/**
 * Represents a function that accepts a message published on topic (of type String) and when applied, produces a WldtEvent.
 * */
public interface MqttSubscribeFunction extends Function<String, List<WldtEvent<?>>> {
}
