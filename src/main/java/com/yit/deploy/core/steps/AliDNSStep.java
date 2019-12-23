package com.yit.deploy.core.steps;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.*;

/**
 * Created by nick on 14/09/2017.
 */
public class AliDNSStep extends AbstractStep {
    private String regionId;
    private String accessKeyId;
    private String accessKeySecret;

    private String recordType = "A";
    private String hostname;
    private String baseDomainName;
    private String ipAddress;

    private boolean enableUpdate;


    public AliDNSStep(JobExecutionContext context) {
        super(context);
    }

    public AliDNSStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() throws Exception {
        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        DescribeDomainRecordsResponse.Record record = getExistingRecord(client);
        if (record == null) {
            addRecord(client);
        } else if (!Objects.equals(record.getValue(), ipAddress)) {
            if (enableUpdate) {
                if (getEnv().isProdEnv()) {
                    getScript().userConfirm(String.format("Updating record value from %s to %s for DNS record %s.%s", record.getValue(), ipAddress, hostname, baseDomainName));
                }
                updateRecord(client, record.getRecordId());
            } else {
                getScript().warn(String.format("The record value %s is not the expected value %s for DNS record %s.%s", record.getValue(), ipAddress, hostname, baseDomainName));
            }
        }

        return null;
    }

    private DescribeDomainRecordsResponse.Record getExistingRecord(IAcsClient client) throws ClientException {
        DescribeDomainRecordsRequest request = new DescribeDomainRecordsRequest();
        request.setDomainName(baseDomainName);
        request.setRRKeyWord(hostname);
        DescribeDomainRecordsResponse response = client.getAcsResponse(request);
        List<DescribeDomainRecordsResponse.Record> records = Lambda.findAll(response.getDomainRecords(),
                r -> hostname.equalsIgnoreCase(r.getRR()));

        if (records.size() > 1) {
            throw new RuntimeException("more than one DNS records found for host " + hostname + " in domain " + baseDomainName);
        }
        if (records.isEmpty()) {
            return null;
        }
        return records.get(0);
    }

    private void addRecord(IAcsClient client) throws ClientException {
        AddDomainRecordRequest request = new AddDomainRecordRequest();
        request.setDomainName(baseDomainName);
        request.setRR(hostname);
        request.setType(recordType);
        request.setValue(ipAddress);
        getScript().info("Add a DNS record %s -> %s for domain %s", hostname, ipAddress, baseDomainName);
        client.getAcsResponse(request);
    }

    private void updateRecord(IAcsClient client, String recordId) throws ClientException {
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setRecordId(recordId);
        request.setRR(hostname);
        request.setType(recordType);
        request.setValue(ipAddress);
        getScript().info("Update the DNS record %s -> %s for domain %s", hostname, ipAddress, baseDomainName);
        client.getAcsResponse(request);
    }

    public static class DslContext {

        private AliDNSStep step;

        public DslContext(AliDNSStep step) {
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

        public DslContext recordType(String value) {
            step.recordType = value;
            return this;
        }

        public DslContext hostname(String value) {
            step.hostname = value;
            return this;
        }

        public DslContext baseDomainName(String value) {
            step.baseDomainName = value;
            return this;
        }

        public DslContext domainName(String value) {
            int index = value.indexOf('.');
            if (index <= 0 || index == value.length() - 1) {
                throw new IllegalArgumentException("invalid domain name " + value);
            }
            step.hostname = value.substring(0, index);
            step.baseDomainName = value.substring(index + 1);
            return this;
        }

        public DslContext ipAddress(String value) {
            step.ipAddress = value;
            return this;
        }

        public DslContext enableUpdate(boolean value) {
            step.enableUpdate = value;
            return this;
        }
    }
}
