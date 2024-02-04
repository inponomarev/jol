/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jol.ljv;

import org.junit.Test;
import org.approvaltests.Approvals;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.ljv.provider.ObjectAttributesProvider;
import org.openjdk.jol.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assume.assumeTrue;


public class LJVTest extends VersionGuardedTest {
    @Test
    public void stringIsNotAPrimitiveType() {
        assumeTrue(is8());
        String actualGraph = new LJV().drawGraph("Hello");
        Approvals.verify(actualGraph);
    }

    @Test
    public void objectArraysHoldReferencesPrimitiveArraysHoldValues() {
        assumeTrue(is8());
        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setIgnorePrivateFields(false)
                .drawGraph(
                        new Object[]{new String[]{"a", "b", "c"}, new int[]{1, 2, 3}}
                );
        Approvals.verify(actual_graph);
    }

    @Test
    public void assignmentDoesNotCreateANewObject() {
        assumeTrue(is8());
        String x = "Hello";
        String y = x;
        String actual_graph = new LJV().drawGraph(new Object[]{x, y});
        Approvals.verify(actual_graph);
    }

    @Test
    public void assignmentWithNewCreateANewObject() {
        assumeTrue(is8());
        String x = "Hello";
        String y = new String(x);
        String actual_graph = new LJV().drawGraph(new Object[]{x, y});
        Approvals.verify(actual_graph);
    }

    @Test
    public void stringIntern() {
        assumeTrue(is8());
        String x = "Hello";
        String y = "Hello";
        String actual_graph = new LJV().drawGraph(new Object[]{x, y.intern()});
        Approvals.verify(actual_graph);
    }

    @Test
    public void multiDimensionalArrays() {
        assumeTrue(is8());
        String actual_graph = new LJV().drawGraph(new int[4][5]);
        Approvals.verify(actual_graph);
    }

    @Test
    public void reversedMultiDimensionalArrays() {
        assumeTrue(is8());
        String actual_graph = new LJV().setDirection(Direction.LR).drawGraph(new int[4][5]);
        Approvals.verify(actual_graph);
    }

    @Test
    public void cyclicalStructuresClassesWithAndWithoutAToStringAndWithoutContext() {
        assumeTrue(is8());
        Node n1 = new Node("A");
        n1.level = 1;
        AnotherNode n2 = new AnotherNode("B");
        n2.level = 2;
        AnotherNode n3 = new AnotherNode("C");
        n3.level = 2;

        n1.left = n2;
        n1.right = n3;
        n1.right.left = n1;
        n1.right.right = n1;
        String actual_graph = new LJV()
                .addFieldAttribute("left", "color=red,fontcolor=red")
                .addFieldAttribute("right", "color=blue,fontcolor=blue")
                .addClassAttribute(Node.class, "color=pink,style=filled")
                .addIgnoreField("level")
                .setTreatAsPrimitive(String.class)
                .setShowFieldNamesInLabels(false)
                .drawGraph(n1);
        Approvals.verify(actual_graph);
    }

