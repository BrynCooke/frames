package com.tinkerpop.frames.domain.classes;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incident;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import com.tinkerpop.frames.domain.incidences.Created;
import com.tinkerpop.frames.domain.incidences.Knows;

import java.util.Collection;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Person extends NamedObject {

    @Property("age")
    public Integer getAge();

    @Property("age")
    public void setAge(int age);

    @Property("age")
    public void removeAge();

    @Incident(label = "knows")
    public Collection<Knows> getKnows();

    @Adjacency(label = "knows")
    public Collection<Person> getKnowsPeople();

    @Adjacency(label = "knows")
    public void setKnowsPeople(final Collection<Person> knows);

    @Incident(label = "created")
    public Collection<Created> getCreated();

    @Adjacency(label = "created")
    public Collection<Project> getCreatedProjects();

    @Adjacency(label = "knows")
    public void addKnowsPerson(final Person person);

    @Incident(label = "knows")
    public Knows addKnows(final Person person);

    @Adjacency(label = "created")
    public void addCreatedProject(final Project project);

    @Incident(label = "created")
    public Created addCreated(final Project project);

    @Adjacency(label = "knows")
    public void removeKnowsPerson(final Person person);

    @Incident(label = "knows")
    public void removeKnows(final Knows knows);

    @Adjacency(label = "latestProject")
    public Project getLatestProject();

    @Adjacency(label = "latestProject")
    public void setLatestProject(final Project latestProject);

    @GremlinGroovy("_().sideEffect{x=it}.out('created').in('created').filter{it!=x}")
    public Collection<Person> getCoCreators();

}
