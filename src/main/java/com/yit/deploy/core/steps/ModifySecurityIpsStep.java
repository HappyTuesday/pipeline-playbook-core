package com.yit.deploy.core.steps;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Closures;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

public class ModifySecurityIpsStep extends AbstractStep {

    private String regionId;
    private String accessKeyId;
    private String accessKeySecret;
    private String instanceId;
    private String securityItems;
    private String securityGroupName;
    private String instanceType;

    public ModifySecurityIpsStep(JobExecutionContext context) {
        super(context);
    }

    public ModifySecurityIpsStep setup(@DelegatesTo(value = ModifySecurityIpsStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    @Override
    protected Object executeOverride() throws Exception {
        getScript().info("InstanceType: %s, Instance id: %s, Security group name: %s, whitlist ips: %s", instanceType, instanceId, securityGroupName, securityItems);

        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = getCommonRequest();
        if(request == null) {
            throw new Exception("Instance type:"+ instanceType +" has no implement request.");
        }

        try {
            CommonResponse response = client.getCommonResponse(request);
            getScript().info(response.getData());
        } catch (ServerException e) {
            throw e;
        } catch (ClientException e) {
            throw e;
        }

        return null;
    }

    private CommonRequest getCommonRequest() {
        switch (instanceType) {
            case "rds":
                return getRdsCommonRequest();
            case "redis":
                return getRedisCommonRequest();
            case "slbAclAdd":
                return getSlbAclAddCommonRequest();
            case "slbAclDel":
                return getSlbAclRemoveCommonRequest();
            case "mongodb":
                return getMongodbCommonRequest();
        }

        return null;
    }

    private CommonRequest getRdsCommonRequest() {
        if (instanceType != "rds") return null;

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("rds.aliyuncs.com");
        request.setVersion("2014-08-15");
        request.setAction("ModifySecurityIps");
        request.putQueryParameter("DBInstanceId", instanceId);
        request.putQueryParameter("SecurityIps", securityItems);
        request.putQueryParameter("DBInstanceIPArrayName", securityGroupName);

        return request;
    }

    private CommonRequest getRedisCommonRequest() {
        if (instanceType != "redis") return null;

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("r-kvstore.aliyuncs.com");
        request.setVersion("2015-01-01");
        request.setAction("ModifySecurityIps");
        request.putQueryParameter("InstanceId", instanceId);
        request.putQueryParameter("SecurityIps", securityItems);
        request.putQueryParameter("SecurityIpGroupName", securityGroupName);

        return request;
    }

    private CommonRequest getSlbAclAddCommonRequest() {
        if (instanceType != "slbAclAdd") return null;

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("slb.aliyuncs.com");
        request.setVersion("2014-05-15");
        request.setAction("AddAccessControlListEntry");
        request.putQueryParameter("RegionId", regionId);
        request.putQueryParameter("AclId", instanceId);
        request.putQueryParameter("AclEntrys", securityItems);

        return request;
    }

    private CommonRequest getSlbAclRemoveCommonRequest() {
        if (instanceType != "slbAclDel") return null;

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("slb.aliyuncs.com");
        request.setVersion("2014-05-15");
        request.setAction("RemoveAccessControlListEntry");
        request.putQueryParameter("RegionId", regionId);
        request.putQueryParameter("AclId", instanceId);
        request.putQueryParameter("AclEntrys", securityItems);

        return request;
    }

    private CommonRequest getMongodbCommonRequest() {
        if (instanceType != "mongodb") return null;

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("mongodb.aliyuncs.com");
        request.setVersion("2015-12-01");
        request.setAction("ModifySecurityIps");
        request.putQueryParameter("RegionId", regionId);
        request.putQueryParameter("DBInstanceId", instanceId);
        request.putQueryParameter("SecurityIps", securityItems);
        request.putQueryParameter("SecurityIpGroupName", securityGroupName);

        return request;
    }

    public static class DslContext {
        private ModifySecurityIpsStep step;

        public DslContext(ModifySecurityIpsStep step) {
            this.step = step;
        }

        public DslContext regionId(String value) {
            step.regionId = value;
            return this;
        }

        public DslContext accessKeyId(String value) {
            step.accessKeyId = value;
            return this;
        }

        public DslContext accessKeySecret(String value) {
            step.accessKeySecret = value;
            return this;
        }

        public DslContext instanceId(String value) {
            step.instanceId = value;
            return this;
        }

        public DslContext securityItems(String value) {
            step.securityItems = value;
            return this;
        }

        public DslContext securityGroupName(String value) {
            step.securityGroupName = value;
            return this;
        }

        public DslContext instanceType(String value) {
            step.instanceType = value;
            return this;
        }
    }
}
