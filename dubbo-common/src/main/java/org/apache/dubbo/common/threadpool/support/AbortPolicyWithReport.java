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
package org.apache.dubbo.common.threadpool.support;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JVMUtil;

import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;

/**
 * dubbo 线程池拒绝策略
 * Abort Policy.
 * Log warn info when abort.
 */
public class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy {

    protected static final Logger logger = LoggerFactory.getLogger(AbortPolicyWithReport.class);

    /**
     * 线程名
     */
    private final String threadName;

    private final URL url;

    /**
     * 上次打印时间
     */
    private static volatile long lastPrintTime = 0;

    /**
     * 十分钟
     */
    private static final long TEN_MINUTES_MILLS = 10 * 60 * 1000;

    /**
     * windows系统前缀
     */
    private static final String OS_WIN_PREFIX = "win";

    /**
     * os系统前缀
     */
    private static final String OS_NAME_KEY = "os.name";

    private static final String WIN_DATETIME_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd_HH:mm:ss";

    /**
     * 信号量，大小是1
     */
    private static Semaphore guard = new Semaphore(1);

    public AbortPolicyWithReport(String threadName, URL url) {
        this.threadName = threadName;
        this.url = url;
    }

    /**
     * TODO dubbo 线程池的拒绝策略实现
     * @param r
     * @param e
     */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        String msg = String.format("Thread pool is EXHAUSTED!" +
                " Thread Name: %s, Pool Size: %d (active: %d, core: %d, max: %d, largest: %d), Task: %d (completed: "
                + "%d)," +
                " Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s), in %s://%s:%d!",
            threadName, e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(),
            e.getLargestPoolSize(),
            e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating(),
            url.getProtocol(), url.getIp(), url.getPort());
        //打印线程池信息和请求URL信息，并记录异常信息
        logger.warn(msg);
        //记录整个栈快照，十分钟触发一次
        dumpJStack();
        throw new RejectedExecutionException(msg);
    }

    /**
     * 记录整个栈快照，十分钟触发一次
     */
    private void dumpJStack() {
        long now = System.currentTimeMillis();

        //每10分钟打印一次，剩下的不要了
        //dump every 10 minutes
        if (now - lastPrintTime < TEN_MINUTES_MILLS) {
            return;
        }

        //使用信号量获取许可，没拿到许可就退出
        if (!guard.tryAcquire()) {
            return;
        }

        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.execute(() -> {
            //Dubbo_JStack.log日志路径
            String dumpPath = url.getParameter(DUMP_DIRECTORY, System.getProperty("user.home"));

            SimpleDateFormat sdf;

            //获取系统名
            String os = System.getProperty(OS_NAME_KEY).toLowerCase();

            // window system don't support ":" in file name
            if (os.contains(OS_WIN_PREFIX)) {
                //windows系统文件命名规则兼容
                sdf = new SimpleDateFormat(WIN_DATETIME_FORMAT);
            } else {
                sdf = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT);
            }

            String dateStr = sdf.format(new Date());
            //try-with-resources
            //获取文件流
            try (FileOutputStream jStackStream = new FileOutputStream(
                new File(dumpPath, "Dubbo_JStack.log" + "." + dateStr))) {
                //记录栈快照
                JVMUtil.jstack(jStackStream);
            } catch (Throwable t) {
                logger.error("dump jStack error", t);
            } finally {
                //释放信号量
                guard.release();
            }
            //记录上次打印时间
            lastPrintTime = System.currentTimeMillis();
        });
        //关闭线程池，避免内存溢出
        //must shutdown thread pool ,if not will lead to OOM
        pool.shutdown();

    }

}
