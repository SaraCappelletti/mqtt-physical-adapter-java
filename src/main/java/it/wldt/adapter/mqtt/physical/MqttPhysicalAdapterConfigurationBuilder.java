package it.wldt.adapter.mqtt.physical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
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
        JsonNode properties = configFileContent.get("paProperties");
        JsonNode actions = configFileContent.get("paActions");
        JsonNode events = configFileContent.get("paEvents");
        for (JsonNode p :properties) {
            addProperty(p);
        }
        for (JsonNode a :actions) {
            addAction(a);
        }
        for (JsonNode e :events) {
            addEvent(e);
        }

        return this;
    }


    private void addProperty(JsonNode p) throws MqttPhysicalAdapterConfigurationException {
        String propertyKey = p.get("propertyKey").asText();
        String topic = p.get("topic").asText();
        String type = p.get("type").asText();
        String initialValue = p.get("initialValue").toString();

        if ("int".equals(type)) {
            addPhysicalAssetPropertyAndTopic(propertyKey, Integer.valueOf(initialValue), topic, s -> Integer.valueOf(s));
        }
        else if ("double".equals(type) || "float".equals(type)) {
            addPhysicalAssetPropertyAndTopic(propertyKey, Double.valueOf(initialValue), topic, s -> Double.valueOf(s));
        }
        else if ("boolean".equals(type)) {
            addPhysicalAssetPropertyAndTopic(propertyKey, Boolean.valueOf(initialValue), topic, s -> Boolean.valueOf(s));
        }
        else if ("string".equals(type)) {
            addPhysicalAssetPropertyAndTopic(propertyKey, String.valueOf(initialValue), topic, s -> String.valueOf(s));
        }
        else if ("json-array".equals(type)) {
            addJsonArrayProperty(p.get("field-type").asText(), propertyKey, initialValue, topic);
        }
        else if ("json-object".equals(type)) {
            addJsonObjectProperty(propertyKey, initialValue, topic);
        }
    }

    private void addJsonArrayProperty(String fieldType, String propertyKey, String initialValue, String topic) throws MqttPhysicalAdapterConfigurationException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode initialValuesArray = null;
        try {
            initialValuesArray = objectMapper.readTree(initialValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        addPhysicalAssetPropertyAndTopic(propertyKey, (ArrayNode) initialValuesArray, topic, s -> {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            try {
                List<JsonNode> values = objectMapper.readValue(s, typeFactory
                        .constructCollectionType(List.class, JsonNode.class));
                ArrayNode parsedList = objectMapper.createArrayNode();
                for (JsonNode element : values) {
                    if ("int".equals(fieldType)) {
                        parsedList.add(Integer.valueOf(element.asText()));
                    } else if ("double".equals(fieldType) || "float".equals(fieldType)) {
                        parsedList.add(Double.valueOf(element.asText()));
                    } else if ("boolean".equals(fieldType)) {
                        parsedList.add(Boolean.valueOf(element.asText()));
                    } else if ("string".equals(fieldType)) {
                        parsedList.add(String.valueOf(element.asText()));
                    } else {
                        parsedList.add(element);
                    }
                }
                return parsedList;
            } catch (JsonProcessingException e){
                e.printStackTrace();
                return null;
            }
        });
    }

    private void addJsonObjectProperty(String propertyKey, String initialValue, String topic) throws MqttPhysicalAdapterConfigurationException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode initialValuesObject = objectMapper.createObjectNode();
        try {
            initialValuesObject = objectMapper.readValue(initialValue, ObjectNode.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        addPhysicalAssetPropertyAndTopic(propertyKey, initialValuesObject, topic, s -> {
            ObjectNode parsedValues = objectMapper.createObjectNode();
            try {
                parsedValues = objectMapper.readValue(s, ObjectNode.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return parsedValues;
        });
    }

    private void addAction(JsonNode action) throws MqttPhysicalAdapterConfigurationException {
        String actionKey = action.get("actionKey").asText();
        String type = action.get("type").asText();
        String contentType = action.get("contentType").asText();
        String topic = action.get("topic").asText();
        String actionWord = action.get("action").asText();
        addPhysicalAssetActionAndTopic(actionKey, type, contentType, topic, actionBody -> actionWord + actionBody);

    }

    private void addEvent(JsonNode e) throws MqttPhysicalAdapterConfigurationException {

        String eventKey = e.get("eventKey").asText();
        String type = e.get("type").asText();
        String topic = e.get("topic").asText();
        addPhysicalAssetEventAndTopic(eventKey, type, topic, Function.identity());
    }
        /*if ("int".equals(type)) {
            addPhysicalAssetEventAndTopic(eventKey, type, topic, s -> Integer.valueOf(s));
        }
        else if ("double".equals(type) || "float".equals(type)) {
            addPhysicalAssetEventAndTopic(eventKey, type, topic, s -> Double.valueOf(s));
        }
        else if ("boolean".equals(type)) {
            addPhysicalAssetEventAndTopic(eventKey, type, topic, s -> Boolean.valueOf(s));
        }
        else if ("string".equals(type)) {
            addPhysicalAssetEventAndTopic(eventKey, type, topic, s -> String.valueOf(s));
        }
        else if ("json-array".equals(type)) {
            addJsonArrayEvent(e.get("field-type").asText(), eventKey, topic);
        }
        else if ("json-object".equals(type)) {
            addJsonObjectEvent(eventKey, topic);
        }
    }

    private void addJsonArrayEvent(String fieldType, String eventKey, String topic) throws MqttPhysicalAdapterConfigurationException {
        addPhysicalAssetEventAndTopic(eventKey, "json-array", topic, s -> {
            ObjectMapper objectMapper = new ObjectMapper();
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            try {
                List<JsonNode> values = objectMapper.readValue(s, typeFactory
                        .constructCollectionType(List.class, JsonNode.class));
                ArrayNode parsedList = objectMapper.createArrayNode();
                for (JsonNode element : values) {
                    if ("int".equals(fieldType)) {
                        parsedList.add(Integer.valueOf(element.asText()));
                    } else if ("double".equals(fieldType) || "float".equals(fieldType)) {
                        parsedList.add(Double.valueOf(element.asText()));
                    } else if ("boolean".equals(fieldType)) {
                        parsedList.add(Boolean.valueOf(element.asText()));
                    } else if ("string".equals(fieldType)) {
                        parsedList.add(String.valueOf(element.asText()));
                    } else {
                        parsedList.add(element);
                    }
                }
                return parsedList;
            } catch (JsonProcessingException e){
                e.printStackTrace();
                return null;
            }
        });
    }

    private void addJsonObjectEvent(String eventKey, String topic) throws MqttPhysicalAdapterConfigurationException {
        addPhysicalAssetEventAndTopic(eventKey, "json-object", topic, s -> {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode parsedValues = objectMapper.createObjectNode();
            try {
                parsedValues = objectMapper.readValue(s, ObjectNode.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return parsedValues;
        });
    }*/

}
