/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.storage;

import com.graphhopper.routing.util.*;
import com.graphhopper.util.*;
import static com.graphhopper.util.GHUtility.*;
import com.graphhopper.util.shapes.BBox;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract test class to be extended for implementations of the Graph interface. Graphs
 * implementing GraphStorage should extend GraphStorageTest instead.
 * <p/>
 * @author Peter Karich
 */
public abstract class AbstractGraphTester
{
    private String location = "./target/graphstorage";
    protected int defaultSize = 100;
    protected String defaultGraph = "./target/graphstorage/default";
    protected EncodingManager encodingManager = new EncodingManager("CAR,FOOT");
    protected CarFlagEncoder carEncoder = (CarFlagEncoder) encodingManager.getEncoder("CAR");
    protected FootFlagEncoder footEncoder = (FootFlagEncoder) encodingManager.getEncoder("FOOT");
    EdgeFilter carOutFilter = new DefaultEdgeFilter(carEncoder, false, true);
    EdgeFilter carInFilter = new DefaultEdgeFilter(carEncoder, true, false);
    EdgeExplorer carOutExplorer;
    EdgeExplorer carInExplorer;
    EdgeExplorer carAllExplorer;
    private Graph graph;

    protected Graph createGraph()
    {
        Graph g = createGraph(defaultGraph, defaultSize);
        carOutExplorer = g.createEdgeExplorer(carOutFilter);
        carInExplorer = g.createEdgeExplorer(carInFilter);
        carAllExplorer = g.createEdgeExplorer();
        return g;
    }

    abstract Graph createGraph( String location, int size );

    @Before
    public void setUp()
    {
        Helper.removeDir(new File(location));
    }

    @After
    public void tearDown()
    {
        close(graph);
        Helper.removeDir(new File(location));
    }

    @Test
    public void testSetNodes()
    {
        graph = createGraph();
        for (int i = 0; i < defaultSize * 2; i++)
        {
            graph.setNode(i, 2 * i, 3 * i);
        }
        graph.edge(defaultSize + 1, defaultSize + 2, 10, true);
        graph.edge(defaultSize + 1, defaultSize + 3, 10, true);
        assertEquals(2, GHUtility.count(carAllExplorer.setBaseNode(defaultSize + 1)));
    }

    @Test
    public void testCreateLocation()
    {
        graph = createGraph();
        graph.edge(3, 1, 50, true);
        assertEquals(1, count(carOutExplorer.setBaseNode(1)));

        graph.edge(1, 2, 100, true);
        assertEquals(2, count(carOutExplorer.setBaseNode(1)));
    }

    @Test
    public void testEdges()
    {
        graph = createGraph();
        graph.edge(2, 1, 12, true);
        assertEquals(1, count(carOutExplorer.setBaseNode(2)));

        graph.edge(2, 3, 12, true);
        assertEquals(1, count(carOutExplorer.setBaseNode(1)));
        assertEquals(2, count(carOutExplorer.setBaseNode(2)));
        assertEquals(1, count(carOutExplorer.setBaseNode(3)));
    }

    @Test
    public void testUnidirectional()
    {
        graph = createGraph();

        graph.edge(1, 2, 12, false);
        graph.edge(1, 11, 12, false);
        graph.edge(11, 1, 12, false);
        graph.edge(1, 12, 12, false);
        graph.edge(3, 2, 112, false);
        EdgeIterator i = carOutExplorer.setBaseNode(2);
        assertFalse(i.next());

        assertEquals(1, GHUtility.count(carInExplorer.setBaseNode(1)));
        assertEquals(2, GHUtility.count(carInExplorer.setBaseNode(2)));
        assertEquals(0, GHUtility.count(carInExplorer.setBaseNode(3)));

        assertEquals(3, GHUtility.count(carOutExplorer.setBaseNode(1)));
        assertEquals(0, GHUtility.count(carOutExplorer.setBaseNode(2)));
        assertEquals(1, GHUtility.count(carOutExplorer.setBaseNode(3)));
        i = carOutExplorer.setBaseNode(3);
        i.next();
        assertEquals(2, i.getAdjNode());

        i = carOutExplorer.setBaseNode(1);
        assertTrue(i.next());        
        assertEquals(12, i.getAdjNode());
        assertTrue(i.next());
        assertEquals(11, i.getAdjNode());
        assertTrue(i.next());
        assertEquals(2, i.getAdjNode());
        assertFalse(i.next());
    }

