package com.yit.deploy.core.utils;

import groovy.lang.Closure;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.sql.DriverManager.println;
import static org.apache.zookeeper.ZooKeeper.States.*;

public class ZookeeperUtils {
    private static final int TIMEOUT_MSEC = 5000;
    private static final int RETRY_MSEC = 100;

    public static <T> T zookeeperOperate(String ip, Closure<T> c) {
        int num_retries = 0;
        String address = ip + ":21081";
        Watcher noOpWatcher = event -> {};
        println("try to connect zookeeper: " +ip);
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(address, TIMEOUT_MSEC, noOpWatcher);

            while (!CONNECTED.equals(zk.getState()) && num_retries < (TIMEOUT_MSEC / RETRY_MSEC)) {
                Thread.sleep(RETRY_MSEC);
                num_retries++;
            }

            if (CONNECTED.equals(zk.getState())) {
                return c.call(zk);
            } else {
                throw new IllegalStateException("after " + TIMEOUT_MSEC + " ms the status is still " + zk.getState());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (zk != null) {
                try {
                    zk.close();
                } catch (InterruptedException e) {
                    // just ignore
                }
            }
        }
    }

    public static List<String> getNode(ZooKeeper zk, String path) {
        if (path == null) {
            return Collections.emptyList();
        }

        try {
            List<String> node = zk.getChildren(path, true);
            if (node.isEmpty()) {
                return Collections.emptyList();
            }

            return node;
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
