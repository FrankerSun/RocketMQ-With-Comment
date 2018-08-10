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
package org.apache.rocketmq.store;

public enum PutMessageStatus {
    PUT_OK,             // 正常状态
    FLUSH_DISK_TIMEOUT, // 同步刷盘Flush超时
    FLUSH_SLAVE_TIMEOUT,// 同步刷盘HA do sync transfer other node, wait return, but failed
    SLAVE_NOT_AVAILABLE,// 同步刷盘HA slave not available
    SERVICE_NOT_AVAILABLE,   // 服务不可用
    CREATE_MAPEDFILE_FAILED, // 创建MappedFile失败
    MESSAGE_ILLEGAL,         // 消息不合法：body大于默认最大值/topic length太长
    PROPERTIES_SIZE_EXCEEDED,// message properties length too long
    OS_PAGECACHE_BUSY,       // 操作系统页缓存忙碌中
    UNKNOWN_ERROR,           // 未知错误
}