    @Test
    public void testUnidirectionalEdgeFilter()
    {
        graph = createGraph();

        graph.edge(1, 2, 12, false);
        graph.edge(1, 11, 12, false);
        graph.edge(11, 1, 12, false);
        graph.edge(1, 12, 12, false);
        graph.edge(3, 2, 112, false);
        EdgeIterator i = carOutExplorer.setBaseNode(2);
        assertFalse(i.next());

        assertEquals(4, GHUtility.count(carAllExplorer.setBaseNode(1)));

        assertEquals(1, GHUtility.count(carInExplorer.setBaseNode(1)));
        assertEquals(2, GHUtility.count(carInExplorer.setBaseNode(2)));
        assertEquals(0, GHUtility.count(carInExplorer.setBaseNode(3)));

        assertEquals(3, GHUtility.count(carOutExplorer.setBaseNode(1)));
        assertEquals(0, GHUtility.count(carOutExplorer.setBaseNode(2)));
        assertEquals(1, GHUtility.count(carOutExplorer.setBaseNode(3)));
        i = carOutExplorer.setBaseNode(3);
        i.next();
        assertEquals(2, i.getAdjNode());

        i = carOutExplorer.setBaseNode(1);
        assertTrue(i.next());
        assertEquals(12, i.getAdjNode());
        assertTrue(i.next());
        assertEquals(11, i.getAdjNode());
        assertTrue(i.next());
        assertEquals(2, i.getAdjNode());        
        assertFalse(i.next());
    }

    @Test
    public void testUpdateUnidirectional()
    {
        graph = createGraph();

        graph.edge(1, 2, 12, false);
        graph.edge(3, 2, 112, false);
        EdgeIterator i = carOutExplorer.setBaseNode(2);
        assertFalse(i.next());
        i = carOutExplorer.setBaseNode(3);
        assertTrue(i.next());
        assertEquals(2, i.getAdjNode());
        assertFalse(i.next());

        graph.edge(2, 3, 112, false);
        i = carOutExplorer.setBaseNode(2);
        assertTrue(i.next());
        assertEquals(3, i.getAdjNode());
        i = carOutExplorer.setBaseNode(3);
        i.next();
        assertEquals(2, i.getAdjNode());
        assertFalse(i.next());
    }

    @Test
    public void testClone()
    {
        graph = createGraph();
        graph.edge(1, 2, 10, true);
        graph.setNode(0, 12, 23);
        graph.setNode(1, 8, 13);
        graph.setNode(2, 2, 10);
        graph.setNode(3, 5, 9);
        graph.edge(1, 3, 10, true);

        Graph clone = graph.copyTo(createGraph(location + "/clone", defaultSize));
        assertEquals(graph.getNodes(), clone.getNodes());
        assertEquals(count(carOutExplorer.setBaseNode(1)), count(clone.createEdgeExplorer(carOutFilter).setBaseNode(1)));
        clone.edge(1, 4, 10, true);
        assertEquals(3, count(clone.createEdgeExplorer(carOutFilter).setBaseNode(1)));
        assertEquals(graph.getBounds(), clone.getBounds());
        close(clone);
    }

    @Test
    public void testGetLocations()
    {
        graph = createGraph();
        graph.setNode(0, 12, 23);
        graph.setNode(1, 22, 23);
        assertEquals(2, graph.getNodes());

        graph.edge(0, 1, 10, true);
        assertEquals(2, graph.getNodes());

        graph.edge(0, 2, 10, true);
        assertEquals(3, graph.getNodes());
        close(graph);

        graph = createGraph();
        assertEquals(0, graph.getNodes());
    }

