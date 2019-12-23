package com.yit.deploy.core.compile;

public class DeployClassloader extends ClassLoader {

    /**
     * Creates a new class loader using the specified parent class loader for
     * delegation.
     *
     * <p> If there is a security manager, its {@link
     * SecurityManager#checkCreateClassLoader()
     * <tt>checkCreateClassLoader</tt>} method is invoked.  This may result in
     * a security exception.  </p>
     *
     * @param parent The parent class loader
     * @throws SecurityException If a security manager exists and its
     *                           <tt>checkCreateClassLoader</tt> method doesn't allow creation
     *                           of a new class loader.
     * @since 1.2
     */
    protected DeployClassloader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Loads the class with the specified <a href="#name">binary name</a>.  The
     * default implementation of this method searches for classes in the
     * following order:
     *
     * <ol>
     *
     * <li><p> Invoke {@link #findLoadedClass(String)} to check if the class
     * has already been loaded.  </p></li>
     *
     * <li><p> Invoke the {@link #loadClass(String) <tt>loadClass</tt>} method
     * on the parent class loader.  If the parent is <tt>null</tt> the class
     * loader built-in to the virtual machine is used, instead.  </p></li>
     *
     * <li><p> Invoke the {@link #findClass(String)} method to find the
     * class.  </p></li>
     *
     * </ol>
     *
     * <p> If the class was found using the above steps, and the
     * <tt>resolve</tt> flag is true, this method will then invoke the {@link
     * #resolveClass(Class)} method on the resulting <tt>Class</tt> object.
     *
     * <p> Subclasses of <tt>ClassLoader</tt> are encouraged to override {@link
     * #findClass(String)}, rather than this method.  </p>
     *
     * <p> Unless overridden, this method synchronizes on the result of
     * {@link #getClassLoadingLock <tt>getClassLoadingLock</tt>} method
     * during the entire class loading process.
     *
     * @param name    The <a href="#name">binary name</a> of the class
     * @param resolve If <tt>true</tt> then resolve the class
     * @return The resulting <tt>Class</tt> object
     * @throws ClassNotFoundException If the class could not be found
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        if (checkIfInWhiteList(name)) {
            throw new ClassNotFoundException(name);
        }
        return super.loadClass(name, resolve);
    }

    private boolean checkIfInWhiteList(String name) {
        if (name.indexOf('_') < 0) {
            return false;
        }
        return checkIfInWhitelistForPrefix(name, "groovy.lang.GroovyObject$") ||
            checkIfInWhitelistForPrefix(name, "groovy.lang.GroovyObject$java$lang$") ||
            checkIfInWhitelistForPrefix(name, "groovy.lang.GroovyObject$java$util$") ||
            checkIfInWhitelistForPrefix(name, "groovy.lang.GroovyObject$groovy$lang$") ||
            checkIfInWhitelistForPrefix(name, "groovy.lang.GroovyObject$groovy$util$") ||
            checkIfInWhitelistForPrefix(name, "groovy.lang.GroovyObject$java$io$") ||
            checkIfInWhitelistForPrefix(name, "groovy.lang.GroovyObject$java$net$") ||
            checkIfInWhitelistForPrefix(name, "java.lang.") ||
            checkIfInWhitelistForPrefix(name, "java.util.") ||
            checkIfInWhitelistForPrefix(name, "groovy.lang.") ||
            checkIfInWhitelistForPrefix(name, "groovy.util.") ||
            checkIfInWhitelistForPrefix(name, "java.io.") ||
            checkIfInWhitelistForPrefix(name, "java.net.") ||
            checkIfContainsSpecialPrefix(name, "envs") ||
            checkIfContainsSpecialPrefix(name, "playbooks") ||
            checkIfContainsSpecialPrefix(name, "projects");
    }

    private boolean checkIfContainsSpecialPrefix(String name, String type) {
        String s;
        if (name.startsWith("groovy.lang.GroovyObject$" + type + "$")) {
            s = name.substring(name.lastIndexOf('$') + 1);
        } else if (name.startsWith(type + ".")) {
            int i = name.lastIndexOf('$');
            if (i > 0) {
                s = name.substring(i + 1);
            } else {
                s = name.substring(name.lastIndexOf('.') + 1);
            }
        } else {
            return false;
        }

        return checkIfCharsInWhitelist(s);
    }

    private boolean checkIfInWhitelistForPrefix(String name, String prefix) {
        if (!name.startsWith(prefix)) {
            return false;
        }
        // all chars are upper cased letters separated by '_'
        return checkIfCharsInWhitelist(name.substring(prefix.length()));
    }

    private boolean checkIfCharsInWhitelist(String s) {
        return s.indexOf('_') > 0 && s.chars().allMatch(c -> c == '_' || c == '.' || c == '$' || c >= 'A' && c <= 'Z');
    }
}