    @Test
    public void paulsExample() {
        assumeTrue(is8());
        ArrayList<Object> a = new ArrayList<>();
        a.add(new Person("Albert", Gender.MALE, 35));
        a.add(new Person("Betty", Gender.FEMALE, 20));
        a.add(new java.awt.Point(100, -100));

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Gender.class)
                .addIgnoreField("hash")
                .addIgnoreField("count")
                .addIgnoreField("offset")
                .drawGraph(a);
        Approvals.verify(actual_graph);
    }

    @Test
    public void multipleRoots() {
        assumeTrue(is8());
        ArrayList<Object> a = new ArrayList<>();
        Person p = new Person("Albert", Gender.MALE, 35);
        Person p2 = new Person("Albert", Gender.MALE, 35);
        String actual_graph = new LJV().addRoot(p).addRoot(p).addRoot(p).addRoot(p2).drawGraph();
        Approvals.verify(actual_graph);
    }

    @Test
    public void testNull() {
        String actualGraph = new LJV().drawGraph(null);
        Approvals.verify(actualGraph);
    }

    @Test
    public void testEmptyArray() {
        String dot = new LJV().drawGraph(new int[0][0]);
        Approvals.verify(dot);
    }

    @Test
    public void testMultiNull() {
        assumeTrue(is8());
        String actualGraph = new LJV().addRoot(null).addRoot(null).drawGraph();
        Approvals.verify(actualGraph);
    }

    @Test
    public void testMixedNullsAndNotNulls() {
        assumeTrue(is8());
        String actualGraph = new LJV().addRoot(null)
                .addRoot(new Object()).addRoot(new Object()).addRoot(null).drawGraph();
        Approvals.verify(actualGraph);
    }

    @Test
    public void treeMap() {
        assumeTrue(is8());
        TreeMap<String, Integer> map = new TreeMap<>();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);
        map.put("F", 4);
        map.put("G", 4);
        map.put("H", 4);
        map.put("J", 4);


        String actualGraph = new LJV()
                .setIgnoreNullValuedFields(true)
                .setTreatAsPrimitive(Integer.class)
                .setTreatAsPrimitive(String.class)
                .addObjectAttributesProvider(new ObjectAttributesProvider() {
                    @Override
                    public String getAttribute(Object o) {
                        return LJVTest.this.redBlack(o);
                    }
                })
                .drawGraph(map);
        Approvals.verify(actualGraph);
    }


    private String redBlack(Object o) {
        Set<Field> colorFields = new HashSet<>();
        for (FieldLayout field : ClassLayout.parseClass(o.getClass()).fields()) {
            Field f = field.data().refField();
            if ("color".equals(f.getName()) && f.getType().equals(boolean.class)) {
                colorFields.add(f);
            }
        }

        if (colorFields.isEmpty()) {
            return "";
        } else {
            Field colorField = colorFields.iterator().next();
            boolean b = (boolean) ObjectUtils.value(o, colorField);
            return b ? "color=black" : "color=red";
        }
    }


    @Test
    public void linkedHashMap() {
        assumeTrue(is8());
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Integer.class)
                .setIgnoreNullValuedFields(true)
                .drawGraph(map);

        Approvals.verify(actual_graph);
    }

    @Test
    public void hashMap() {
        assumeTrue(is8());
        HashMap<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Integer.class)
                .setIgnoreNullValuedFields(true)
                .drawGraph(map);
        Approvals.verify(actual_graph);
    }

    @Test
    public void hashMapCollision2() {
        assumeTrue(is8());
        List<String> collisionString = new HashCodeCollision().genCollisionString(8);
        HashMap<String, Integer> map = new HashMap<>();

        for (int i = 0; i < collisionString.size(); i++) {
            map.put(collisionString.get(i), i);
        }

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Integer.class)
                .drawGraph(map);

        Approvals.verify(actual_graph);
    }


    @Test
    public void wrappedObjects() {
        assumeTrue(is8());
        String actual_graph = new LJV().drawGraph(new Example());
        Approvals.verify(actual_graph);
    }

    @Test
    public void linkedList() {
        assumeTrue(is8());
        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.add(1);
        linkedList.add(42);
        linkedList.add(21);

        String actual_graph = new LJV()
                .setTreatAsPrimitive(Integer.class)
                .addFieldAttribute("next", "color=red,fontcolor=red")
                .addFieldAttribute("prev", "color=blue,fontcolor=blue")
                .addFieldAttribute("first", "color=red,fontcolor=red")
                .addFieldAttribute("last", "color=red,fontcolor=red")
                .drawGraph(linkedList);
        Approvals.verify(actual_graph);
    }

    @Test
    public void testArrayWithHighlighting() {
        assumeTrue(is8());
        LJV ljv = new LJV()
                .setTreatAsPrimitive(Integer.class)
                .highlightChangingArrayElements();
        Deque<Integer> arrayDeque = new ArrayDeque<>(4);
        arrayDeque.add(1);
        arrayDeque.add(2);
        arrayDeque.add(3);
        ljv.drawGraph(arrayDeque);
        arrayDeque.poll();
        arrayDeque.poll();
        Approvals.verify(ljv.drawGraph(arrayDeque));
    }

    @Test
    public void testNewObjectsHighlighting() {
        assumeTrue(is8());
        LJV ljv = new LJV()
                .setTreatAsPrimitive(Integer.class)
                .setTreatAsPrimitive(String.class)
                .setIgnoreNullValuedFields(true)
                .addIgnoreField("color")
                .highlightNewObjects();

        Map<String, Integer> map = new TreeMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        ljv.drawGraph(map);
        map.put("four", 4);
        Approvals.verify(ljv.drawGraph(map));
    }

    @Test
    public void arrayWithFieldAttribute() {
        assumeTrue(is8());
        String actualGraph = new LJV()
                .addFieldAttribute("value", "color=red,fontcolor=red")
                .drawGraph("Hello");
        Approvals.verify(actualGraph);
    }

    @Test
    public void twoObjectsLinksToOneArray() {
        assumeTrue(is8());
        int[] arr = {1, 2, 3};
        A x = new A(arr);
        B y = new B(arr);
        String actualGraph = new LJV()
                .addFieldAttribute("a", "color=blue,fontcolor=red")
                .addFieldAttribute("b", "color=yellow,fontcolor=green")
                .addRoot(x).addRoot(y)
                .drawGraph();
        Approvals.verify(actualGraph);
    }

    @Test
    public void arrayItemLinksToArray() {
        assumeTrue(is8());
        ArrayItem child = new ArrayItem();
        ArrayItem[] array = {child};
        child.prev = array;
        String actualGraph = new LJV().drawGraph(array);
        Approvals.verify(actualGraph);
    }
}
