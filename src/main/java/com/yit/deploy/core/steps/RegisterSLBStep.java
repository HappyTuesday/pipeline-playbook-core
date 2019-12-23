package com.yit.deploy.core.steps;

import com.aliyuncs.AcsRequest;
import com.aliyuncs.AcsResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.slb.model.v20140515.*;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.global.resource.Resources;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import groovy.json.JsonOutput;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to (de)register a server to an aliyun slb
 *
 * Created by nick on 14/09/2017.
 */
public class RegisterSLBStep extends AbstractStep {

    private String regionId;

    private String accessId;

    private String keySecret;

    private String slbId;

    private Host targetHost;

    private int weight;

    public RegisterSLBStep(JobExecutionContext context) {
        super(context);
    }

    public RegisterSLBStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() {

        String hostId = getServerId(targetHost.getName());

        Resources.slb.acquire(slbId);
        try {
            DescribeLoadBalancerAttributeResponse slbInfo = getSlbInfo();

            DescribeLoadBalancerAttributeResponse.BackendServer server = Lambda.find(slbInfo.getBackendServers(), s ->
                hostId.equals(s.getServerId()));

            if (server == null) {
                addBackendServer(targetHost.getHostname(), hostId, weight);
            } else if (server.getWeight() != weight) {
                setBackendServerWeight(targetHost.getHostname(), hostId, server.getWeight(), weight);
            } else {
                // already updated
            }
        } finally {
            Resources.slb.release(slbId);
        }

        return null;
    }

    private String getServerId(String innerIp) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInnerIpAddresses(JsonOutput.toJson(Collections.singletonList(innerIp)));
        DescribeInstancesResponse result = doAction(request);
        if (result.getInstances().isEmpty()) {
            throw new IllegalArgumentException("could not find any ecs with inner ip address " + innerIp);
        }
        if (result.getInstances().size() != 1) {
            throw new IllegalArgumentException("more than one servers are found with inner ip address " + innerIp);
        }
        return result.getInstances().get(0).getInstanceId();
    }

    private DescribeLoadBalancerAttributeResponse getSlbInfo() {
        DescribeLoadBalancerAttributeRequest request = new DescribeLoadBalancerAttributeRequest();
        request.setLoadBalancerId(slbId);
        return doAction(request);
    }

    private AddBackendServersResponse addBackendServer(String hostname, String hostId, int weight) {
        AddBackendServersRequest request = new AddBackendServersRequest();
        request.setLoadBalancerId(slbId);
        request.setBackendServers(createHostsData(hostId, weight));
        getScript().debug("add backend " + hostname + " with weight " + weight + " to slb " + slbId);
        return doAction(request);
    }

    private SetBackendServersResponse setBackendServerWeight(String hostname, String hostId, int oldWeight, int newWeight) {
        SetBackendServersRequest request = new SetBackendServersRequest();
        request.setLoadBalancerId(slbId);
        request.setBackendServers(createHostsData(hostId, weight));
        getScript().debug("change weight of backend " + hostname + " from " + oldWeight + " to " + newWeight);
        return doAction(request);
    }

    private <T extends AcsResponse> T doAction(AcsRequest<T> request) {
        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessId, keySecret);
        IAcsClient client = new DefaultAcsClient(profile);
        try {
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private String createHostsData(String hostId, int weight) {
        Map<String, Object> map = new HashMap<>();
        map.put("ServerId", hostId);
        map.put("Weight", weight);
        return JsonOutput.toJson(Collections.singletonList(map));
    }

    public static class DslContext {

        private RegisterSLBStep step;

        public DslContext(RegisterSLBStep step) {
            this.step = step;
        }

        public DslContext regionId(String value) {
            step.regionId = value;
            return this;
        }

        public DslContext accessId(String value) {
            step.accessId = value;
            return this;
        }

        public DslContext keySecret(String value) {
            step.keySecret = value;
            return this;
        }

        public DslContext slbId(String value) {
            step.slbId = value;
            return this;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext weight(int value) {
            step.weight = value;
            return this;
        }
    }
}
