package it.wldt.adapter.mqtt.physical;

import it.wldt.adapter.mqtt.physical.exception.MqttPhysicalAdapterConfigurationException;
import it.wldt.adapter.mqtt.physical.topic.incoming.DigitalTwinIncomingTopic;

import java.util.ArrayList;
import java.util.function.Function;

public class MqttPhysicalAdapterBuilder {

    private final String filename = "";
    private final MqttPhysicalAdapterConfigurationBuilder configuration;
    private final String brokerAddress = "127.0.0.1";
    private final Integer brokerPort = 1883;

    public MqttPhysicalAdapterBuilder(String filename) throws MqttPhysicalAdapterConfigurationException {
        filename = filename;
        configuration = new MqttPhysicalAdapterConfigurationBuilder(brokerAddress, brokerPort);
    }

    public MqttPhysicalAdapterConfigurationBuilder getBuilder(){
        return configuration;
    }

    public MqttPhysicalAdapterConfigurationBuilder readFromConfig() throws MqttPhysicalAdapterConfigurationException {
        configuration.addPhysicalAssetActionAndTopic("switch-off", "sensor.actuation", "text/plain", "sensor/actions/switch", actionBody -> "switch" + actionBody)
                .addPhysicalAssetPropertyAndTopic("intensity", 0, "sensor/intensity", Integer::parseInt)
                .addPhysicalAssetEventAndTopic("overheating", "text/plain", "sensor/overheating", Function.identity());

        return configuration;
    }
}
