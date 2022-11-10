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

package com.dtstack.chunjun.connector.emqx.util;

import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.utils.LogicalTypeChecks;

import java.util.stream.IntStream;

// import static org.apache.flink.table.types.logical.utils.LogicalTypeChecks.hasRoot;

/**
 * @author chuixue
 * @create 2021-06-18 14:26
 * @description
 */
public class DataTypeConventerUtil {
    public static int[] createValueFormatProjection(DataType physicalDataType) {
        final LogicalType physicalType = physicalDataType.getLogicalType();
        // TODO: lisai flink 1.16.0已经没有此方法；
        //        Preconditions.checkArgument(hasRoot(physicalType, LogicalTypeRoot.ROW), "Row data
        // type expected.");
        final int physicalFieldCount = LogicalTypeChecks.getFieldCount(physicalType);
        final IntStream physicalFields = IntStream.range(0, physicalFieldCount);

        return physicalFields.toArray();
    }
}
