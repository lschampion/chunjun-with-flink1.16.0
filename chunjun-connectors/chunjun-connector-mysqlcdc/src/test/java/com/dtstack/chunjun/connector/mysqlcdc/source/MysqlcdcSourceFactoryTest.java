package com.dtstack.chunjun.connector.mysqlcdc.source;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.config.MySqlSourceConfigFactory;
import com.ververica.cdc.connectors.mysql.table.MySqlReadableMetadata;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import com.ververica.cdc.debezium.table.MetadataConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** @PROJECT_NAME: chunjun-1.16 @DESCRIPTION: @USER: lisai @DATE: 2022/11/9 3:37 下午 */
class MysqlcdcSourceFactoryTest {

    public static void main(String[] args) throws Exception {
        Logger LOG = LoggerFactory.getLogger(MysqlcdcSourceFactory.class);

        LocalStreamEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();

        ArrayList<MetadataConverter> metadataConverterArrayList = new ArrayList<>();
        metadataConverterArrayList.add(MySqlReadableMetadata.DATABASE_NAME.getConverter());
        metadataConverterArrayList.add(MySqlReadableMetadata.TABLE_NAME.getConverter());
        metadataConverterArrayList.add(MySqlReadableMetadata.OP_TS.getConverter());

        MySqlSource<String> mySqlSource =
                MySqlSource.<String>builder()
                        .serverTimeZone("Asia/Shanghai")
                        .hostname("localhost")
                        .port(3306)
                        .databaseList("test")
                        .tableList("test.student")
                        .username("root")
                        .password("123456")
                        .serverId("10")
                        .deserializer(new JsonDebeziumDeserializationSchema(false))
                        .build();
        MySqlSourceConfigFactory configFactory = mySqlSource.getConfigFactory();
        System.out.println("configFactory" + configFactory);
        LOG.warn("configFactory" + configFactory);
        DataStreamSource<String> mysqlCdcSource =
                env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MysqlCdcSource")
                        .setParallelism(1);
        mysqlCdcSource.print();
        env.execute("mytest");
    }
}