    @Test
    public void testCopyTo()
    {
        graph = createGraph();
        initExampleGraph(graph);
        Graph gs = new GraphStorage(new RAMDirectory(), encodingManager).setSegmentSize(8000).create(10);
        try
        {
            graph.copyTo(gs);
            checkExampleGraph(gs);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            assertTrue(ex.toString(), false);
        }

        try
        {
            close(graph);
            graph = createGraph();
            gs.copyTo(graph);
            checkExampleGraph(graph);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            assertTrue(ex.toString(), false);
        }
    }

    @Test
    public void testAddLocation()
    {
        graph = createGraph();
        initExampleGraph(graph);
        checkExampleGraph(graph);
    }

    protected void initExampleGraph( Graph g )
    {
        g.setNode(0, 12, 23);
        g.setNode(1, 38.33f, 135.3f);
        g.setNode(2, 6, 139);
        g.setNode(3, 78, 89);
        g.setNode(4, 2, 1);
        g.setNode(5, 7, 5);
        g.edge(0, 1, 12, true);
        g.edge(0, 2, 212, true);
        g.edge(0, 3, 212, true);
        g.edge(0, 4, 212, true);
        g.edge(0, 5, 212, true);
    }

    private void checkExampleGraph( Graph graph )
    {
        assertEquals(12f, graph.getLatitude(0), 1e-6);
        assertEquals(23f, graph.getLongitude(0), 1e-6);

        assertEquals(38.33f, graph.getLatitude(1), 1e-6);
        assertEquals(135.3f, graph.getLongitude(1), 1e-6);

        assertEquals(6, graph.getLatitude(2), 1e-6);
        assertEquals(139, graph.getLongitude(2), 1e-6);

        assertEquals(78, graph.getLatitude(3), 1e-6);
        assertEquals(89, graph.getLongitude(3), 1e-6);

        assertEquals(GHUtility.asSet(0), GHUtility.getNeighbors(carOutExplorer.setBaseNode((1))));
        assertEquals(GHUtility.asSet(5, 4, 3, 2, 1), GHUtility.getNeighbors(carOutExplorer.setBaseNode(0)));
        try
        {
            assertEquals(0, count(carOutExplorer.setBaseNode(6)));
            // for now return empty iterator
            // assertFalse(true);
        } catch (Exception ex)
        {
        }
    }

    @Test
    public void testDirectional()
    {
        graph = createGraph();
        graph.edge(1, 2, 12, true);
        graph.edge(2, 3, 12, false);
        graph.edge(3, 4, 12, false);
        graph.edge(3, 5, 12, true);
        graph.edge(6, 3, 12, false);

        assertEquals(1, count(carAllExplorer.setBaseNode(1)));
        assertEquals(1, count(carInExplorer.setBaseNode(1)));
        assertEquals(1, count(carOutExplorer.setBaseNode(1)));

        assertEquals(2, count(carAllExplorer.setBaseNode(2)));
        assertEquals(1, count(carInExplorer.setBaseNode(2)));
        assertEquals(2, count(carOutExplorer.setBaseNode(2)));

        assertEquals(4, count(carAllExplorer.setBaseNode(3)));
        assertEquals(3, count(carInExplorer.setBaseNode(3)));
        assertEquals(2, count(carOutExplorer.setBaseNode(3)));

        assertEquals(1, count(carAllExplorer.setBaseNode(4)));
        assertEquals(1, count(carInExplorer.setBaseNode(4)));
        assertEquals(0, count(carOutExplorer.setBaseNode(4)));

        assertEquals(1, count(carAllExplorer.setBaseNode(5)));
        assertEquals(1, count(carInExplorer.setBaseNode(5)));
        assertEquals(1, count(carOutExplorer.setBaseNode(5)));
    }

