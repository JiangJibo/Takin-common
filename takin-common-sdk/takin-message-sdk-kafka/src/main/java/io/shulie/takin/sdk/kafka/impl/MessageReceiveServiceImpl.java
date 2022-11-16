package io.shulie.takin.sdk.kafka.impl;

import cn.chinaunicom.client.ThriftDeserializer;
import cn.chinaunicom.pinpoint.thrift.dto.TStressTestAgentData;
import com.alibaba.fastjson.JSON;
import io.shulie.takin.sdk.kafka.MessageReceiveCallBack;
import io.shulie.takin.sdk.kafka.MessageReceiveService;
import io.shulie.takin.sdk.kafka.entity.MessageEntity;
import io.shulie.takin.sdk.kafka.util.MessageSwitchUtil;
import io.shulie.takin.sdk.kafka.util.PropertiesReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MessageReceiveServiceImpl implements MessageReceiveService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageReceiveServiceImpl.class.getName());

    private KafkaConsumer<String, byte[]> kafkaConsumer;
    private ThriftDeserializer deserializer;
    private Long sleepMills;

    @Override
    public void init() {
        boolean kafkaSwitch = MessageSwitchUtil.userKafkaSwitch();
        if (!kafkaSwitch) {
            LOGGER.warn("kafka开关处理关闭状态，不进行发送初始化");
            return;
        }
        String serverConfig = null;
        String sleepMillStr = null;
        try {
            PropertiesReader propertiesReader = new PropertiesReader("kafka-sdk.properties");
            serverConfig = propertiesReader.getProperty("kafka.sdk.bootstrap");
            //环境变量优先级高于配置文件
            sleepMillStr = System.getProperty("kafka.poll.timeout");
            if (StringUtils.isBlank(sleepMillStr)) {
                sleepMillStr = propertiesReader.getProperty("kafka.poll.timeout", "2000");
            }
            sleepMills = Long.parseLong(sleepMillStr);
        } catch (Exception e) {
            LOGGER.error("读取配置文件失败", e);
        }

        String kafkaSdkBootstrap = System.getProperty("kafka.sdk.bootstrap");
        if (kafkaSdkBootstrap != null) {
            serverConfig = kafkaSdkBootstrap;
        }
        if (serverConfig == null) {
            LOGGER.info("kafka配置serverConfig未找到，不进行kafka发送初始化");
            return;
        }
        LOGGER.info("消息接收获取到地址为:{},超时时间为:{}", serverConfig, sleepMillStr);

        Properties props = new Properties();
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverConfig);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-sdk-consumer");

        kafkaConsumer = new KafkaConsumer<>(props);
        try {
            deserializer = new ThriftDeserializer();
        } catch (TTransportException e) {
            LOGGER.error("初始化反序列化工具失败", e);
        }
    }

    @Override
    public void stop() {
        kafkaConsumer.close();
    }

    @Override
    public void receive(List<String> topics, MessageReceiveCallBack callBack) {
        if (kafkaConsumer == null){
            return;
        }
        kafkaConsumer.subscribe(topics);
        while (true) {
            ConsumerRecords<String, byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(300));
            consumerRecords.forEach(record -> {
                try {
                    byte[] bytes = record.value();
                    if (bytes == null) {
                        callBack.fail("接收到消息为空");
                        return;
                    }
                    TStressTestAgentData tStressTestAgentData = deserializer.deserialize(bytes);
                    MessageEntity messageEntity = new MessageEntity();
                    messageEntity.setHeaders(this.getHeaders(tStressTestAgentData));
                    Map map = JSON.parseObject(tStressTestAgentData.getStringValue(), Map.class);
                    messageEntity.setBody(map);
                    callBack.success(messageEntity);
                } catch (Exception e) {
                    callBack.fail(e.getMessage());
                }
            });

            try {
                Thread.sleep(sleepMills);
            } catch (InterruptedException e) {
                LOGGER.error("休眠出现异常", e);
            }
        }

    }

    private Map<String, Object> getHeaders(TStressTestAgentData tStressTestAgentData) {
        Map<String, Object> headers = new HashMap<>();
        if (tStressTestAgentData != null) {
            headers.put("userAppKey", tStressTestAgentData.getUserAppKey());
            headers.put("tenantAppKey", tStressTestAgentData.getTenantAppKey());
            headers.put("userId", tStressTestAgentData.getUserId());
            headers.put("envCode", tStressTestAgentData.getEnvCode());
            headers.put("agentExpand", tStressTestAgentData.getAgentExpand());
            headers.put("dataType", tStressTestAgentData.getDataType());
            headers.put("hostIp", tStressTestAgentData.getHostIp());
            headers.put("version", tStressTestAgentData.getVersion());
        }
        return headers;

    }
}
