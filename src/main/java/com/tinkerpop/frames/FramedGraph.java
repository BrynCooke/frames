package com.tinkerpop.frames;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackHelper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.NoOp;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.StringFactory;
import com.tinkerpop.blueprints.util.wrappers.WrapperGraph;
import com.tinkerpop.frames.annotations.AdjacencyAnnotationHandler;
import com.tinkerpop.frames.annotations.AnnotationHandler;
import com.tinkerpop.frames.annotations.DomainAnnotationHandler;
import com.tinkerpop.frames.annotations.IncidenceAnnotationHandler;
import com.tinkerpop.frames.annotations.PropertyAnnotationHandler;
import com.tinkerpop.frames.annotations.RangeAnnotationHandler;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovyAnnotationHandler;
import com.tinkerpop.frames.structures.FramedEdgeIterable;
import com.tinkerpop.frames.structures.FramedVertexIterable;

/**
 * The primary class for interpreting/framing elements of a graph in terms of
 * particulate annotated interfaces. This is a wrapper graph in that it requires
 * an underlying graph from which to add functionality. The standard Blueprints
 * graph methods are exposed along with extra methods to make framing easy.
 * 
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class FramedGraph<T extends Graph> implements Graph, WrapperGraph<T> {

    public static class VertexFrameImpl implements VertexFrame {
        private Vertex vertex;

        public final void setVertex(final Vertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public final Vertex asVertex() {
            return vertex;
        }

        @Override
        public final int hashCode() {
            return vertex.hashCode();
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj instanceof VertexFrame) {
                return vertex.equals(((VertexFrame) obj).asVertex());
            } else if (obj instanceof Vertex) {
                return vertex.equals(obj);
            }
            return false;
        }

        @Override
        public final String toString() {
            return vertex.toString();
        }

    }

    public static class EdgeFrameImpl implements EdgeFrame {
        private Edge edge;
        private Direction direction;

        public final void setEdge(final Edge edge) {
            this.edge = edge;
        }

        @Override
        public final Edge asEdge() {
            return edge;
        }

        @Override
        public final int hashCode() {
            return edge.hashCode();
        }

        @Override
        public final String toString() {
            return edge.toString();
        }
        
        public Direction getDirection() {
            return direction;
        }
        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj instanceof EdgeFrame) {
                return edge.equals(((EdgeFrame) obj).asEdge());
            } else if (obj instanceof Edge) {
                return edge.equals(obj);
            }
            return false;
        }

    }

    private final Map<Class, Factory> classCache = new ConcurrentHashMap<Class, Factory>();
    private final Map<Class, CallbackHelper> callbackCache = new ConcurrentHashMap<Class, CallbackHelper>();

    protected final T baseGraph;
    private final Map<Class<? extends Annotation>, AnnotationHandler<? extends Annotation>> annotationHandlers = new HashMap<Class<? extends Annotation>, AnnotationHandler<? extends Annotation>>();
    private List<FrameInitializer> frameInitializers = new ArrayList<FrameInitializer>();

    /**
     * Construct a FramedGraph that will frame the elements of the underlying
     * graph.
     * 
     * @param baseGraph
     *            the graph whose elements to frame
     */
    public FramedGraph(final T baseGraph) {
        this.baseGraph = baseGraph;

        registerAnnotationHandler(new PropertyAnnotationHandler());
        registerAnnotationHandler(new AdjacencyAnnotationHandler());
        registerAnnotationHandler(new IncidenceAnnotationHandler());
        registerAnnotationHandler(new DomainAnnotationHandler());
        registerAnnotationHandler(new RangeAnnotationHandler());
        registerAnnotationHandler(new GremlinGroovyAnnotationHandler());
    }

    public <F> F generate(Class<F> superclass, Class frameKind, final Element element) {
        Factory factory = classCache.get(frameKind);
        
        if (factory == null) {
            Class[] interfaces = new Class[] { frameKind };
            CallbackHelper callbackHelper = new CallbackHelper(superclass, interfaces) {
                
                @Override
                protected Object getCallback(Method method) {
                    for(Annotation annotation : method.getAnnotations()) {
                        AnnotationHandler handler = annotationHandlers.get(annotation.annotationType());
                        if(handler != null) {
                            return new AnnotationHandlerInvoker(FramedGraph.this, annotation, handler);
                        }
                    }
                    return NoOp.INSTANCE;
                }
            };
                
            Enhancer enhancer = new Enhancer();
            enhancer.setClassLoader(frameKind.getClassLoader());
            factory = (Factory) enhancer.create(superclass, interfaces, callbackHelper, callbackHelper.getCallbacks());
            classCache.put(frameKind, factory);
            callbackCache.put(frameKind, callbackHelper);
        }

        return (F) factory.newInstance(callbackCache.get(frameKind).getCallbacks());
    }


    /**
     * A helper method for framing a vertex.
     * 
     * @param vertex
     *            the vertex to frame
     * @param kind
     *            the annotated interface to frame the vertex as
     * @param <F>
     *            the type of the annotated interface
     * @return a proxy objects backed by a vertex and interpreted from the
     *         perspective of the annotate interface
     */
    public <F> F frame(final Vertex vertex, final Class<F> kind) {
        VertexFrameImpl framed = generate(VertexFrameImpl.class, kind, vertex);
        framed.setVertex(vertex);
        return (F) framed;
    }

    /**
     * A helper method for framing an edge.
     * 
     * @param edge
     *            the edge to frame
     * @param direction
     *            the direction of the edges
     * @param kind
     *            the annotated interface to frame the edges as
     * @param <F>
     *            the type of the annotated interface
     * @return an iterable of proxy objects backed by an edge and interpreted
     *         from the perspective of the annotate interface
     */
    public <F> F frame(final Edge edge, final Direction direction, final Class<F> kind) {
        EdgeFrameImpl framed = generate(EdgeFrameImpl.class, kind, edge);
        framed.setEdge(edge);
        framed.setDirection(direction);
        return (F) framed;
    }

    /**
     * A helper method for framing an iterable of vertices.
     * 
     * @param vertices
     *            the vertices to frame
     * @param kind
     *            the annotated interface to frame the vertices as
     * @param <F>
     *            the type of the annotated interface
     * @return an iterable of proxy objects backed by a vertex and interpreted
     *         from the perspective of the annotate interface
     */
    public <F> Iterable<F> frameVertices(final Iterable<Vertex> vertices, final Class<F> kind) {
        return new FramedVertexIterable<F>(this, vertices, kind);
    }

    /**
     * A helper method for framing an iterable of edges.
     * 
     * @param edges
     *            the edges to frame
     * @param direction
     *            the direction of the edges
     * @param kind
     *            the annotated interface to frame the edges as
     * @param <F>
     *            the type of the annotated interface
     * @return an iterable of proxy objects backed by an edge and interpreted
     *         from the perspective of the annotate interface
     */
    public <F> Iterable<F> frameEdges(final Iterable<Edge> edges, final Direction direction, final Class<F> kind) {
        return new FramedEdgeIterable<F>(this, edges, direction, kind);
    }

    public Vertex getVertex(final Object id) {
        return this.baseGraph.getVertex(id);
    }

    /**
     * Frame a vertex according to a particular kind of annotated interface.
     * 
     * @param id
     *            the id of the vertex
     * @param kind
     *            the annotated interface to frame the vertex as
     * @param <F>
     *            the type of the annotated interface
     * @return a proxy object backed by the vertex and interpreted from the
     *         perspective of the annotate interface
     */
    public <F> F getVertex(final Object id, final Class<F> kind) {
        return this.frame(this.baseGraph.getVertex(id), kind);
    }

    public Vertex addVertex(final Object id) {
        return this.baseGraph.addVertex(id);
    }

    /**
     * Add a vertex to the underlying graph and return it as a framed vertex.
     * 
     * @param id
     *            the id of the newly created vertex
     * @param kind
     *            the annotated interface to frame the vertex as
     * @param <F>
     *            the type of the annotated interface
     * @return a proxy object backed by the vertex and interpreted from the
     *         perspective of the annotate interface
     */
    public <F> F addVertex(final Object id, final Class<F> kind) {
        Vertex vertex = this.baseGraph.addVertex(id);
        for (FrameInitializer initializer : frameInitializers) {
            initializer.initElement(kind, this, vertex);
        }
        return this.frame(vertex, kind);
    }

    public Edge getEdge(final Object id) {
        return this.baseGraph.getEdge(id);
    }

    /**
     * Frame an edge according to a particular kind of annotated interface.
     * 
     * @param id
     *            the id of the edge
     * @param direction
     *            the direction of the edge
     * @param kind
     *            the annotated interface to frame the edge as
     * @param <F>
     *            the type of the annotated interface
     * @return a proxy object backed by the edge and interpreted from the
     *         perspective of the annotate interface
     */
    public <F> F getEdge(final Object id, final Direction direction, final Class<F> kind) {
        return this.frame(this.baseGraph.getEdge(id), direction, kind);
    }

    public Edge addEdge(final Object id, final Vertex outVertex, final Vertex inVertex, final String label) {
        return this.baseGraph.addEdge(id, outVertex, inVertex, label);
    }

    /**
     * Add an edge to the underlying graph and return it as a framed edge.
     * 
     * @param id
     *            the id of the newly created edge
     * @param outVertex
     *            the outgoing vertex
     * @param inVertex
     *            the incoming vertex
     * @param label
     *            the label of the edge
     * @param direction
     *            the direction of the edge
     * @param kind
     *            the annotated interface to frame the edge as
     * @param <F>
     *            the type of the annotated interface
     * @return a proxy object backed by the edge and interpreted from the
     *         perspective of the annotate interface
     */
    public <F> F addEdge(final Object id, final Vertex outVertex, final Vertex inVertex, final String label, final Direction direction, final Class<F> kind) {
        Edge edge = this.baseGraph.addEdge(id, outVertex, inVertex, label);
        for (FrameInitializer initializer : frameInitializers) {
            initializer.initElement(kind, this, edge);
        }

        return this.frame(edge, direction, kind);
    }

    public void removeVertex(final Vertex vertex) {
        this.baseGraph.removeVertex(vertex);
    }

    public void removeEdge(final Edge edge) {
        this.baseGraph.removeEdge(edge);
    }

    public Iterable<Vertex> getVertices() {
        return this.baseGraph.getVertices();
    }

    public Iterable<Vertex> getVertices(final String key, final Object value) {
        return this.baseGraph.getVertices(key, value);
    }

    /**
     * Frame vertices according to a particular kind of annotated interface.
     * 
     * @param key
     *            the key of the vertices to get
     * @param value
     *            the value of the vertices to get
     * @param kind
     *            the annotated interface to frame the vertices as
     * @param <F>
     *            the type of the annotated interface
     * @return an iterable of proxy objects backed by the vertices and
     *         interpreted from the perspective of the annotate interface
     */
    public <F> Iterable<F> getVertices(final String key, final Object value, final Class<F> kind) {
        return new FramedVertexIterable<F>(this, this.baseGraph.getVertices(key, value), kind);
    }

    public Iterable<Edge> getEdges() {
        return this.baseGraph.getEdges();
    }

    public Iterable<Edge> getEdges(final String key, final Object value) {
        return this.baseGraph.getEdges(key, value);
    }

    /**
     * Frame edges according to a particular kind of annotated interface.
     * 
     * @param key
     *            the key of the edges to get
     * @param value
     *            the value of the edges to get
     * @param direction
     *            the direction of the edges
     * @param kind
     *            the annotated interface to frame the edges as
     * @param <F>
     *            the type of the annotated interface
     * @return an iterable of proxy objects backed by the edges and interpreted
     *         from the perspective of the annotate interface
     */
    public <F> Iterable<F> getEdges(final String key, final Object value, final Direction direction, final Class<F> kind) {
        return new FramedEdgeIterable<F>(this, this.baseGraph.getEdges(key, value), direction, kind);
    }

    public Features getFeatures() {
        Features features = this.baseGraph.getFeatures().copyFeatures();
        features.isWrapper = true;
        return features;
    }

    public void shutdown() {
        this.baseGraph.shutdown();
    }

    public T getBaseGraph() {
        return this.baseGraph;
    }

    public String toString() {
        return StringFactory.graphString(this, this.baseGraph.toString());
    }

    public GraphQuery query() {
        return this.baseGraph.query();
    }

    /**
     * The method used to register a new annotation handler for every new
     * annotation a new annotation handler has to be registered in the framed
     * graph
     * 
     * @param handler
     *            the annotation handler
     */
    public void registerAnnotationHandler(final AnnotationHandler<? extends Annotation> handler) {
        this.annotationHandlers.put(handler.getAnnotationType(), handler);
      }


    /**
     * Register a <code>FrameInitializer</code> that will be called whenever a
     * new vertex or edge is added to the graph. The initializer may mutate the
     * vertex (or graph) before returning the framed element to the user.
     * 
     * @param frameInitializer
     *            the frame initializer
     */
    public void registerFrameInitializer(FrameInitializer frameInitializer) {
        frameInitializers.add(frameInitializer);
    }

}
