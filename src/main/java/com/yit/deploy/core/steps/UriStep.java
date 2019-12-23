package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.utils.CustomizedDNSNameService;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.utils.IO;
import groovy.json.JsonSlurperClassic;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import sun.net.spi.nameservice.NameService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by nick on 14/09/2017.
 */
public class UriStep extends AbstractStep {
    private String url;
    private String method = "GET";
    private Map<String, String> headers = new HashMap<>();
    private Object payload;
    private boolean saveCookie;
    private Map<String, Object> params;
    private boolean followRedirects = false;
    private Set<Integer> statusCode = Collections.singleton(200);
    private boolean returnContent;
    private boolean autoParseJson;
    private List<String> dnsServers;
    private List<String> searchDomains;
    private Map<String, String> hosts;

    private String username;
    private String password;

    private String cookie;
    private Map<String, String> cookiesMap = new HashMap<>();

    private boolean verbose;

    public UriStep(JobExecutionContext context) {
        super(context);
    }

    public UriStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() throws Exception {
        assert url != null && !url.isEmpty() && method != null;

        String realUrl = this.url;
        if (params != null && !params.isEmpty()) {
            realUrl += "?" + String.join("&", Lambda.map(params.entrySet(), kv -> urlencode(kv.getKey()) + "=" + urlencode(kv.getValue().toString())));
        }

        if (verbose) {
            getScript().debug(method + " " + realUrl);
        }

        return processRequest(realUrl);
    }

    private Object processRequest(String realUrl) throws IOException {

        HttpRequestBase request = createHttpRequest(realUrl);
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = getPayloadEntity();
            if (entity != null) {
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            }
        }
        if (cookie != null) {
            request.setHeader("Cookie", cookie);
        }
        for (Map.Entry<String, String> h : headers.entrySet()) {
            request.setHeader(h.getKey(), h.getValue());
        }

        if (!Lambda.isNullOrEmpty(username)) {
            BasicScheme basicScheme = new BasicScheme(StandardCharsets.UTF_8);
            try {
                Header header = basicScheme.authenticate(new UsernamePasswordCredentials(username, password), request, new BasicHttpContext());
                request.addHeader(header);
            } catch (AuthenticationException e) {
                throw new IllegalArgumentException(e);
            }
        }

        HttpClientBuilder builder = HttpClientBuilder.create();
        if (followRedirects) {
            builder.setRedirectStrategy(new DefaultRedirectStrategy());
        }
        if (dnsServers != null && !dnsServers.isEmpty() ||
            searchDomains != null && !searchDomains.isEmpty() ||
            hosts != null && !hosts.isEmpty()) {

            NameService dns = new CustomizedDNSNameService(
                dnsServers,
                searchDomains,
                CustomizedDNSNameService.convertHosts(hosts)
            );

            builder.setDnsResolver(dns::lookupAllHostAddr);
        }

