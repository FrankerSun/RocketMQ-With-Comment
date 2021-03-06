/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.broker.filter;

import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.filter.util.BitsArray;
import org.apache.rocketmq.store.CommitLogDispatcher;
import org.apache.rocketmq.store.DispatchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

/**
 * 计算filter的位图
 * Calculate bit map of filter.
 */
public class CommitLogDispatcherCalcBitMap implements CommitLogDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.FILTER_LOGGER_NAME);

    protected final BrokerConfig brokerConfig;
    protected final ConsumerFilterManager consumerFilterManager;

    public CommitLogDispatcherCalcBitMap(BrokerConfig brokerConfig, ConsumerFilterManager consumerFilterManager) {
        this.brokerConfig = brokerConfig;
        this.consumerFilterManager = consumerFilterManager;
    }

    @Override
    public void dispatch(DispatchRequest request) {

        if (!this.brokerConfig.isEnableCalcFilterBitMap()) {
            return;
        }

        try {

            // 对于每一条消息，首先根据话题取得所有的消费过滤数据
            Collection<ConsumerFilterData> filterDatas = consumerFilterManager.get(request.getTopic());

            if (filterDatas == null || filterDatas.isEmpty()) {
                return;
            }
            // 这每一条数据代表的就是一条 SQL 过滤语句信息
            Iterator<ConsumerFilterData> iterator = filterDatas.iterator();
            BitsArray filterBitMap = BitsArray.create(
                this.consumerFilterManager.getBloomFilter().getM()
            );

            long startTime = System.currentTimeMillis();
            while (iterator.hasNext()) {
                ConsumerFilterData filterData = iterator.next();

                if (filterData.getCompiledExpression() == null) {
                    log.error("[BUG] Consumer in filter manager has no compiled expression! {}", filterData);
                    continue;
                }

                if (filterData.getBloomFilterData() == null) {
                    log.error("[BUG] Consumer in filter manager has no bloom data! {}", filterData);
                    continue;
                }

                Object ret = null;
                try {
                    MessageEvaluationContext context = new MessageEvaluationContext(request.getPropertiesMap());

                    // 根据SQL语句对消息进行匹配
                    ret = filterData.getCompiledExpression().evaluate(context);
                } catch (Throwable e) {
                    log.error("Calc filter bit map error!commitLogOffset={}, consumer={}, {}", request.getCommitLogOffset(), filterData, e);
                }

                log.debug("Result of Calc bit map:ret={}, data={}, props={}, offset={}", ret, filterData, request.getPropertiesMap(), request.getCommitLogOffset());

                if (ret != null && ret instanceof Boolean && (Boolean) ret) {
                    // 若匹配，则将在注册客户端阶段计算好的BloomFilterData中的映射位信息赋值到filterBitMap中(位数组相应位设为1)
                    consumerFilterManager.getBloomFilter().hashTo(
                        filterData.getBloomFilterData(),
                        filterBitMap
                    );
                }
            }

            request.setBitMap(filterBitMap.bytes());

            long eclipseTime = System.currentTimeMillis() - startTime;
            // 1ms
            if (eclipseTime >= 1) {
                log.warn("Spend {} ms to calc bit map, consumerNum={}, topic={}", eclipseTime, filterDatas.size(), request.getTopic());
            }
        } catch (Throwable e) {
            log.error("Calc bit map error! topic={}, offset={}, queueId={}, {}", request.getTopic(), request.getCommitLogOffset(), request.getQueueId(), e);
        }
    }
}
