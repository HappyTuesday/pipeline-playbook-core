package com.yit.deploy.core.model;

import com.yit.deploy.core.global.cache.HostInfoCache;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.HostInfo;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

public class Host {

    private final String user;
    private final String name;
    private final int port;
    private final ConnectionChannel channel;
    private final boolean retired;
    private final Map<String, Object> labels;
    private final String description;

    private final transient Facts facts;

    public Host(HostInfo info, Environment env) {
        this.user = Lambda.cascade(info.getUser(), env.getDefaultDeployUser());
        this.name = info.getName();
        this.port = Lambda.cascade(info.getPort(), 22);
        this.channel = Lambda.cascade(info.getChannel(), ConnectionChannel.ssh);
        this.retired = Lambda.cascade(info.getRetired(), false);
        this.labels = info.getLabels() != null ? info.getLabels() : Collections.emptyMap();
        this.description = info.getDescription();

        this.facts = new Facts();
    }

    private Host(Host host, boolean retired) {
        this.user = host.user;
        this.name = host.name;
        this.port = host.port;
        this.channel = host.channel;
        this.retired = retired;
        this.labels = host.labels;
        this.description = host.description;

        this.facts = host.facts;
    }

    public Host toRetired(boolean retired) {
        return this.retired == retired ? this : new Host(this, retired);
    }

    public boolean isLocalhost() {
        return "localhost".equals(name);
    }

    public String getUname() {
        return facts.getUname();
    }

    public String getHostname() {
        return facts.getHostname();
    }

    public String getPublicIPAddress() {
        return facts.getPublicIPAddress();
    }

    public boolean isLinux() {
        return "Linux".equals(this.getUname());
    }

    public boolean isDarwin() {
        return "Darwin".equals(this.getUname());
    }

    public ProcessLauncher processLauncher(String shell) {
        return processLauncher(null, shell);
    }

    public ProcessLauncher processLauncher(PipelineScript script, String shell) {
        return new RemoteProcessLauncher(this, shell).launcher.script(script);
    }

    public String ssh(PipelineScript script, String shell) {
        return ssh(script, shell, Collections.singleton(0));
    }

    public String ssh(PipelineScript script, String shell, List<Integer> allowedCodes) {
        return ssh(script, shell, new HashSet<>(allowedCodes));
    }

    public String ssh(PipelineScript script, String shell, Set<Integer> allowedCodes) {
        return processLauncher(script, shell).allowedCodes(allowedCodes).executeReturnText();
    }

    public String ssh(String shell) {
        return ssh(shell, Collections.singleton(0));
    }

    public String ssh(String shell, List<Integer> allowedCodes) {
        return ssh(shell, new HashSet<>(allowedCodes));
    }

    public String ssh(String shell, Set<Integer> allowedCodes) {
        return ssh(null, shell, allowedCodes);
    }

    @Override
    public String toString() {
        return name;
    }

    public String getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public ConnectionChannel getChannel() {
        return channel;
    }

    public boolean isRetired() {
        return retired;
    }

    public boolean isNotRetired() {
        return !retired;
    }

    public int getPort() {
        return port;
    }

    public Map<String, Object> getLabels() {
        return labels;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Facts object of a set of hosts with the same name.
     * It is bound to the origin host instance.
     */
    private class Facts implements Serializable {
        String _uname;
        String _hostname;
        String _publicIPAddress;

        Function<String, String> getFetcher(String shell) {
            return x -> processLauncher(shell).timeout(3000).executeReturnText();
        }

        String getUname() {
            if (_uname == null) _uname = HostInfoCache.get(name, "uname", getFetcher("uname"));
            return _uname;
        }

        String getHostname() {
            if (_hostname == null) _hostname = HostInfoCache.get(name, "hostname", getFetcher("hostname"));
            return _hostname;
        }

        String getPublicIPAddress() {
            if (_publicIPAddress == null) {
                _publicIPAddress = HostInfoCache.get(name, "public-ip", x -> {
                    String address = null;
                    if ("Linux".equals(getUname())) {
                        List<String> ips = Lambda.tokenize(processLauncher("hostname -I").timeout(3000).executeReturnText(), " ");
                        address = Lambda.find(ips, ip ->
                                !(ip.startsWith("10.") || ip.startsWith("192.") || ip.startsWith("172.")));
                    }
                    if (address == null) {
                        address = "0.0.0.0";
                    }
                    return address;
                });
            }
            return _publicIPAddress;
        }
    }
}
