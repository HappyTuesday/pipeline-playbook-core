package com.yit.deploy.core.support;

import com.yit.deploy.core.model.*;
import com.yit.deploy.core.steps.*;
import com.yit.deploy.core.utils.Utils;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import javax.annotation.Nonnull;
import java.util.*;

public interface StepsSupportWithHost extends StepsSupport {

    Host getCurrentHost();

    default Object ssh(String value) {
        SSHStep step = new SSHStep(getExecutionContext());
        new SSHStep.DslContext(step).targetHost(getCurrentHost()).shell(value);
        return step.execute();
    }

    default Object ssh(@DelegatesTo(value = SSHStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        SSHStep step = new SSHStep(getExecutionContext());
        new SSHStep.DslContext(step).targetHost(getCurrentHost());
        return step.setup(closure).execute();
    }

    default Object rsync(String sourceFile, String targetFile) {
        return rsync(null, sourceFile, targetFile);
    }

    default Object rsync(Host sourceHost, String sourceFile, String targetFile) {
        RsyncStep step = new RsyncStep(getExecutionContext());
        new RsyncStep.DslContext(step).sourceHost(sourceHost).targetHost(getCurrentHost()).source(sourceFile).target(targetFile);
        return step.execute();
    }

    default Object rsync(@DelegatesTo(value = RsyncStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        RsyncStep step = new RsyncStep(getExecutionContext());
        new RsyncStep.DslContext(step).targetHost(getCurrentHost());
        return step.setup(closure).execute();
    }

    default Object file(@DelegatesTo(value = FileStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        FileStep step = new FileStep(getExecutionContext());
        new FileStep.DslContext(step).targetHost(getCurrentHost());
        return step.setup(closure).execute();
    }

    default StatStruct stat(@DelegatesTo(value = StatStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        StatStep step = new StatStep(getExecutionContext());
        new StatStep.DslContext(step).targetHost(getCurrentHost());
        return step.setup(closure).execute(StatStruct.class);
    }

    default StatStruct stat(String value) {
        StatStep step = new StatStep(getExecutionContext());
        new StatStep.DslContext(step).targetHost(getCurrentHost()).path(value);
        return step.execute(StatStruct.class);
    }

    default void registerEtcd(@DelegatesTo(value = RegisterEtcdStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        RegisterEtcdStep step = new RegisterEtcdStep(getExecutionContext());
        new RegisterEtcdStep.DslContext(step).targetHost(getCurrentHost());
        step.setup(closure).execute();
    }

    default void registerEtcd(List<String> serviceNamesToRegister) {
        RegisterEtcdStep step = new RegisterEtcdStep(getExecutionContext());
        new RegisterEtcdStep.DslContext(step).targetHost(getCurrentHost()).serviceNames(serviceNamesToRegister).register();
        step.execute();
    }

    default void deregisterEtcd(List<String> serviceNamesToRegister) {
        RegisterEtcdStep step = new RegisterEtcdStep(getExecutionContext());
        new RegisterEtcdStep.DslContext(step).targetHost(getCurrentHost()).serviceNames(serviceNamesToRegister).deregister();
        step.execute();
    }

    default void dockerVolume(@DelegatesTo(value = DockerVolumeStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        DockerVolumeStep step = new DockerVolumeStep(getExecutionContext());
        new DockerVolumeStep.DslContext(step).targetHost(getCurrentHost());
        step.setup(closure).execute();
    }

    default void createDockerVolume(String value) {
        DockerVolumeStep step = new DockerVolumeStep(getExecutionContext());
        new DockerVolumeStep.DslContext(step).targetHost(getCurrentHost()).name(value);
        step.execute();
    }

    default DockerProcessCheckStep newDockerProcessCheckStep() {
        DockerProcessCheckStep step = new DockerProcessCheckStep(getExecutionContext());
        new DockerProcessCheckStep.DslContext(step)
            .targetHost(getCurrentHost())
            .projectName(getExecutionContext().getVariable("PROJECT_NAME", String.class));
        return step;
    }

    default void registerSLB(@DelegatesTo(value = RegisterSLBStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        RegisterSLBStep step = new RegisterSLBStep(getExecutionContext());
        new RegisterSLBStep.DslContext(step).targetHost(getCurrentHost());
        step.setup(closure).execute();
    }

    default String executeOnTargetReturnText(String shell) {
        return executeOnTargetReturnText(shell, null);
    }

    default String executeOnTargetReturnText(String shell, String pwd) {
        return new RemoteProcessLauncher(getCurrentHost(), shell, pwd).launcher.script(getScript()).executeReturnText();
    }

    default byte[] readFromTargetServer(@Nonnull String path) {
        return getCurrentHost().processLauncher(getScript(), "cat '" + path + "'").executeReturnOutput();
    }

    default String readTextFromTargetServer(@Nonnull String path) {
        return new String(readFromTargetServer(path), Utils.DefaultCharset);
    }
}
