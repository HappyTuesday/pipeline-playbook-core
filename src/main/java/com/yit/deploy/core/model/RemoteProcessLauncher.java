package com.yit.deploy.core.model;

public class RemoteProcessLauncher {
    public final ProcessLauncher launcher;

    public RemoteProcessLauncher(Host host, String shell) {
        this(host, shell, null);
    }

    public RemoteProcessLauncher(Host host, String shell, String pwd) {
        String finalShell = shell;
        if (pwd != null) {
            finalShell = "cd '" + pwd + "'\n" + shell;
        }

        launcher = new ProcessLauncher();
        if (ConnectionChannel.local.equals(host.getChannel())) {
            launcher.bash(finalShell);
        } else {
            finalShell = "set -euo pipefail\n" + finalShell;
            launcher.cmd("ssh", "-o", "StrictHostKeyChecking=no", "-p", String.valueOf(host.getPort()), host.getUser() + "@" + host.getName(), finalShell);
        }
    }
}
