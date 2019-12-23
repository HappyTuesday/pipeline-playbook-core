package com.yit.deploy.core.utils;

import com.yit.deploy.core.function.Lambda;
import sun.net.dns.ResolverConfiguration;
import sun.net.spi.nameservice.NameService;
import sun.net.util.IPAddressUtil;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.spi.NamingManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public final class CustomizedDNSNameService implements NameService {

    private final List<String> domainList;
    private final DirContext context;
    // in memory hosts file
    private final Map<String, InetAddress[]> hosts;

    public CustomizedDNSNameService() {
        this(null);
    }

    public CustomizedDNSNameService(List<String> nameServers) {
        this(nameServers, null);
    }

    public CustomizedDNSNameService(List<String> nameServers, List<String> domainList) {
        this(nameServers, domainList, null);
    }

    public CustomizedDNSNameService(List<String> nameServers, List<String> domainList, Map<String, InetAddress[]> hosts) {
        String nameProviderUrl;
        if (nameServers != null) {
            nameProviderUrl = createProviderURL(nameServers);
        } else {
            // name servers
            String value = System.getProperty("sun.net.spi.nameservice.nameservers");
            if (value != null && value.length() > 0) {
                nameProviderUrl = createProviderURL(value);
            } else {
                List<String> nsList = ResolverConfiguration.open().nameservers();
                nameProviderUrl = createProviderURL(nsList);
            }
        }

        if (nameProviderUrl.isEmpty()) {
            throw new IllegalArgumentException("no name server is provided");
        }

        Hashtable<String,Object> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", nameProviderUrl);
        try {
            this.context = (DirContext) NamingManager.getInitialContext(env);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }

        if (domainList != null) {
            this.domainList = domainList;
        } else {
            // default domain
            String value = System.getProperty("sun.net.spi.nameservice.domain");
            if (value != null && value.length() > 0) {
                this.domainList = Collections.singletonList(value);
            } else {
                this.domainList = ResolverConfiguration.open().searchlist();
            }
        }

        this.hosts = hosts;
    }

    public static Map<String, InetAddress[]> convertHosts(Map<String, String> hosts) {
        if (hosts == null) {
            return null;
        }

        return Lambda.mapValues(hosts, s -> {
            try {
                return InetAddress.getAllByName(s);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
        if (hosts != null) {
            // predefined hosts
            if (hosts.containsKey(host)) {
                return hosts.get(host);
            }
        }

        // DNS records that we search for
        String[] ids = {"A"};

        ArrayList<String> results = null;
        UnknownHostException uhe = null;

        // If host already contains a domain name then just look it up
        if (host.indexOf('.') >= 0) {
            try {
                results = resolve(host, ids, 0);
            } catch (UnknownHostException x) {
                uhe = x;
            }
        }

        // Here we try to resolve the host using the domain suffix or
        // the domain suffix search list. If the host cannot be resolved
        // using the domain suffix then we attempt devolution of
        // the suffix - eg: if we are searching for "foo" and our
        // domain suffix is "eng.sun.com" we will try to resolve
        // "foo.eng.sun.com" and "foo.sun.com".
        // It's not normal to attempt devolation with domains on the
        // domain suffix search list - however as ResolverConfiguration
        // doesn't distinguish domain or search list in the list it
        // returns we approximate by doing devolution on the domain
        // suffix if the list has one entry.

        if (results == null) {
            for (String parentDomain : domainList) {
                int start;
                while ((start = parentDomain.indexOf(".")) != -1
                    && start < parentDomain.length() -1) {

                    String finalHost = host+"."+parentDomain;
                    if (hosts != null) {
                        if (hosts.containsKey(finalHost)) {
                            return hosts.get(finalHost);
                        }
                    }

                    try {
                        results = resolve(finalHost, ids, 0);
                        break;
                    } catch (UnknownHostException x) {
                        uhe = x;
                        // devolve
                        parentDomain = parentDomain.substring(start+1);
                    }
                }
                if (results != null) {
                    break;
                }
            }
        }

        // finally try the host if it doesn't have a domain name
        if (results == null && (host.indexOf('.') < 0)) {
            results = resolve(host, ids, 0);
        }

        // if not found then throw the (last) exception thrown.
        if (results == null) {
            assert uhe != null;
            throw uhe;
        }

        /**
         * Convert the array list into a byte aray list - this
         * filters out any invalid IPv4/IPv6 addresses.
         */
        assert results.size() > 0;
        InetAddress[] addrs = new InetAddress[results.size()];
        int count = 0;
        for (int i=0; i<results.size(); i++) {
            String addrString = results.get(i);
            byte addr[] = IPAddressUtil.textToNumericFormatV4(addrString);
            if (addr == null) {
                addr = IPAddressUtil.textToNumericFormatV6(addrString);
            }
            if (addr != null) {
                addrs[count++] = InetAddress.getByAddress(host, addr);
            }
        }

        /**
         * If addresses are filtered then we need to resize the
         * array. Additionally if all addresses are filtered then
         * we throw an exception.
         */
        if (count == 0) {
            throw new UnknownHostException(host + ": no valid DNS records");
        }
        if (count < results.size()) {
            InetAddress[] tmp = new InetAddress[count];
            for (int i=0; i<count; i++) {
                tmp[i] = addrs[i];
            }
            addrs = tmp;
        }

        return addrs;
    }

    /**
     * Reverse lookup code. I.E: find a host name from an IP address.
     * IPv4 addresses are mapped in the IN-ADDR.ARPA. top domain, while
     * IPv6 addresses can be in IP6.ARPA or IP6.INT.
     * In both cases the address has to be converted into a dotted form.
     */
    public String getHostByAddr(byte[] addr) throws UnknownHostException {
        if (hosts != null) {
            for (Map.Entry<String, InetAddress[]> entry : hosts.entrySet()) {
                for (InetAddress a : entry.getValue()) {
                    if (Arrays.equals(a.getAddress(), addr)) {
                        return entry.getKey();
                    }
                }
            }
        }

        String host = null;
        try {
            String literalip = "";
            String[] ids = { "PTR" };
            ArrayList<String> results = null;
            if (addr.length == 4) { // IPv4 Address
                for (int i = addr.length-1; i >= 0; i--) {
                    literalip += (addr[i] & 0xff) +".";
                }
                literalip += "IN-ADDR.ARPA.";

                results = resolve(literalip, ids, 0);
                host = results.get(0);
            } else if (addr.length == 16) { // IPv6 Address
                /**
                 * Because RFC 3152 changed the root domain name for reverse
                 * lookups from IP6.INT. to IP6.ARPA., we need to check
                 * both. I.E. first the new one, IP6.ARPA, then if it fails
                 * the older one, IP6.INT
                 */

                for (int i = addr.length-1; i >= 0; i--) {
                    literalip += Integer.toHexString((addr[i] & 0x0f)) +"."
                        +Integer.toHexString((addr[i] & 0xf0) >> 4) +".";
                }
                String ip6lit = literalip + "IP6.ARPA.";

                try {
                    results = resolve(ip6lit, ids, 0);
                    host = results.get(0);
                } catch (UnknownHostException e) {
                    host = null;
                }
                if (host == null) {
                    // IP6.ARPA lookup failed, let's try the older IP6.INT
                    ip6lit = literalip + "IP6.INT.";
                    results = resolve(ip6lit, ids, 0);
                    host = results.get(0);
                }
            }
        } catch (Exception e) {
            throw new UnknownHostException(e.getMessage());
        }
        // Either we couldn't find it or the address was neither IPv4 or IPv6
        if (host == null)
            throw new UnknownHostException();
        // remove trailing dot
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }

    /**
     * Resolves the specified entry in DNS.
     *
     * Canonical name records are recursively resolved (to a maximum
     * of 5 to avoid performance hit and potential CNAME loops).
     *
     * @param   name    name to resolve
     * @param   ids     record types to search
     * @param   depth   call depth - pass as 0.
     *
     * @return  array list with results (will have at least on entry)
     *
     * @throws UnknownHostException if lookup fails or other error.
     */
    private ArrayList<String> resolve(final String name,
                                      final String[] ids, int depth)
        throws UnknownHostException
    {
        ArrayList<String> results = new ArrayList<>();
        Attributes attrs;

        // do the query
        try {
            attrs = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedExceptionAction<Attributes>() {
                    public Attributes run() throws NamingException {
                        return context.getAttributes(name, ids);
                    }
                });
        } catch (java.security.PrivilegedActionException pae) {
            throw new UnknownHostException(pae.getException().getMessage());
        }

        // non-requested type returned so enumeration is empty
        NamingEnumeration<? extends Attribute> ne = attrs.getAll();
        if (!ne.hasMoreElements()) {
            throw new UnknownHostException("DNS record not found");
        }

        // iterate through the returned attributes
        UnknownHostException uhe = null;
        try {
            while (ne.hasMoreElements()) {
                Attribute attr = ne.next();
                String attrID = attr.getID();

                for (NamingEnumeration<?> e = attr.getAll(); e.hasMoreElements();) {
                    String addr = (String)e.next();

                    // for canoncical name records do recursive lookup
                    // - also check for CNAME loops to avoid stack overflow

                    if (attrID.equals("CNAME")) {
                        if (depth > 4) {
                            throw new UnknownHostException(name + ": possible CNAME loop");
                        }
                        try {
                            results.addAll(resolve(addr, ids, depth+1));
                        } catch (UnknownHostException x) {
                            // canonical name can't be resolved.
                            if (uhe == null)
                                uhe = x;
                        }
                    } else {
                        results.add(addr);
                    }
                }
            }
        } catch (NamingException nx) {
            throw new UnknownHostException(nx.getMessage());
        }

        // pending exception as canonical name could not be resolved.
        if (results.isEmpty() && uhe != null) {
            throw uhe;
        }

        return results;
    }


    // ---------

    private static void appendIfLiteralAddress(String addr, StringBuffer sb) {
        if (IPAddressUtil.isIPv4LiteralAddress(addr)) {
            sb.append("dns://" + addr + " ");
        } else {
            if (IPAddressUtil.isIPv6LiteralAddress(addr)) {
                sb.append("dns://[" + addr + "] ");
            }
        }
    }

    /*
     * @return String containing the JNDI-DNS provider URL
     *         corresponding to the supplied List of nameservers.
     */
    private static String createProviderURL(List<String> nsList) {
        StringBuffer sb = new StringBuffer();
        for (String s: nsList) {
            appendIfLiteralAddress(s, sb);
        }
        return sb.toString();
    }

    /*
     * @return String containing the JNDI-DNS provider URL
     *         corresponding to the list of nameservers
     *         contained in the provided str.
     */
    private static String createProviderURL(String str) {
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(str, ",");
        while (st.hasMoreTokens()) {
            appendIfLiteralAddress(st.nextToken(), sb);
        }
        return sb.toString();
    }
}
