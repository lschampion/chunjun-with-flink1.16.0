/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.chunjun.connector.mysqlcdc.source;

import com.dtstack.chunjun.config.SyncConfig;
import com.dtstack.chunjun.connector.jdbc.adapter.ConnectionAdapter;
import com.dtstack.chunjun.connector.jdbc.config.CdcJdbcConf;
import com.dtstack.chunjun.connector.jdbc.config.ConnectionConfig;
import com.dtstack.chunjun.connector.jdbc.exclusion.FieldNameExclusionStrategy;
import com.dtstack.chunjun.connector.mysqlcdc.converter.MysqlCdcRawTypeConverter;
import com.dtstack.chunjun.converter.RawTypeConverter;
import com.dtstack.chunjun.source.SourceFactory;
import com.dtstack.chunjun.util.GsonUtil;
import com.dtstack.chunjun.util.TableUtil;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSourceBuilder;
import com.ververica.cdc.connectors.mysql.table.MySqlDeserializationConverterFactory;
import com.ververica.cdc.connectors.mysql.table.MySqlReadableMetadata;
import com.ververica.cdc.debezium.table.MetadataConverter;
import com.ververica.cdc.debezium.table.RowDataDebeziumDeserializeSchema;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class MysqlcdcSourceFactory extends SourceFactory {

    protected CdcJdbcConf cdcConf;
    private RowDataDebeziumDeserializeSchema debeziumDeserializationSchema;

    public MysqlcdcSourceFactory(SyncConfig syncConf, StreamExecutionEnvironment env) {
        super(syncConf, env);
        Gson gson =
                new GsonBuilder()
                        .registerTypeAdapter(
                                ConnectionConfig.class,
                                new ConnectionAdapter("SourceConnectionConf"))
                        .addDeserializationExclusionStrategy(
                                new FieldNameExclusionStrategy("column"))
                        .create();
        GsonUtil.setTypeAdapter(gson);
        cdcConf = gson.fromJson(gson.toJson(syncConf.getReader().getParameter()), getConfClass());
    }

    @Override
    public RawTypeConverter getRawTypeConverter() {
        return MysqlCdcRawTypeConverter::apply;
    }

    @Override
    public DataStream<RowData> createSource() {

        List<DataTypes.Field> dataTypes =
                new ArrayList<>(syncConfig.getReader().getFieldList().size());

        syncConfig
                .getReader()
                .getFieldList()
                .forEach(
                        fieldConf -> {
                            dataTypes.add(
                                    DataTypes.FIELD(
                                            fieldConf.getName(),
                                            getRawTypeConverter().apply(fieldConf.getType())));
                        });
        final DataType dataType = DataTypes.ROW(dataTypes.toArray(new DataTypes.Field[0]));

        buildDeserializeSchema();
        MySqlSourceBuilder<RowData> builder = MySqlSource.<RowData>builder();
        builder.serverTimeZone("Asia/Shanghai")
                .hostname(cdcConf.getHost())
                .port(cdcConf.getPort())
                .databaseList(cdcConf.getDatabase())
                .tableList(cdcConf.getTableList().toArray(new String[0]))
                .username(cdcConf.getUsername())
                .password(cdcConf.getPassword())
                //  .serverId("" + cdcConf.getServerId())
                //  .deserializer(new
                // JsonDebeziumDeserializationSchema(false))
                .deserializer(this.debeziumDeserializationSchema);
        MySqlSource<RowData> mySqlSource = builder.build();
        DataStreamSource<RowData> mysqlCdcSource =
                env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MysqlCdcSource")
                        .setParallelism(1);
        return mysqlCdcSource;
    }

    private void buildDeserializeSchema() {
        RowType rowType = TableUtil.createRowType(fieldList, getRawTypeConverter());
        ArrayList<MetadataConverter> metadataConverterArrayList = new ArrayList<>();
        metadataConverterArrayList.add(MySqlReadableMetadata.DATABASE_NAME.getConverter());
        metadataConverterArrayList.add(MySqlReadableMetadata.TABLE_NAME.getConverter());
        metadataConverterArrayList.add(MySqlReadableMetadata.OP_TS.getConverter());
        MetadataConverter[] metadataConverters =
                metadataConverterArrayList.toArray(new MetadataConverter[0]);
        this.debeziumDeserializationSchema =
                RowDataDebeziumDeserializeSchema.newBuilder()
                        //                        .setMetadataConverters(metadataConverters)
                        .setServerTimeZone(ZoneOffset.of("+8"))
                        .setValueValidator(new DemoValueValidator())
                        .setUserDefinedConverterFactory(
                                MySqlDeserializationConverterFactory.instance())
                        .setResultTypeInfo(getTypeInformation())
                        .setPhysicalRowType(rowType)
                        .build();
        List<DataTypes.Field> dataTypes =
                new ArrayList<>(syncConfig.getReader().getFieldList().size());

        syncConfig
                .getReader()
                .getFieldList()
                .forEach(
                        fieldConf -> {
                            dataTypes.add(
                                    DataTypes.FIELD(
                                            fieldConf.getName(),
                                            getRawTypeConverter().apply(fieldConf.getType())));
                        });
        final DataType dataType = DataTypes.ROW(dataTypes.toArray(new DataTypes.Field[0]));
    }

    protected Class<? extends CdcJdbcConf> getConfClass() {
        return CdcJdbcConf.class;
    }

    public static final class DemoValueValidator
            implements RowDataDebeziumDeserializeSchema.ValueValidator {

        @Override
        public void validate(RowData rowData, RowKind rowKind) {
            // do nothing
        }
    }
}
