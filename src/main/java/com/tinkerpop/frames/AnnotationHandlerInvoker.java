package com.tinkerpop.frames;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.InvocationHandler;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.frames.FramedGraph.EdgeFrameImpl;
import com.tinkerpop.frames.annotations.AnnotationHandler;

public class AnnotationHandlerInvoker<T extends Annotation> implements InvocationHandler{

    private AnnotationHandler<T> handler;
    private FramedGraph<?> graph;
    private T annotation;

    AnnotationHandlerInvoker(FramedGraph<?> graph, T annotation, AnnotationHandler<T> handler) {
        this.graph = graph;
        this.annotation = annotation;
        this.handler = handler;
        
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Element element;
        Direction direction = null;
        if(proxy instanceof VertexFrame) {
            element = ((VertexFrame) proxy).asVertex();
        }
        else {
            EdgeFrameImpl edge = (EdgeFrameImpl)proxy;
            direction = edge.getDirection();
            element = edge.asEdge();
        }
        
        return handler.processElement(annotation, method, args, graph, element, direction);
    }

}
