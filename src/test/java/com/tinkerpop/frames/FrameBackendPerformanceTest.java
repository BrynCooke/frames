package com.tinkerpop.frames;

import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.frames.domain.classes.Person;

/**
 * Test that attempts to benchmark different frame implementations
 *
 * @author Greg Bowyer
 */
@BenchmarkOptions(benchmarkRounds = 50, warmupRounds = 5)
public class FrameBackendPerformanceTest {

    private Graph graph;

    @Rule public MethodRule benchmarkRun = new BenchmarkRule();

    @Before
    public void createGraph() {
        graph = TinkerGraphFactory.createTinkerGraph();
        
        Random random = new Random();
        for (int i=0; i<50000; i++) {
            int id = random.nextInt();
            Vertex relation = null;

            if (graph.getVertex(id) != null) {
                relation = graph.getVertex(id);
            }
            while(graph.getVertex(id) != null) {
                id = random.nextInt();
            }

            Vertex vertex = graph.addVertex(id);
            vertex.setProperty("age", random.nextInt());
            if (relation != null) {
                try {
                    Edge edge = graph.addEdge(i+1, relation, vertex, "knows");
                    edge.setProperty("weight", i);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    @Test
    public void perfTest() throws Exception {
        
        FramedGraph<Graph> framedGraph = new FramedGraph<Graph>(graph);
        Iterable<Person> person = framedGraph.frameVertices(framedGraph.getVertices(), Person.class);
        for (Person p : person) {
            p.getAge();
        }
        
    }


}