        try (CloseableHttpClient client = builder.build()) {
            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getStatusLine().getStatusCode();
                if (!statusCode.contains(code)) {
                    HttpEntity entity = response.getEntity();
                    String message = null;
                    if (entity != null) message = IO.getText(entity.getContent());
                    throw new UnexpectedStatusCodeException(code, message);
                }
                if (saveCookie) {
                    if (response.containsHeader("Set-Cookie")) {
                        Header[] setCookieHeaders = response.getHeaders("Set-Cookie");
                        for (Header setCookieHeader : setCookieHeaders) {
                            String setCookieValue = setCookieHeader.getValue();
                            String cookieDefinition = setCookieValue.split(";")[0];
                            String[] cookieNameValueArray = cookieDefinition.split("=");
                            String cookieName = cookieNameValueArray[0];
                            String cookieValue = cookieNameValueArray[1];
                            cookiesMap.put(cookieName, cookieValue);
                        }

                        cookie = getCookie(cookiesMap);
                    }
                }

                HttpEntity entity = response.getEntity();
                if (returnContent && entity != null) {
                    String text = IO.getText(entity.getContent());
                    String responseContentType = Lambda.safeNavigate(entity.getContentType(), Header::getValue);
                    if (autoParseJson || responseContentType != null && (responseContentType.startsWith("text/json") || responseContentType.startsWith("application/json"))) {
                        return new JsonSlurperClassic().parseText(text);
                    }
                    return text;
                }
                return null;
            }
        }
    }

    private String getCookie(Map<String, String> cookiesMap) {
        Collection<String> cookies = new ArrayList<>();
        for (Map.Entry<String, String> entry : cookiesMap.entrySet()) {
            cookies.add(entry.getKey() + "=" + entry.getValue());
        }

        return String.join(";", cookies);

    }

    private HttpRequestBase createHttpRequest(String url) {
        switch (method.toUpperCase()) {
            case "GET":
                return new HttpGet(url);
            case "POST":
                return new HttpPost(url);
            case "DELETE":
                return new HttpDelete(url);
            case "HEAD":
                return new HttpHead(url);
            case "PUT":
                return new HttpPut(url);
            default:
                throw new UnsupportedMethodException(method);
        }
    }

    private static String urlencode(String value) {
        try {
            return value != null ? URLEncoder.encode(value, "UTF-8") : null;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private HttpEntity getPayloadEntity() {
        if (payload instanceof String) {
            try {
                return new StringEntity((String) payload);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else if (payload instanceof Map) {
            List<BasicNameValuePair> pairs = Lambda.map((Map<String, Object>) payload, (n, v) -> new BasicNameValuePair(n, v.toString()));
            return new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public static class DslContext {

        private UriStep step;

        public DslContext(UriStep step) {
            this.step = step;
        }

        public DslContext url(String value) {
            step.url = value;
            return this;
        }

        public DslContext method(String value) {
            step.method = value;
            return this;
        }

        public DslContext GET() {
            return method("GET");
        }

        public DslContext POST() {
            return method("POST");
        }

        public DslContext DELETE() {
            return method("DELETE");
        }

        public DslContext HEAD() {
            return method("HEAD");
        }

        public DslContext PUT() {
            return method("PUT");
        }

        public DslContext followRedirects() {
            step.followRedirects = true;
            return this;
        }

        public DslContext statusCode(Integer... value) {
            step.statusCode = new HashSet<>(Arrays.asList(value));
            return this;
        }

        public DslContext returnContent() {
            step.returnContent = true;
            return this;
        }

        public DslContext autoParseJson() {
            step.autoParseJson = true;
            step.returnContent = true;
            return this;
        }

        public DslContext params(Map<String, Object> value) {
            step.params = value;
            return this;
        }

        public DslContext saveCookie() {
            return saveCookie(true);
        }

        public DslContext saveCookie(boolean value) {
            step.saveCookie = value;
            return this;
        }

        /**
         * the payload of the request, can be of type Map<String, String> or String
         *
         * @param value
         * @return
         */
        public DslContext payload(Object value) {
            step.payload = value;
            return this;
        }

        public DslContext contentType(String value) {
            step.headers.put("Content-Type", value);
            return this;
        }

        public DslContext acceptType(String value) {
            step.headers.put("Accept", value);
            return this;
        }

        public DslContext headers(Map<String, String> value) {
            step.headers = value;
            return this;
        }

        public DslContext username(String value) {
            step.username = value;
            return this;
        }

        public DslContext password(String value) {
            step.password = value;
            return this;
        }

        public DslContext verbose(boolean value) {
            step.verbose = value;
            return this;
        }

        public DslContext dnsServers(List<String> value) {
            step.dnsServers = value;
            return this;
        }

        public DslContext searchDomains(List<String> value) {
            step.searchDomains = value;
            return this;
        }

        public DslContext hosts(String host, String ipAddr) {
            if (step.hosts == null) {
                step.hosts = new HashMap<>();
            }
            step.hosts.put(host, ipAddr);
            return this;
        }
    }

    public static class UnsupportedMethodException extends RuntimeException {
        UnsupportedMethodException(String method) {
            super("HTTP METHOD " + method + " is not supported");
        }
    }

    public static class UnexpectedStatusCodeException extends RuntimeException {
        UnexpectedStatusCodeException(int code, String response) {
            super("HTTP status code " + code + " is not expected: " + response);
        }
    }
}
