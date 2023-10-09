package it.wldt.adapter.mqtt.physical;

import com.google.gson.Gson;
import it.wldt.adapter.mqtt.physical.exception.MqttPhysicalAdapterConfigurationException;
import it.wldt.adapter.mqtt.physical.topic.incoming.DigitalTwinIncomingTopic;
import it.wldt.adapter.mqtt.physical.topic.incoming.MqttSubscribeFunction;
import it.wldt.adapter.mqtt.physical.utils.ConsoleDigitalAdapter;
import it.wldt.adapter.mqtt.physical.utils.DefaultShadowingFunction;
import it.wldt.adapter.mqtt.physical.utils.MessageDescriptor;

import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.core.engine.WldtEngine;
import it.wldt.core.event.WldtEvent;
import it.wldt.exception.EventBusException;
import it.wldt.exception.ModelException;
import it.wldt.exception.WldtConfigurationException;
import it.wldt.exception.WldtRuntimeException;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TestMain {
    public static void main(String[] args) throws WldtConfigurationException, ModelException, WldtRuntimeException, EventBusException, MqttException, InterruptedException, MqttPhysicalAdapterConfigurationException, IOException {
        WldtEngine dt = new WldtEngine(new DefaultShadowingFunction(), "mqtt-digital-twin");
        ConsoleDigitalAdapter dtAdapter = new ConsoleDigitalAdapter();
        dt.addDigitalAdapter(dtAdapter);

        String filepath = "src/main/resources/config.yml";
        MqttPhysicalAdapterConfiguration config = MqttPhysicalAdapterConfiguration.builder(filepath)
                .readFromConfig()
                //.addIncomingTopic(new DigitalTwinIncomingTopic("sensor/state", getSensorStateFunction()), createIncomingTopicRelatedPropertyList(), new ArrayList<>())
                .build();

//                .addPhysicalAssetPropertyAndTopic("intensity", 0, "sensor/intensity", Integer::parseInt)
//                .addIncomingTopic(new DigitalTwinIncomingTopic("sensor/state", getSensorStateFunction()))
//                .addPhysicalAssetProperty("temperature", 0)
//                .addPhysicalAssetProperty("humidity", 0)
//                .addPhysicalAssetEventAndTopic("overheating", "text/plain", "sensor/overheating", Function.identity())
//                .addPhysicalAssetActionAndTopic("switch-off", "sensor.actuation", "text/plain", "sensor/overheating", actionBody -> "switch" + actionBody)
//      .addPhysicalAssetAction("switch-off", "sensor.actuation", "text/plain");
//      .addOutgoingTopic("switch-off", new ActionOutgoingTopic<String>("sensor/switch", actionEventBody -> "switch-"+actionEventBody));

        dt.addPhysicalAdapter(new MqttPhysicalAdapter("test-mqtt-pa", config));

        dt.startLifeCycle();

        Thread.sleep(2000);

        dtAdapter.invokeAction("switch-off", "off");

    }

    private static List<PhysicalAssetProperty<?>> createIncomingTopicRelatedPropertyList(){
        List<PhysicalAssetProperty<?>> properties = new ArrayList<>();
        properties.add(new PhysicalAssetProperty<>("temperature", 0));
        properties.add(new PhysicalAssetProperty<>("humidity", 0));
        return properties;
    }

    private static MqttSubscribeFunction getSensorStateFunction(){
        return msgPayload -> {
            MessageDescriptor md = new Gson().fromJson(msgPayload, MessageDescriptor.class);
            List<WldtEvent<?>> events = new ArrayList<>();
            try {
                events.add(new PhysicalAssetPropertyWldtEvent<>("temperature", md.getTemperatureValue()));
                events.add(new PhysicalAssetPropertyWldtEvent<>("humidity", md.getHumidityValue()));
            } catch (EventBusException e) {
                e.printStackTrace();
            }
            return events;
        };
    }
}