    @Test
    public void testDozendEdges()
    {
        graph = createGraph();
        graph.edge(1, 2, 12, true);
        assertEquals(1, count(carAllExplorer.setBaseNode(1)));

        graph.edge(1, 3, 13, false);
        assertEquals(2, count(carAllExplorer.setBaseNode(1)));

        graph.edge(1, 4, 14, false);
        assertEquals(3, count(carAllExplorer.setBaseNode(1)));

        graph.edge(1, 5, 15, false);
        assertEquals(4, count(carAllExplorer.setBaseNode(1)));

        graph.edge(1, 6, 16, false);
        assertEquals(5, count(carAllExplorer.setBaseNode(1)));

        graph.edge(1, 7, 16, false);
        assertEquals(6, count(carAllExplorer.setBaseNode(1)));

        graph.edge(1, 8, 16, false);
        assertEquals(7, count(carAllExplorer.setBaseNode(1)));

        graph.edge(1, 9, 16, false);
        assertEquals(8, count(carAllExplorer.setBaseNode(1)));
        assertEquals(8, count(carOutExplorer.setBaseNode(1)));

        assertEquals(1, count(carInExplorer.setBaseNode(1)));
        assertEquals(1, count(carInExplorer.setBaseNode(2)));
    }

    @Test
    public void testCheckFirstNode()
    {
        graph = createGraph();

        assertEquals(0, count(carAllExplorer.setBaseNode(1)));
        graph.edge(0, 1, 12, true);
        assertEquals(1, count(carAllExplorer.setBaseNode(1)));
    }

    @Test
    public void testDeleteNodeForUnidir()
    {
        graph = createGraph();
        graph.setNode(10, 10, 1);
        graph.setNode(6, 6, 1);
        graph.setNode(20, 20, 1);
        graph.setNode(21, 21, 1);

        graph.edge(10, 20, 10, false);
        graph.edge(21, 6, 10, false);

        graph.markNodeRemoved(0);
        graph.markNodeRemoved(7);
        assertEquals(22, graph.getNodes());
        graph.optimize();
        assertEquals(20, graph.getNodes());

        assertEquals(1, GHUtility.count(carInExplorer.setBaseNode(getIdOf(graph, 20))));
        assertEquals(0, GHUtility.count(carOutExplorer.setBaseNode(getIdOf(graph, 20))));

        assertEquals(1, GHUtility.count(carOutExplorer.setBaseNode(getIdOf(graph, 10))));
        assertEquals(0, GHUtility.count(carInExplorer.setBaseNode(getIdOf(graph, 10))));

        assertEquals(1, GHUtility.count(carInExplorer.setBaseNode(getIdOf(graph, 6))));
        assertEquals(0, GHUtility.count(carOutExplorer.setBaseNode(getIdOf(graph, 6))));

        assertEquals(1, GHUtility.count(carOutExplorer.setBaseNode(getIdOf(graph, 21))));
        assertEquals(0, GHUtility.count(carInExplorer.setBaseNode(getIdOf(graph, 21))));
    }

    @Test
    public void testComplexDeleteNode()
    {
        testDeleteNodes(21);
    }

    @Test
    public void testComplexDeleteNode2()
    {
        testDeleteNodes(6);
    }

