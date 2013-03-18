package com.tinkerpop.frames;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.InvocationHandler;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.ElementHelper;

/**
 * The proxy class of a framed element.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class FramedElement implements InvocationHandler {

    private final Direction direction;
    protected final Element element;
    private FramedGraph framedGraph;

    protected static Object NO_INVOCATION_PATH = new Object();


    public FramedElement(final FramedGraph framedGraph, final Element element, final Direction direction) {
        if (null == framedGraph) {
            throw new IllegalArgumentException("FramedGraph can not be null");
        }

        if (null == element) {
            throw new IllegalArgumentException("Element can not be null");
        }
        this.element = element;
        this.framedGraph = framedGraph;
        this.direction = direction;
    }

    public FramedElement(final FramedGraph framedGraph, final Element element) {
        this(framedGraph, element, null);
    }

    public Object invoke(final Object proxy, final Method method, Object[] arguments) {

        final Annotation[] annotations = method.getAnnotations();
        for (final Annotation annotation : annotations) {
            if (this.framedGraph.hasAnnotationHandler(annotation.annotationType())) {
                if (arguments != null && arguments.length == 0) {
                    arguments = null;
                }
                return this.framedGraph.getAnnotationHandler(annotation.annotationType()).processElement(annotation, method, arguments, this.framedGraph, this.element, this.direction);
            }
        }

        return NO_INVOCATION_PATH;
    }


}
