// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;


public class FilterTest {
    @BeforeClass
    public static void setUp() {
        Main.proj = new Mercator();
    }
    
    @Test
    public void basic_test() throws ParseError {
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        n1.put("amenity", "parking");
        Node n2 = new Node(2);
        n2.put("fixme", "continue");
        ds.addPrimitive(n1);
        OsmPrimitive p = ds.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertNotNull(p);

        Collection<OsmPrimitive> all = new HashSet<OsmPrimitive>();
        all.addAll(Arrays.asList(new OsmPrimitive[] {n1, n2}));

        List<Filter> filters = new LinkedList<Filter>();
        Filter f1 = new Filter();
        f1.text = "fixme";
        f1.hiding = true;
        filters.addAll(Arrays.asList(new Filter[] {f1}));

        FilterMatcher filterMatcher = new FilterMatcher();
        filterMatcher.update(filters);

        FilterWorker.executeFilters(all, filterMatcher);

        assertTrue(n2.isDisabledAndHidden());
        assertTrue(!n1.isDisabled());
    }

    @Test
    public void filter_test() throws ParseError, IllegalDataException, FileNotFoundException {
        for (int i : new int [] {1,2,3, 11,12,13,14}) {
            DataSet ds = OsmReader.parseDataSet(new FileInputStream("data_nodist/filterTests.osm"), NullProgressMonitor.INSTANCE);

            List<Filter> filters = new LinkedList<Filter>();
            switch (i) {
                case 1: {
                    Filter f1 = new Filter();
                    f1.text = "power";
                    f1.hiding = true;
                    filters.add(f1);
                    break;
                }
                case 2: {
                    Filter f1 = new Filter();
                    f1.text = "highway";
                    f1.inverted = true;
                    filters.add(f1);
                    break;
                }
                case 3: {
                    Filter f1 = new Filter();
                    f1.text = "power";
                    f1.inverted = true;
                    f1.hiding = true;
                    Filter f2 = new Filter();
                    f2.text = "highway";
                    filters.addAll(Arrays.asList(new Filter[] {f1, f2}));
                    break;
                }
                case 11: {
                    Filter f1 = new Filter();
                    f1.text = "highway";
                    f1.inverted = true;
                    f1.hiding = true;
                    filters.add(f1);
                    break;
                }
                case 12: {
                    Filter f1 = new Filter();
                    f1.text = "highway";
                    f1.inverted = true;
                    f1.hiding = true;
                    Filter f2 = new Filter();
                    f2.text = "water";
                    f2.mode = SearchMode.remove;
                    filters.addAll(Arrays.asList(new Filter[] {f1, f2}));
                    break;
                }
                case 13: {
                    Filter f1 = new Filter();
                    f1.text = "highway";
                    f1.inverted = true;
                    f1.hiding = true;
                    Filter f2 = new Filter();
                    f2.text = "water";
                    f2.mode = SearchMode.remove;
                    Filter f3 = new Filter();
                    f3.text = "natural";
                    filters.addAll(Arrays.asList(new Filter[] {f1, f2, f3}));
                    break;
                }
                case 14: {
                    /* show all highways and all water features, but not lakes
                     * except those that have a name */
                    Filter f1 = new Filter();
                    f1.text = "highway";
                    f1.inverted = true;
                    f1.hiding = true;
                    Filter f2 = new Filter();
                    f2.text = "water";
                    f2.mode = SearchMode.remove;
                    Filter f3 = new Filter();
                    f3.text = "natural";
                    Filter f4 = new Filter();
                    f4.text = "name";
                    f4.mode = SearchMode.remove;
                    filters.addAll(Arrays.asList(new Filter[] {f1, f2, f3, f4}));
                    break;
                }
            }

            FilterMatcher filterMatcher = new FilterMatcher();
            filterMatcher.update(filters);

            FilterWorker.executeFilters(ds.allPrimitives(), filterMatcher);

            boolean foundAtLeastOne = false;
            System.err.println("Run #"+i);
            for (OsmPrimitive osm : ds.allPrimitives()) {
                String key = "source:RESULT"+i; // use key that counts as untagged
                if (osm.hasKey(key)) {
                    foundAtLeastOne = true;
//                    System.err.println("osm "+osm.getId()+" "+filterCode(osm)+" "+osm.get(key));
                    assertEquals(String.format("Run #%d Object %s", i,osm.toString()), filterCode(osm), osm.get(key));
                }
            }
            assertTrue(foundAtLeastOne);
        }
    }

    private String filterCode(OsmPrimitive osm) {
        if (!osm.isDisabled())
            return "v";
        if (!osm.isDisabledAndHidden())
            return "d";
        return "h";
    }
}