    public void testDeleteNodes( int fillToSize )
    {
        graph = createGraph();
        graph.setNode(0, 12, 23);
        graph.setNode(1, 38.33f, 135.3f);
        graph.setNode(2, 3, 3);
        graph.setNode(3, 78, 89);
        graph.setNode(4, 2, 1);
        graph.setNode(5, 2.5f, 1);

        int deleted = 2;
        for (int i = 6; i < fillToSize; i++)
        {
            graph.setNode(i, i * 1.5, i * 1.6);
            if (i % 3 == 0)
            {
                graph.markNodeRemoved(i);
                deleted++;
            } else
            {
                // connect to
                // ... a deleted node
                graph.edge(i, 0, 10 * i, true);
                // ... a non-deleted and non-moved node
                graph.edge(i, 2, 10 * i, true);
                // ... a moved node
                graph.edge(i, fillToSize - 1, 10 * i, true);
            }
        }

        graph.edge(0, 1, 10, true);
        graph.edge(0, 3, 20, false);
        graph.edge(3, 5, 20, true);
        graph.edge(1, 5, 20, false);

        graph.markNodeRemoved(0);
        graph.markNodeRemoved(2);
        // no deletion happend
        assertEquals(fillToSize, graph.getNodes());

        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));

        // now actually perform deletion
        graph.optimize();

        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));

        assertEquals(fillToSize - deleted, graph.getNodes());
        int id1 = getIdOf(graph, 38.33f);
        assertEquals(135.3f, graph.getLongitude(id1), 1e-4);
        assertTrue(containsLatitude(graph, carAllExplorer.setBaseNode(id1), 2.5));
        assertFalse(containsLatitude(graph, carAllExplorer.setBaseNode(id1), 12));

        int id3 = getIdOf(graph, 78);
        assertEquals(89, graph.getLongitude(id3), 1e-4);
        assertTrue(containsLatitude(graph, carAllExplorer.setBaseNode(id3), 2.5));
        assertFalse(containsLatitude(graph, carAllExplorer.setBaseNode(id3), 12));
    }

    public boolean containsLatitude( Graph g, EdgeIterator iter, double latitude )
    {
        while (iter.next())
        {
            if (Math.abs(g.getLatitude(iter.getAdjNode()) - latitude) < 1e-4)
            {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testSimpleDelete()
    {
        graph = createGraph();
        graph.setNode(0, 12, 23);
        graph.setNode(1, 38.33f, 135.3f);
        graph.setNode(2, 3, 3);
        graph.setNode(3, 78, 89);

        graph.edge(3, 0, 21, true);
        graph.edge(5, 0, 22, true);
        graph.edge(5, 3, 23, true);

        graph.markNodeRemoved(0);
        graph.markNodeRemoved(3);

        assertEquals(6, graph.getNodes());
        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));

        // now actually perform deletion
        graph.optimize();

        assertEquals(4, graph.getNodes());
        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));
        // shouldn't change anything
        graph.optimize();
        assertEquals(4, graph.getNodes());
        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));
    }

    @Test
    public void testSimpleDelete2()
    {
        graph = createGraph();
        assertEquals(-1, getIdOf(graph, 12));
        graph.setNode(9, 9, 1);
        assertEquals(-1, getIdOf(graph, 12));

        graph.setNode(11, 11, 1);
        graph.setNode(12, 12, 1);


        // mini subnetwork which gets completely removed:
        graph.edge(5, 10, 510, true);
        graph.markNodeRemoved(5);
        graph.markNodeRemoved(10);

        PointList pl = new PointList();
        pl.add(1, 2);
        pl.add(1, 3);
        graph.edge(9, 11, 911, true).setWayGeometry(pl);
        graph.edge(9, 12, 912, true).setWayGeometry(pl);

        assertEquals(13, graph.getNodes());
        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));

        // perform deletion
        graph.optimize();

        assertEquals(11, graph.getNodes());
        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));

        int id11 = getIdOf(graph, 11); // is now 10
        int id12 = getIdOf(graph, 12); // is now 5
        int id9 = getIdOf(graph, 9);   // is now 9
        assertEquals(GHUtility.asSet(id12, id11), GHUtility.getNeighbors(carAllExplorer.setBaseNode(id9)));
        assertEquals(GHUtility.asSet(id9), GHUtility.getNeighbors(carAllExplorer.setBaseNode(id11)));
        assertEquals(GHUtility.asSet(id9), GHUtility.getNeighbors(carAllExplorer.setBaseNode(id12)));

        EdgeIterator iter = carAllExplorer.setBaseNode(id9);
        assertTrue(iter.next());
        assertEquals(id12, iter.getAdjNode());
        assertEquals(2, iter.getWayGeometry().getLongitude(0), 1e-7);
        
        assertTrue(iter.next());
        assertEquals(id11, iter.getAdjNode());
        assertEquals(2, iter.getWayGeometry().getLongitude(0), 1e-7);        
    }

    @Test
    public void testSimpleDelete3()
    {
        graph = createGraph();
        graph.setNode(7, 7, 1);
        graph.setNode(8, 8, 1);
        graph.setNode(9, 9, 1);
        graph.setNode(11, 11, 1);

        // mini subnetwork which gets completely removed:
        graph.edge(5, 10, 510, true);
        graph.markNodeRemoved(3);
        graph.markNodeRemoved(4);
        graph.markNodeRemoved(5);
        graph.markNodeRemoved(10);

        graph.edge(9, 11, 911, true);
        graph.edge(7, 9, 78, true);
        graph.edge(8, 9, 89, true);

        // perform deletion
        graph.optimize();

        assertEquals(Arrays.<String>asList(), GHUtility.getProblems(graph));

        assertEquals(3, GHUtility.count(carAllExplorer.setBaseNode(getIdOf(graph, 9))));
        assertEquals(1, GHUtility.count(carAllExplorer.setBaseNode(getIdOf(graph, 7))));
        assertEquals(1, GHUtility.count(carAllExplorer.setBaseNode(getIdOf(graph, 8))));
        assertEquals(1, GHUtility.count(carAllExplorer.setBaseNode(getIdOf(graph, 11))));
    }

    @Test
    public void testDeleteAndOptimize()
    {
        graph = createGraph();
        graph.setNode(20, 10, 10);
        graph.setNode(21, 10, 11);
        graph.markNodeRemoved(20);
        graph.optimize();
        assertEquals(11, graph.getLongitude(20), 1e-5);
    }

    @Test
    public void testBounds()
    {
        graph = createGraph();
        BBox b = graph.getBounds();
        assertEquals(BBox.INVERSE.maxLat, b.maxLat, 1e-6);

        graph.setNode(0, 10, 20);
        assertEquals(10, b.maxLat, 1e-6);
        assertEquals(20, b.maxLon, 1e-6);

        graph.setNode(0, 15, -15);
        assertEquals(15, b.maxLat, 1e-6);
        assertEquals(20, b.maxLon, 1e-6);
        assertEquals(10, b.minLat, 1e-6);
        assertEquals(-15, b.minLon, 1e-6);
    }

    @Test
    public void testFlags()
    {
        graph = createGraph();
        graph.edge(0, 1, 10, carEncoder.flags(120, true));
        graph.edge(2, 3, 10, carEncoder.flags(10, false));

        EdgeIterator iter = carAllExplorer.setBaseNode(0);
        assertTrue(iter.next());
        assertEquals(carEncoder.flags(120, true), iter.getFlags());

        iter = carAllExplorer.setBaseNode(2);
        assertTrue(iter.next());
        assertEquals(carEncoder.flags(10, false), iter.getFlags());
    }

    @Test
    public void testEdgeProperties()
    {
        graph = createGraph();
        EdgeIteratorState iter1 = graph.edge(0, 1, 10, true);
        EdgeIteratorState iter2 = graph.edge(0, 2, 20, true);

        int edgeId = iter1.getEdge();
        EdgeIteratorState iter = graph.getEdgeProps(edgeId, 0);
        assertEquals(10, iter.getDistance(), 1e-5);

        edgeId = iter2.getEdge();
        iter = graph.getEdgeProps(edgeId, 0);
        assertEquals(2, iter.getBaseNode());
        assertEquals(0, iter.getAdjNode());
        assertEquals(20, iter.getDistance(), 1e-5);

        iter = graph.getEdgeProps(edgeId, 2);
        assertEquals(0, iter.getBaseNode());
        assertEquals(2, iter.getAdjNode());
        assertEquals(20, iter.getDistance(), 1e-5);

        // minor API glitch: should be RawEdgeIterator
        iter = graph.getEdgeProps(edgeId, -1);
        assertFalse(iter == null);
        assertEquals(0, iter.getBaseNode());
        assertEquals(2, iter.getAdjNode());

        iter = graph.getEdgeProps(edgeId, 1);
        assertTrue(iter == null);

        // delete
        graph.markNodeRemoved(1);
        graph.optimize();

        // throw exception if accessing deleted edge
        try
        {
            graph.getEdgeProps(iter1.getEdge(), -1);
            assertTrue(false);
        } catch (Exception ex)
        {
        }
    }

    @Test
    public void testCreateDuplicateEdges()
    {
        graph = createGraph();
        graph.edge(2, 1, 12, true);
        graph.edge(2, 3, 12, true);
        graph.edge(2, 3, 13, false);
        assertEquals(3, GHUtility.count(carOutExplorer.setBaseNode(2)));

        // no exception        
        graph.getEdgeProps(1, 3);

        // raise exception
        try
        {
            graph.getEdgeProps(4, 3);
            assertTrue(false);
        } catch (Exception ex)
        {
        }
        try
        {
            graph.getEdgeProps(-1, 3);
            assertTrue(false);
        } catch (Exception ex)
        {
        }

        EdgeIterator iter = carOutExplorer.setBaseNode(2);        
        assertTrue(iter.next());
        EdgeIteratorState oneIter = graph.getEdgeProps(iter.getEdge(), 3);
        assertEquals(13, oneIter.getDistance(), 1e-6);
        assertEquals(2, oneIter.getBaseNode());
        assertTrue(carEncoder.isForward(oneIter.getFlags()));
        assertFalse(carEncoder.isBoth(oneIter.getFlags()));

        oneIter = graph.getEdgeProps(iter.getEdge(), 2);
        assertEquals(13, oneIter.getDistance(), 1e-6);
        assertEquals(3, oneIter.getBaseNode());
        assertTrue(carEncoder.isBackward(oneIter.getFlags()));
        assertFalse(carEncoder.isBoth(oneIter.getFlags()));

        graph.edge(3, 2, 14, true);
        assertEquals(4, GHUtility.count(carOutExplorer.setBaseNode(2)));
    }

    @Test
    public void testIdenticalNodes()
    {
        graph = createGraph();
        graph.edge(0, 0, 100, true);
        assertEquals(1, GHUtility.count(carAllExplorer.setBaseNode(0)));
    }

    @Test
    public void testIdenticalNodes2()
    {
        graph = createGraph();
        graph.edge(0, 0, 100, false);
        graph.edge(0, 0, 100, false);
        assertEquals(2, GHUtility.count(carAllExplorer.setBaseNode(0)));
    }

    @Test
    public void testEdgeReturn()
    {
        graph = createGraph();
        EdgeIteratorState iter = graph.edge(4, 10, 100, carEncoder.flags(10, false));
        assertEquals(4, iter.getBaseNode());
        assertEquals(10, iter.getAdjNode());
        iter = graph.edge(14, 10, 100, carEncoder.flags(10, false));
        assertEquals(14, iter.getBaseNode());
        assertEquals(10, iter.getAdjNode());
    }

    @Test
    public void testPillarNodes()
    {
        graph = createGraph();
        PointList pointList = Helper.createPointList(1, 1, 1, 2, 1, 3);
        graph.edge(0, 4, 100, carEncoder.flags(10, false)).setWayGeometry(pointList);
        pointList = Helper.createPointList(1, 5, 1, 6, 1, 7, 1, 8, 1, 9);
        graph.edge(4, 10, 100, carEncoder.flags(10, false)).setWayGeometry(pointList);
        pointList = Helper.createPointList(1, 13, 1, 12, 1, 11);
        graph.edge(14, 0, 100, carEncoder.flags(10, false)).setWayGeometry(pointList);

        // if tower node requested => return only tower nodes
        EdgeIterator iter = carAllExplorer.setBaseNode(0);
        assertTrue(iter.next());
        assertEquals(14, iter.getAdjNode());
        assertPList(Helper.createPointList(1, 11, 1, 12, 1, 13.0), iter.getWayGeometry());        
        assertTrue(iter.next());
        assertEquals(4, iter.getAdjNode());
        assertPList(Helper.createPointList(1, 1, 1, 2, 1, 3), iter.getWayGeometry());        
        assertFalse(iter.next());

        iter = carOutExplorer.setBaseNode(0);
        assertTrue(iter.next());
        assertPList(Helper.createPointList(1, 1, 1, 2, 1, 3), iter.getWayGeometry());
        assertEquals(4, iter.getAdjNode());
        assertFalse(iter.next());

        iter = carInExplorer.setBaseNode(10);
        assertTrue(iter.next());
        assertPList(Helper.createPointList(1, 9, 1, 8, 1, 7, 1, 6, 1, 5), iter.getWayGeometry());
        assertEquals(4, iter.getAdjNode());
        assertFalse(iter.next());
    }

    @Test
    public void testFootMix()
    {
        graph = createGraph();
        graph.edge(0, 1, 10, footEncoder.flags(10, true));
        graph.edge(0, 2, 10, carEncoder.flags(10, true));
        graph.edge(0, 3, 10, footEncoder.flags(10, true) | carEncoder.flags(10, true));
        EdgeExplorer footOutExplorer = graph.createEdgeExplorer(new DefaultEdgeFilter(footEncoder, false, true));
        assertEquals(GHUtility.asSet(3, 1), GHUtility.getNeighbors(footOutExplorer.setBaseNode(0)));
        assertEquals(GHUtility.asSet(3, 2), GHUtility.getNeighbors(carOutExplorer.setBaseNode(0)));
    }

    @Test
    public void testGetAllEdges()
    {
        graph = createGraph();
        graph.edge(0, 1, 2, true);
        graph.edge(3, 1, 1, false);
        graph.edge(3, 2, 1, false);

        EdgeIterator iter = graph.getAllEdges();
        assertTrue(iter.next());
        int edgeId = iter.getEdge();
        assertEquals(0, iter.getBaseNode());
        assertEquals(1, iter.getAdjNode());
        assertEquals(2, iter.getDistance(), 1e-6);

        assertTrue(iter.next());
        int edgeId2 = iter.getEdge();
        assertEquals(1, edgeId2 - edgeId);
        assertEquals(1, iter.getBaseNode());
        assertEquals(3, iter.getAdjNode());

        assertTrue(iter.next());
        assertEquals(2, iter.getBaseNode());
        assertEquals(3, iter.getAdjNode());

        assertFalse(iter.next());
    }

    @Test
    public void testGetAllEdgesWithDelete()
    {
        graph = createGraph();
        graph.setNode(0, 0, 5);
        graph.setNode(1, 1, 5);
        graph.setNode(2, 2, 5);
        graph.setNode(3, 3, 5);
        graph.edge(0, 1, 1, true);
        graph.edge(0, 2, 1, true);
        graph.edge(1, 2, 1, true);
        graph.edge(2, 3, 1, true);
        AllEdgesIterator iter = graph.getAllEdges();
        assertEquals(4, GHUtility.count(iter));
        assertEquals(4, iter.getMaxId());

        // delete
        graph.markNodeRemoved(1);
        graph.optimize();
        iter = graph.getAllEdges();
        assertEquals(2, GHUtility.count(iter));
        assertEquals(4, iter.getMaxId());
    }

    public static void assertPList( PointList expected, PointList list )
    {
        assertEquals("size of point lists is not equal", expected.getSize(), list.getSize());
        for (int i = 0; i < expected.getSize(); i++)
        {
            assertEquals(expected.getLatitude(i), list.getLatitude(i), 1e-4);
            assertEquals(expected.getLongitude(i), list.getLongitude(i), 1e-4);
        }
    }

    public static int getIdOf( Graph g, double latitude )
    {
        int s = g.getNodes();
        for (int i = 0; i < s; i++)
        {
            if (Math.abs(g.getLatitude(i) - latitude) < 1e-4)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Windows forces us to close files properly and so we need to close the graph properly if it
     * supports closeable
     */
    static void close( Object o )
    {
        if (o == null)
        {
            return;
        }
        if (o instanceof Closeable)
        {
            try
            {
                ((Closeable) o).close();
            } catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    @Test
    public void testNameIndex()
    {
        graph = createGraph();
        EdgeIteratorState iter1 = graph.edge(0, 1, 10, true);
        iter1.setName("named street1");

        EdgeIteratorState iter2 = graph.edge(0, 1, 10, true);
        iter2.setName("named street2");

        assertEquals("named street1", graph.getEdgeProps(iter1.getEdge(), -1).getName());
        assertEquals("named street2", graph.getEdgeProps(iter2.getEdge(), -1).getName());
    }
}
