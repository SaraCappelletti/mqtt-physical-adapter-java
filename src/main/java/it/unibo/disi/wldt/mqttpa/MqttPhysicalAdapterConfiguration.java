package it.unibo.disi.wldt.mqttpa;

import it.unibo.disi.wldt.mqttpa.exception.MqttPhysicalAdapterConfigurationException;
import it.unibo.disi.wldt.mqttpa.topic.incoming.DigitalTwinIncomingTopic;
import it.unibo.disi.wldt.mqttpa.topic.incoming.EventIncomingTopic;
import it.unibo.disi.wldt.mqttpa.topic.incoming.PropertyIncomingTopic;
import it.unibo.disi.wldt.mqttpa.topic.outgoing.ActionOutgoingTopic;
import it.unibo.disi.wldt.mqttpa.topic.outgoing.DigitalTwinOutgoingTopic;
import it.unibo.disi.wldt.mqttpa.topic.outgoing.MqttPublishFunction;
import it.unimore.dipi.iot.wldt.adapter.physical.PhysicalAssetAction;
import it.unimore.dipi.iot.wldt.adapter.physical.PhysicalAssetDescription;
import it.unimore.dipi.iot.wldt.adapter.physical.PhysicalAssetEvent;
import it.unimore.dipi.iot.wldt.adapter.physical.PhysicalAssetProperty;
import it.unimore.dipi.iot.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.*;
import java.util.function.Function;

public class MqttPhysicalAdapterConfiguration {
    private final String brokerAddress;
    private final Integer brokerPort;
    private String username;
    private String password;
    private String clientId;
    private boolean cleanSessionFlag = true;
    private Integer connectionTimeout = 10;
    private MqttClientPersistence persistence = new MemoryPersistence();
    private boolean automaticReconnectFlag = true;


    private PhysicalAssetDescription physicalAssetDescription;

    //INCOMING TOPICS: Topics to which the PhysicalAdapter must subscribe
    private final List<DigitalTwinIncomingTopic> incomingTopics = new ArrayList<>();
    //OUTGOING TOPICS: Topics on which the PhysicalAdapter must publish
    private final Map<String, DigitalTwinOutgoingTopic> outgoingTopics = new HashMap<>();

    protected MqttPhysicalAdapterConfiguration(String brokerAddress, Integer brokerPort, String clientId) {
        this.brokerAddress = brokerAddress;
        this.brokerPort = brokerPort;
        this.clientId = clientId;
    }

    protected MqttPhysicalAdapterConfiguration(String brokerAddress, Integer brokerPort){
        this(brokerAddress, brokerPort, "wldt.mqtt.client."+new Random(System.currentTimeMillis()).nextInt());
    }

    public static MqttPhysicalAdapterConfigurationBuilder builder(String brokerAddress, Integer brokerPort, String clientId) throws MqttPhysicalAdapterConfigurationException {
        return new MqttPhysicalAdapterConfigurationBuilder(brokerAddress, brokerPort, clientId);
    }

    public static MqttPhysicalAdapterConfigurationBuilder builder(String brokerAddress, Integer brokerPort) throws MqttPhysicalAdapterConfigurationException {
        return new MqttPhysicalAdapterConfigurationBuilder(brokerAddress, brokerPort);
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public Integer getBrokerPort() {
        return brokerPort;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getClientId() {
        return clientId;
    }

    public String getBrokerConnectionString(){
        return String.format("tcp://%s:%d", brokerAddress, brokerPort);
    }

    public MqttClientPersistence getPersistence() {
        return persistence;
    }

    public MqttConnectOptions getConnectOptions(){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(automaticReconnectFlag);
        options.setCleanSession(cleanSessionFlag);
        options.setConnectionTimeout(connectionTimeout);
        if(username != null && !username.isEmpty() && password != null && !password.isEmpty()){
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
        return options;
    }

    public List<DigitalTwinIncomingTopic> getIncomingTopics() {
        return incomingTopics;
    }

    public Map<String, DigitalTwinOutgoingTopic> getOutgoingTopics() {
        return outgoingTopics;
    }

    public Optional<DigitalTwinOutgoingTopic> getOutgoingTopicByActionKey(String key){
        return outgoingTopics.containsKey(key) ? Optional.of(outgoingTopics.get(key)) : Optional.empty();
    }

    public PhysicalAssetDescription getPhysicalAssetDescription() {
        return physicalAssetDescription;
    }

    protected void addIncomingTopic(DigitalTwinIncomingTopic topic){
        this.incomingTopics.add(topic);
    }

    protected void addOutgoingTopic(String actionKey, DigitalTwinOutgoingTopic topic){
        this.outgoingTopics.put(actionKey,topic);
    }

    protected void setPhysicalAssetDescription(List<PhysicalAssetAction> actions,
                                            List<PhysicalAssetProperty<?>> properties,
                                            List<PhysicalAssetEvent> events){
        this.physicalAssetDescription = new PhysicalAssetDescription(actions, properties, events);
    }

    protected void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    protected void setCleanSessionFlag(boolean cleanSession) {
        this.cleanSessionFlag = cleanSession;
    }

    protected void setAutomaticReconnectFlag(boolean automaticReconnect){
        this.automaticReconnectFlag = automaticReconnect;
    }

    protected void setMqttClientPersistence(MqttClientPersistence persistence) {
        this.persistence = persistence;
    }

}

