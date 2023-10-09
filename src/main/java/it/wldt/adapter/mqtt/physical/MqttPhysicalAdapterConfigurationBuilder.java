package it.wldt.adapter.mqtt.physical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.wldt.adapter.mqtt.physical.exception.MqttPhysicalAdapterConfigurationException;
import it.wldt.adapter.mqtt.physical.topic.MqttTopic;
import it.wldt.adapter.mqtt.physical.topic.incoming.DigitalTwinIncomingTopic;
import it.wldt.adapter.mqtt.physical.topic.incoming.EventIncomingTopic;
import it.wldt.adapter.mqtt.physical.topic.incoming.PropertyIncomingTopic;
import it.wldt.adapter.mqtt.physical.topic.outgoing.ActionOutgoingTopic;
import it.wldt.adapter.mqtt.physical.topic.outgoing.DigitalTwinOutgoingTopic;
import it.wldt.adapter.physical.PhysicalAssetAction;
import it.wldt.adapter.physical.PhysicalAssetEvent;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MqttPhysicalAdapterConfigurationBuilder {

    private final MqttPhysicalAdapterConfiguration configuration;
    private JsonNode configFileContent;
    private final List<PhysicalAssetProperty<?>> properties = new ArrayList<>();
    private final List<PhysicalAssetEvent> events = new ArrayList<>();
    private final List<PhysicalAssetAction> actions = new ArrayList<>();



    public MqttPhysicalAdapterConfigurationBuilder(String brokerAddress, int brokerPort, String clientId) throws MqttPhysicalAdapterConfigurationException {
        if(!isValid(brokerAddress) || !isValid(brokerPort) || !isValid(clientId))
            throw new MqttPhysicalAdapterConfigurationException("Broker Address or Client Id cannot be empty strings or null and Broker Port must be a positive number");
        configuration = new MqttPhysicalAdapterConfiguration(brokerAddress, brokerPort, clientId);
        //configFileContent = null;
    }

    public MqttPhysicalAdapterConfigurationBuilder(String brokerAddress, int brokerPort) throws MqttPhysicalAdapterConfigurationException {
        if(!isValid(brokerAddress) || !isValid(brokerPort))
            throw new MqttPhysicalAdapterConfigurationException("Broker Address cannot be empty strings or null and Broker Port must be a positive number");
        configuration = new MqttPhysicalAdapterConfiguration(brokerAddress, brokerPort);
        //configFileContent = null;
    }

    public MqttPhysicalAdapterConfigurationBuilder(JsonNode fileContent) throws MqttPhysicalAdapterConfigurationException, IOException {
        /*if(!isValid(brokerAddress) || !isValid(brokerPort))
            throw new MqttPhysicalAdapterConfigurationException("Broker Address cannot be empty strings or null and Broker Port must be a positive number");
        */
        configFileContent = fileContent;
        configuration = new MqttPhysicalAdapterConfiguration(getBrokerAddress(), getBrokerPort());
    }

    public <T> MqttPhysicalAdapterConfigurationBuilder addPhysicalAssetPropertyAndTopic(String propertyKey, T initialValue, String topic, Function<String, T> topicFunction) throws MqttPhysicalAdapterConfigurationException {
        checkTopicAndFunction(topic, topicFunction, this.configuration.getIncomingTopics().stream().map(MqttTopic::getTopic).collect(Collectors.toList()));
        configuration.addIncomingTopic(new PropertyIncomingTopic<>(topic, propertyKey, topicFunction));
        return addPhysicalAssetProperty(propertyKey, initialValue);
    }

    public <T> MqttPhysicalAdapterConfigurationBuilder addPhysicalAssetActionAndTopic(String actionKey, String type, String contentType,
                                                                                      String topic, Function<T, String> topicFunction) throws MqttPhysicalAdapterConfigurationException {
        checkTopicAndFunction(topic, topicFunction, this.configuration.getOutgoingTopics().values().stream().map(MqttTopic::getTopic).collect(Collectors.toList()));
        configuration.addOutgoingTopic(actionKey, new ActionOutgoingTopic<>(topic, topicFunction));
        return addPhysicalAssetAction(actionKey, type, contentType);
    }

    public <T> MqttPhysicalAdapterConfigurationBuilder addPhysicalAssetEventAndTopic(String eventKey, String type, String topic, Function<String, T> topicFunction) throws MqttPhysicalAdapterConfigurationException {
        checkTopicAndFunction(topic, topicFunction, this.configuration.getIncomingTopics().stream().map(MqttTopic::getTopic).collect(Collectors.toList()));
        configuration.addIncomingTopic(new EventIncomingTopic<>(topic, eventKey, topicFunction));
        return addPhysicalAssetEvent(eventKey, type);

    }

    public MqttPhysicalAdapterConfigurationBuilder addIncomingTopic(DigitalTwinIncomingTopic topic, List<PhysicalAssetProperty<?>> properties, List<PhysicalAssetEvent> events) throws MqttPhysicalAdapterConfigurationException {
        if(topic == null) throw new MqttPhysicalAdapterConfigurationException("DigitalTwinIncomingTopic cannot be null");
        if(!isValid(properties) && !isValid(events)) throw new MqttPhysicalAdapterConfigurationException("Property and event list cannot be null or empty. For each DigitalTwinIncomingTopic, related properties and events must be specified");
        checkTopicAndFunction(topic.getTopic(), topic.getSubscribeFunction(), this.configuration.getIncomingTopics().stream().map(MqttTopic::getTopic).collect(Collectors.toList()));
        this.properties.addAll(properties);
        this.events.addAll(events);
        configuration.addIncomingTopic(topic);
        return this;
    }

    public MqttPhysicalAdapterConfigurationBuilder addOutgoingTopic(String actionKey,  String type, String contentType, DigitalTwinOutgoingTopic topic) throws MqttPhysicalAdapterConfigurationException {
        if(topic == null || isValid(actionKey)) throw new MqttPhysicalAdapterConfigurationException("DigitalTwinOutgoingTopic cannot be null | Action key cannot be empty string or null");
        checkTopicAndFunction(topic.getTopic(), topic.getPublishFunction(), this.configuration.getOutgoingTopics().values().stream().map(MqttTopic::getTopic).collect(Collectors.toList()));
        configuration.addOutgoingTopic(actionKey, topic);
        return addPhysicalAssetAction(actionKey, type, contentType);
    }

    private <T> MqttPhysicalAdapterConfigurationBuilder addPhysicalAssetProperty(String key, T initValue){
        this.properties.add(new PhysicalAssetProperty<>(key, initValue));
        return this;
    }

    private MqttPhysicalAdapterConfigurationBuilder addPhysicalAssetAction(String key, String type, String contentType){
        this.actions.add(new PhysicalAssetAction(key, type, contentType));
        return this;
    }

    private MqttPhysicalAdapterConfigurationBuilder addPhysicalAssetEvent(String key, String type){
        this.events.add(new PhysicalAssetEvent(key, type));
        return this;
    }

    public MqttPhysicalAdapterConfigurationBuilder setConnectionTimeout(Integer connectionTimeout) throws MqttPhysicalAdapterConfigurationException {
        if(!isValid(connectionTimeout)) throw new MqttPhysicalAdapterConfigurationException("Connection Timeout must be a positive number");
        this.configuration.setConnectionTimeout(connectionTimeout);
        return this;
    }

    public MqttPhysicalAdapterConfigurationBuilder setCleanSessionFlag(boolean cleanSession) {
        this.configuration.setCleanSessionFlag(cleanSession);
        return this;
    }

    public MqttPhysicalAdapterConfigurationBuilder setAutomaticReconnectFlag(boolean automaticReconnect){
        this.configuration.setAutomaticReconnectFlag(automaticReconnect);
        return this;
    }

    public MqttPhysicalAdapterConfigurationBuilder setMqttClientPersistence(MqttClientPersistence persistence) throws MqttPhysicalAdapterConfigurationException {
        if(persistence == null) throw new MqttPhysicalAdapterConfigurationException("MqttClientPersistence cannot be null");
        this.configuration.setMqttClientPersistence(persistence);
        return this;
    }

    public MqttPhysicalAdapterConfiguration build() throws MqttPhysicalAdapterConfigurationException {
        if(properties.isEmpty() && actions.isEmpty() && events.isEmpty())
            throw new MqttPhysicalAdapterConfigurationException("Physical Adapter must have at least one property or event or action");
        if(this.configuration.getIncomingTopics().isEmpty() && this.configuration.getOutgoingTopics().isEmpty())
            throw new MqttPhysicalAdapterConfigurationException("MQTT Physical Adapter must define at least one DigitalTwinIncomingTopic or DigitalTwinOutgoingTopic");
        this.configuration.setPhysicalAssetDescription(actions, properties, events);
        return this.configuration;
    }

    private <I, O> void checkTopicAndFunction(String topic, Function<I, O> topicFunction, List<String> topicList) throws MqttPhysicalAdapterConfigurationException {
        if(!isValid(topic) || topicFunction == null)
            throw new MqttPhysicalAdapterConfigurationException("topic cannot be empty or null | topic function cannot be null");
        if(topicList.contains(topic))
            throw new MqttPhysicalAdapterConfigurationException("topic already defined");
    }

    private <T> boolean isValid(List<T> list){
        return list != null && !list.isEmpty();
    }

    private boolean isValid(String param){
        return param != null && !param.isEmpty();
    }

    private boolean isValid(int param){
        return param > 0;
    }

    /*private static JsonNode readConfigFile(String filepath) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        return yamlMapper.readTree(new File(filepath));
    }*/

    private String getBrokerAddress() {
        return configFileContent.get("brokerAddress").asText();
    }

    private int getBrokerPort() {
        return configFileContent.get("brokerPort").asInt();
    }

    public MqttPhysicalAdapterConfigurationBuilder readFromConfig() throws MqttPhysicalAdapterConfigurationException, IOException {
        addProperties(configFileContent.get("paProperties"));

        addPhysicalAssetActionAndTopic("switch-off", "sensor.actuation", "text/plain", "sensor/actions/switch", actionBody -> "switch" + actionBody);
        //addPhysicalAssetPropertyAndTopic("intensity", 0, "sensor/intensity", Integer::parseInt);
        //String propertyKey, T initialValue, String topic, Function<String, T> topicFunction
        addPhysicalAssetEventAndTopic("overheating", "text/plain", "sensor/overheating", Function.identity());

        return this;
    }

    private <T> void addProperties(JsonNode properties) throws MqttPhysicalAdapterConfigurationException {
        for (JsonNode p :properties) {
            String propertyKey = p.get("propertyKey").asText();
            T initialValue = (T) parseField(p.get("initialValue")).apply(p.get("initialValue").asText());
            String topic = p.get("topic").asText();
            System.out.println(initialValue);
            //TODO i'm ignoring type field
            addPhysicalAssetPropertyAndTopic(propertyKey, initialValue, topic, parseField(p.get("initialValue")));
        }
    }

    public <T> Function<String, T> parseField(JsonNode field) {
        return String -> {
            if (field instanceof IntNode) {
                return (T) Integer.valueOf(field.asInt());
            }
            else if (field instanceof DoubleNode) {
                return (T) Double.valueOf((float) field.asDouble());
            }
            else if (field instanceof FloatNode) {
                return (T) Float.valueOf((float) field.asDouble());
            }
            else if (field instanceof BooleanNode) {
                return (T) Boolean.valueOf(field.asBoolean());
            }
            else if (field instanceof TextNode) {
                return (T) String.valueOf(field.asText());
            }
            else if (field instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) field;
                List<T> parsedList = new ArrayList<>();
                for (JsonNode element : arrayNode) {
                    parsedList.add((T) parseField(element).apply(element.asText()));
                }
                return (T) parsedList;
            }
            return (T) Function.identity();
        };
    }

}
