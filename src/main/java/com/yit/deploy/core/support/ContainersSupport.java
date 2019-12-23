package com.yit.deploy.core.support;

import com.yit.deploy.core.docker.DockerImage;
import com.yit.deploy.core.docker.containers.*;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.model.DBConnection;
import com.yit.deploy.core.model.MysqlDBConnection;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by nick on 24/10/2017.
 */
public interface ContainersSupport {

    JobExecutionContext getExecutionContext();

    default void withRun(String imageFullName, Consumer<DockerContainer> f) {
        withRun(imageFullName, Collections.emptyList(), f);
    }

    default void withRun(String imageFullName, List<String> runOptions, Consumer<DockerContainer> f) {
        withRun(new DockerContainer(getExecutionContext(), imageFullName, runOptions), f);
    }

    default <TC extends DockerContainer> void withRun(TC container, Consumer<TC> f) {
        try {
            f.accept(container);
        } finally {
            container.stop();
        }
    }

    default void withMysqlQuery(
            MysqlDBConnection conn,
            Consumer<MysqlQueryContainer> f
    ) {
        withRun(newMysqlQuery(conn), f);
    }

    default MysqlQueryContainer newMysqlQuery(MysqlDBConnection conn) {
        return new MysqlQueryContainer(getExecutionContext(), conn);
    }

    default PostgresQueryContainer newPostgresQuery(DBConnection conn) {
        return new PostgresQueryContainer(getExecutionContext(), conn);
    }

    default MavenContainer newMavenContainer() {
        return new MavenContainer(getExecutionContext());
    }

    default MavenContainer newMavenContainer(String imageFullName, List<String> runOptions) {
        return new MavenContainer(getExecutionContext(), imageFullName, runOptions);
    }

    default NodeContainer newNodeContainer() {
        return new NodeContainer(getExecutionContext());
    }

    default NodeContainer newNodeContainer(String imageFullName, List<String> runOptions) {
        return new NodeContainer(getExecutionContext(), imageFullName, runOptions);
    }

    default JavaContainer newJavaContainer() {
        return new JavaContainer(getExecutionContext());
    }

    default DockerContainer newContainer(String imageFullName, String ... runOptions) {
        return new DockerContainer(newImage(imageFullName, runOptions));
    }

    default DockerImage newImage(String imageFullName, String ... runOptions) {
        return new DockerImage(getExecutionContext(), imageFullName, runOptions);
    }
}
