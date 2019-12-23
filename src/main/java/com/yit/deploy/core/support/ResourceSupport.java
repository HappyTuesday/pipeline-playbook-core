package com.yit.deploy.core.support;

import com.yit.deploy.core.dsl.evaluate.EnvironmentEvaluationContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.utils.EncryptionUtils;
import com.yit.deploy.core.utils.IO;
import com.yit.deploy.core.utils.Utils;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import hudson.FilePath;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public interface ResourceSupport extends ExceptionSupport, JinjaSupport {

    EnvironmentEvaluationContext getExecutionContext();

    default Environment getEnv() {
        return getExecutionContext().getEnv();
    }

    DeployModelTable getModelTable();

    PipelineScript getScript();

    default byte[] loadResource(String resourceName) {
        byte[] content = getModelTable().getDeployConfig().getResource(resourceName);
        if (EncryptionUtils.isEncrypted(content)) {
            content = new EncryptionUtils(getEnv().getEnvtype()).decrypt(new String(content, Utils.DefaultCharset));
        }
        return content;
    }

    default String loadTextResource(String resourceName) {
        byte[] content = loadResource(resourceName);
        return content == null ? null : new String(content, Utils.DefaultCharset);
    }

    default List<String> listResourcesInFolder(String folderName) {
        return listResourcesInFolder(folderName, false);
    }

    default List<String> listResourcesInFolder(String folderName, boolean recursive) {
        return getModelTable().getDeployConfig().listResourcesInFolder(folderName, recursive);
    }

    @Nonnull
    default String template(String templateName) throws IOException, ClassNotFoundException {
        return template(templateName, Collections.emptyMap());
    }

    @Nonnull
    default String template(String templateName, Map<String, Object> binding) throws IOException, ClassNotFoundException {
        TemplateEngine e = new SimpleTemplateEngine();
        String res = loadTextResource("templates/" + templateName);
        if (res == null) {
            throw new FileNotFoundException(templateName);
        }
        Template template = e.createTemplate(res);
        return template.make(binding).toString();
    }

    @Nonnull
    default byte[] loadResourceCascade(String catalog, String resourceName) throws FileNotFoundException {
        for (String alternative : new String[]{getEnv().getName(), getEnv().getEnvtype(), "default"}) {
            byte[] content = loadResource(catalog + "/" + alternative + "/" + resourceName);
            if (content != null) return content;
        }
        throw new FileNotFoundException("resources/" + catalog + "/*/" + resourceName);
    }

    default void copyResourceToFolderCascade(String catalog, String resourceFolder, FilePath target) throws FileNotFoundException {
        if (resourceFolder.endsWith("/")) {
            resourceFolder = resourceFolder.substring(0, resourceFolder.length() - 1);
        }
        for (String alternative : new String[]{getEnv().getName(), getEnv().getEnvtype(), "default"}) {
            String prefix = catalog + "/" + alternative + "/" + resourceFolder;
            List<String> resources = listResourcesInFolder(prefix, true);
            if (!resources.isEmpty()) {
                copyResourcesToFolder(prefix, resources, target);
                return;
            }
        }
        throw new FileNotFoundException("resources/" + "catalog/*/" + resourceFolder);
    }

    default void copyResourcesToFolder(String resourcesFolder, List<String> resources, FilePath targetForder) {
        for (String resource : resources) {
            assert resource.startsWith(resourcesFolder);
            String name = resource.substring(resourcesFolder.length() + 1);
            FilePath path = targetForder.child(name);
            unchecked(path.getParent(), FilePath::mkdirs);
            IO.writeBytes(path, loadResource(resource));
        }
    }

    default String loadTextResourceCascade(String catalog, String resourceName) throws FileNotFoundException {
        return new String(loadResourceCascade(catalog, resourceName), Utils.DefaultCharset);
    }

    default String loadSecretResource(String secretName) throws FileNotFoundException {
        return loadTextResourceCascade("secrets", secretName);
    }

    default String loadSecretResourceBase64(String secretName) throws FileNotFoundException {
        return Base64.getEncoder().encodeToString(loadResourceCascade("secrets", secretName));
    }

    default void copySecretResourcesToFolder(String secretFolderName, FilePath target) throws FileNotFoundException {
        copyResourceToFolderCascade("secrets", secretFolderName, target);
    }

    default String loadTemplateFileResource(String templateName) throws FileNotFoundException {
        return loadTextResourceCascade("templates", templateName);
    }

    default String loadTemplateResource(String templateName) throws FileNotFoundException {
        return createJinjaTemplate().render(loadTextResourceCascade("templates", templateName), templateName);
    }

    default String loadRestrictedTemplateResource(String templateName) throws FileNotFoundException {
        return createRestrictedJinjaTemplate().render(loadTextResourceCascade("templates", templateName), templateName);
    }

    default byte[] loadNormalFileResource(String filename) throws FileNotFoundException {
        return loadResourceCascade("files", filename);
    }

    default void copyResourcesToFolder(String resourceFolder, FilePath target) {
        if (resourceFolder.endsWith("/")) {
            resourceFolder = resourceFolder.substring(0, resourceFolder.length() - 1);
        }
        List<String> resources = listResourcesInFolder(resourceFolder, true);
        copyResourcesToFolder(resourceFolder, resources, target);
    }

    default String getRule(String name){
        return loadTextResource("rules/" + name);
    }

    default void withTemplateResource(@Nonnull String templateName, @Nonnull Consumer<FilePath> consumer) throws IOException, InterruptedException {
        String template = loadTemplateResource(templateName);

        String name = Lambda.last(Lambda.tokenize(templateName, "/"));
        int index = name.lastIndexOf('.');
        String prefix = index > 0 ? name.substring(0, index) : "template-";
        if (prefix.length() < 3) prefix += "-template-";
        String suffix = index >= 0 && index < name.length() - 1 ? name.substring(index) : null;

        FilePath path = getScript().createTextTempFile(template, "templates", prefix, suffix);
        try {
            consumer.accept(path);
        } finally {
            path.delete();
        }
    }
}
