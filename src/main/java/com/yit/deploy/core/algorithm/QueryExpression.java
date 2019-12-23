package com.yit.deploy.core.algorithm;

import com.yit.deploy.core.function.Lambda;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.Objects;

public interface QueryExpression {

    String getText();

    default String getTextWithBrace(boolean withBrace) {
        return withBrace ? "(" + getText() + ")" : getText();
    }

    boolean match(String target, Matcher matcher);

    default boolean match(String target) {
        return match(target, Objects::equals);
    }

    boolean includes(QueryExpression another, Matcher matcher);

    default boolean includes(QueryExpression another) {
        return includes(another, Objects::equals);
    }

    abstract class Node implements QueryExpression {
        /**
         * Returns a string representation of the object. In general, the
         * {@code toString} method returns a string that
         * "textually represents" this object. The result should
         * be a concise but informative representation that is easy for a
         * person to read.
         * It is recommended that all subclasses override this method.
         * <p>
         * The {@code toString} method for class {@code Object}
         * returns a string consisting of the name of the class of which the
         * object is an instance, the at-sign character `{@code @}', and
         * the unsigned hexadecimal representation of the hash code of the
         * object. In other words, this method returns a string equal to the
         * value of:
         * <blockquote>
         * <pre>
         * getClass().getName() + '@' + Integer.toHexString(hashCode())
         * </pre></blockquote>
         *
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            return getText();
        }

        @Override
        abstract public int hashCode();

        @Override
        abstract public boolean equals(Object obj);
    }

    class Or extends Node {
        private final QueryExpression left;
        private final QueryExpression right;

        public Or(QueryExpression left, QueryExpression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean match(String target, Matcher matcher) {
            return left.match(target, matcher) || right.match(target, matcher);
        }

        @Override
        public boolean includes(QueryExpression another, Matcher matcher) {
            if (another instanceof Or) {
                Or that = (Or) another;
                return this.includes(that.left, matcher) && this.includes(that.right, matcher);
            }

            return left.includes(another, matcher) || right.includes(another, matcher);
        }

        @Override
        public String getText() {
            return left.getText() + " : " + right.getText();
        }

        @Override
        public int hashCode() {
            return Objects.hash(1, this.left, this.right);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Or) {
                Or that = (Or) obj;
                return this.left.equals(that.left) && this.right.equals(that.right)
                    || this.left.equals(that.right) && this.right.equals(that.left);
            } else {
                return this.left.equals(obj) && this.right.equals(obj);
            }
        }
    }

    class And extends Node {
        private final QueryExpression left;
        private final QueryExpression right;

        public And(QueryExpression left, QueryExpression right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean match(String target, Matcher matcher) {
            return left.match(target, matcher) && right.match(target, matcher);
        }

        @Override
        public boolean includes(QueryExpression another, Matcher matcher) {
            if (another instanceof And) {
                And that = (And) another;
                return this.includes(that.left, matcher) || this.includes(that.right, matcher);
            }

            return left.includes(another, matcher) && right.includes(another, matcher);
        }

        @Override
        public String getText() {
            return left.getTextWithBrace(left instanceof Or) + " & " + right.getTextWithBrace(right instanceof Or);
        }

        @Override
        public int hashCode() {
            return Objects.hash(2, this.left, this.right);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof And) {
                And that = (And) obj;
                return this.left.equals(that.left) && this.right.equals(that.right)
                    || this.left.equals(that.right) && this.right.equals(that.left);
            } else {
                return this.left.equals(obj) && this.right.equals(obj);
            }
        }
    }

    class Not extends Node {
        private final QueryExpression expr;

        public Not(QueryExpression expr) {
            this.expr = expr;
        }

        @Override
        public boolean match(String target, Matcher matcher) {
            return !expr.match(target, matcher);
        }

        @Override
        public boolean includes(QueryExpression another, Matcher matcher) {
            if (another instanceof Not) {
                return ((Not) another).expr.includes(expr, matcher);
            }

            if (another instanceof Or) {
                Or that = (Or) another;
                return this.includes(that.left, matcher) && this.includes(that.right, matcher);
            }

            if (another instanceof And) {
                And that = (And) another;
                return this.includes(that.left, matcher) && this.includes(that.right, matcher);
            }

            return false;
        }

        @Override
        public String getText() {
            return "!" + expr.getTextWithBrace(expr instanceof Or || expr instanceof And);
        }

        @Override
        public int hashCode() {
            return Objects.hash(3, this.expr);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Not) {
                return this.expr.equals(((Not) obj).expr);
            }

            if (obj instanceof Or) {
                Or that = (Or) obj;
                return this.equals(that.left) && this.equals(that.right);
            }

            if (obj instanceof And) {
                And that = (And) obj;
                return this.equals(that.left) && this.equals(that.right);
            }

            return false;
        }
    }

    interface Term extends QueryExpression {
        boolean includes(Term another, Matcher matcher);
    }

    abstract class TermNode extends Node implements Term {
        protected final String key;

        protected TermNode(String key) {
            this.key = key;
        }

        @Override
        public boolean includes(QueryExpression another, Matcher matcher) {
            if (another instanceof Or) {
                Or that = (Or) another;
                return this.includes(that.left, matcher) && this.includes(that.right, matcher);
            }

            if (another instanceof And) {
                And that = (And) another;
                return this.includes(that.left, matcher) || this.includes(that.right, matcher);
            }

            if (another instanceof Term) {
                return includes((Term) another, matcher);
            }

            return false;
        }

        @Override
        public String getText() {
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Or) {
                Or that = (Or) obj;
                return this.equals(that.left) && this.equals(that.right);
            }

            if (obj instanceof And) {
                And that = (And) obj;
                return this.equals(that.left) && this.equals(that.right);
            }

            if (obj instanceof Term) {
                return this.equals((Term) obj);
            }

            return false;
        }

        protected abstract boolean equals(Term obj);
    }

    class Simple extends TermNode {
        public Simple(String text) {
            super(text);
        }

        @Override
        public boolean match(String target, Matcher matcher) {
            return matcher.match(key, target);
        }

        @Override
        public boolean includes(Term another, Matcher matcher) {
            if (another instanceof Simple) {
                return matcher.match(key, ((Simple) another).key);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(4, key);
        }

        @Override
        public boolean equals(Term obj) {
            return obj instanceof Simple && this.key.equals(((Simple) obj).key);
        }
    }

    class Pattern extends TermNode {
        private final List<String> parts;

        public Pattern(String text) {
            super(text);
            this.parts = Lambda.tokenize(this.key, "*");
        }

        @Override
        public boolean match(String target, Matcher matcher) {
            if (parts.isEmpty()) {
                return true;
            }

            if (!key.startsWith("*") && !target.startsWith(parts.get(0))) {
                return false;
            }

            if (!key.endsWith("*") && !target.endsWith(parts.get(parts.size() - 1))) {
                return false;
            }

            int j = 0;
            for (String part : parts) {
                j = target.indexOf(part, j);
                if (j < 0) {
                    return false;
                }
                j += part.length();
            }

            return true;
        }

        @Override
        public boolean includes(Term another, Matcher matcher) {
            if (another instanceof Simple) {
                return match(((Simple) another).key, matcher);
            }

            if (another instanceof Pattern) {
                Pattern that = (Pattern) another;

                if (that.key.startsWith("*") && !this.key.startsWith("*")) {
                    return false;
                }

                if (that.key.endsWith("*") && !this.key.endsWith("*")) {
                    return false;
                }

                int j = 0, k = 0;
                for (String part : this.parts) {
                    boolean found = false;
                    for (; j < that.parts.size(); j++) {
                        String target = that.parts.get(j);
                        int n = target.indexOf(part, k);
                        if (n >= 0) {
                            found = true;
                            k = n + part.length();
                            break;
                        } else {
                            k = 0;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }

                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(5, key);
        }

        @Override
        protected boolean equals(Term obj) {
            return obj instanceof Pattern && this.key.equals(((Pattern) obj).key);
        }
    }

    static QueryExpression parse(QueryExpressionParser.ExpressionContext context) {
        context.children.forEach(QueryExpression::checkParseError);
        if (context instanceof QueryExpressionParser.OrContext) {
            QueryExpressionParser.OrContext or = (QueryExpressionParser.OrContext) context;
            return new Or(parse(or.left), parse(or.right));
        }
        if (context instanceof QueryExpressionParser.AndContext) {
            QueryExpressionParser.AndContext and = (QueryExpressionParser.AndContext) context;
            return new And(parse(and.left), parse(and.right));
        }
        if (context instanceof QueryExpressionParser.NotContext) {
            return new Not(parse(((QueryExpressionParser.NotContext) context).expr));
        }
        if (context instanceof QueryExpressionParser.ParenContext) {
            return parse(((QueryExpressionParser.ParenContext) context).expr);
        }
        if (context instanceof QueryExpressionParser.TermContext) {
            Token token = ((QueryExpressionParser.TermContext) context).term;
            String text = token.getText();
            if (text.indexOf('*') < 0) {
                return new Simple(text);
            } else {
                return new Pattern(text);
            }
        }
        if (context.exception != null) {
            throw context.exception;
        }
        throw new IllegalArgumentException("invalid expr type: " + context.getClass());
    }

    static QueryExpression parse(String query) {
        QueryExpressionLexer lexer = new QueryExpressionLexer(new ANTLRInputStream(query));
        QueryExpressionParser parser = new QueryExpressionParser(new CommonTokenStream(lexer));
        QueryExpressionParser.ProgramContext program = parser.program();
        program.children.forEach(QueryExpression::checkParseError);
        return parse(program.expr);
    }

    static void checkParseError(ParseTree tree) {
        if (tree instanceof ErrorNode) {
            throw new IllegalArgumentException(tree.getText());
        }
    }

    @FunctionalInterface
    interface Matcher {
        boolean match(String key, String target);
    }
}
