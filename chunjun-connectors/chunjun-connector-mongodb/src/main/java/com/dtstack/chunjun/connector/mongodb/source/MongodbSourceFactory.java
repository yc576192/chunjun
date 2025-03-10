/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.chunjun.connector.mongodb.source;

import com.dtstack.chunjun.config.SyncConfig;
import com.dtstack.chunjun.connector.mongodb.converter.MongodbRawTypeMapper;
import com.dtstack.chunjun.connector.mongodb.datasync.MongoConverterFactory;
import com.dtstack.chunjun.connector.mongodb.datasync.MongodbDataSyncConfig;
import com.dtstack.chunjun.converter.AbstractRowConverter;
import com.dtstack.chunjun.converter.RawTypeMapper;
import com.dtstack.chunjun.source.SourceFactory;
import com.dtstack.chunjun.util.GsonUtil;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MongodbSourceFactory extends SourceFactory {

    private final MongodbDataSyncConfig mongodbDataSyncConfig;

    public MongodbSourceFactory(SyncConfig syncConfig, StreamExecutionEnvironment env) {
        super(syncConfig, env);
        Gson gson = new GsonBuilder().create();
        GsonUtil.setTypeAdapter(gson);
        mongodbDataSyncConfig =
                gson.fromJson(
                        gson.toJson(syncConfig.getReader().getParameter()),
                        MongodbDataSyncConfig.class);
    }

    @Override
    public RawTypeMapper getRawTypeMapper() {
        return MongodbRawTypeMapper::apply;
    }

    @Override
    public DataStream<RowData> createSource() {
        MongodbInputFormatBuilder builder =
                MongodbInputFormatBuilder.newBuild(mongodbDataSyncConfig);
        MongoConverterFactory mongoConverterFactory =
                new MongoConverterFactory(mongodbDataSyncConfig);
        AbstractRowConverter converter;
        if (useAbstractBaseColumn) {
            converter = mongoConverterFactory.createColumnConverter();
        } else {
            converter = mongoConverterFactory.createRowConverter();
        }
        builder.setRowConverter(converter, useAbstractBaseColumn);
        builder.setConfig(mongodbDataSyncConfig);
        return createInput(builder.finish());
    }
}
