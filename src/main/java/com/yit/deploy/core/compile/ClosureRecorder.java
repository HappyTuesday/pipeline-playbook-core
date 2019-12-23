package com.yit.deploy.core.compile;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.io.ReaderSource;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClosureRecorder extends CompilationCustomizer {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ClosureRecorder() {
        super(CompilePhase.CLASS_GENERATION); // after class generation, the closure will become a inner class
    }

    public void clear() {
        cache.clear();
    }

    public String getClosureText(Class<? extends Closure> clazz) {
        return cache.get(clazz.getName());
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        visitInnerClasses(source, classNode);
    }

    private void visitInnerClasses(SourceUnit source, ClassNode classNode) {
        for (Iterator<InnerClassNode> iter = classNode.getInnerClasses(); iter.hasNext();) {
            InnerClassNode cn = iter.next();
            if (!cn.isScriptBody()) {
                continue;
            }
            ReaderSource rs = source.getSource();
            if (rs == null) {
                continue;
            }
            String groovy = retrieveCode(rs, cn);
            this.cache.put(cn.getName(), groovy);
            visitInnerClasses(source, cn);
        }
    }

    private static String retrieveCode(ReaderSource source, ASTNode node) {
        int l1 = node.getLineNumber();
        int c1 = node.getColumnNumber();
        int l2 = node.getLastLineNumber();
        int c2 = node.getLastColumnNumber();

        if (l1 <= 0 || c1 <= 0 || l2 <= 0 || c2 <= 0 || l1 > l2) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = l1; i <= l2; i++) {
            String line = source.getLine(i, null);
            if (line == null) {
                return null;
            }
            if (i == l1 && i == l2) { // single line
                sb.append(line.substring(c1 - 1, c2 - 1));
            } else if (i == l1) { // first line
                sb.append(line.substring(c1 - 1));
            } else if (i == l2) { // last line
                sb.append(line.substring(0, c2 - 1));
            } else {
                sb.append(line);
            }
            if (i < l2) {
                sb.append('\n');
            }
        }

        return sb.toString();
    }
}
