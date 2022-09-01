package com.coldnorth.homeautomations.utils


import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlin.text.Charsets.UTF_8





object NetworkUtils {


    private const val HOST = "broker.emqx.io"
    private const val PORT = 1883
    private const val USERNAME = "emqx"
    private const val PASSWORD = "public"

    const val APP_TOPIC = "diplomatikiuth/autohome/app"
    const val SERVER_TOPIC = "diplomatikiuth/autohome/server"
    const val DEVICE_TOPIC = "diplomatikiuth/autohome/devices"

    private lateinit var  client:Mqtt5BlockingClient

    fun connect(){
        client = MqttClient.builder()
            .useMqttVersion5()
            .serverHost(HOST)
            .serverPort(PORT)
            .buildBlocking()

        client.connectWith()
            .simpleAuth()
            .username(USERNAME)
            .password(UTF_8.encode(PASSWORD))
            .applySimpleAuth()
            .send();
    }


    fun unsubscribe(topic:String){
        client.unsubscribeWith().addTopicFilter(topic).send()
    }

    fun subscribe(topic:String, onconnect: (publish: Mqtt5Publish)-> Unit ){
        client.subscribeWith().topicFilter(topic).send()
        client.toAsync().publishes(MqttGlobalPublishFilter.ALL,onconnect)

    }

    fun publish(topic:String, msg: String){
        client.publishWith().topic(topic).payload(UTF_8.encode(msg)).send()
    }


}
