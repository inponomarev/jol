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

import org.openjdk.jol.ljv.nodes.ArrayNode;
import org.openjdk.jol.ljv.nodes.Node;
import org.openjdk.jol.ljv.nodes.ObjectNode;

import java.util.IdentityHashMap;

public class GraphvizVisualization implements Visualization {
    private final StringBuilder out = new StringBuilder();
    private final LJV ljv;
    private final IdentityHashMap<Object, String> alreadyDrawnObjectsIds = new IdentityHashMap<>();
    private boolean alreadyDrawnNull;

    public GraphvizVisualization(LJV ljv) {
        this.ljv = ljv;
    }

    @Override
    public boolean alreadyVisualized(Object obj) {
        if (obj == null) {
            return alreadyDrawnNull;
        }

        return alreadyDrawnObjectsIds.containsKey(obj);
    }

    @Override
    public void diagramBegin() {
        out.setLength(0); // Clearing String Builder before starting new DOT
        out.append("digraph Java {\n")
                .append("\trankdir=\"")
                .append(ljv.getDirection())
                .append("\";\n")
                .append("\tnode[shape=plaintext]\n");
    }

    @Override
    public String diagramEnd() {
        out.append("}\n");
        return out.toString();
    }

    @Override
    public void visitNull() {
        if (!alreadyDrawnNull) {
            out.append("\t").append(dotName(null)).append("[label=\"null\"").append(", shape=plaintext];\n");
            alreadyDrawnNull = true;
        }
    }

    @Override
    public void visitArrayBegin(ArrayNode arrayNode) {
        out.append("\t")
                .append(dotName(arrayNode.getValue()))
                .append("[label=<\n");

        if (arrayNode.areValuesPrimitive()) {
            out.append("\t\t<table border='0' cellborder='1' cellspacing='0'>\n");
        } else {
            out.append("\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n");
        }
        out.append("\t\t\t<tr>\n");
        out.append("\t\t\t\t<td>");
        out.append(arrayNode.getClassName());
        out.append("</td>\n");
    }

    @Override
    public void visitArrayElement(ArrayNode arrayNode, String element, int elementIndex) {
        out.append("\t\t\t\t<td");
        if (!arrayNode.areValuesPrimitive()) {
            out.append(" port=\"f").append(elementIndex).append("\"");
        }
        out.append(ljv.getArrayElementAttributes(arrayNode.getValue(), elementIndex))
                .append(">");

        // If array element is treated as primitive - than filling array cell with value
        // Otherwise cell will be empty, but arrow-connected with object it is containing
        if (arrayNode.areValuesPrimitive()) {
            out.append(Quote.quote(element));
        }

        out.append("</td>\n");
    }

    @Override
    public void visitArrayElementObjectConnection(Object array, int elementIndex, Object obj) {
        out.append("\t")
                .append(dotName(array))
                .append(":f")
                .append(elementIndex)
                .append(" -> ")
                .append(dotName(obj))
                .append("[label=\"")
                .append(elementIndex)
                .append("\",fontsize=12];\n");
    }


    @Override
    public void visitArrayEnd(Object array) {
        out.append("\t\t\t</tr>\n\t\t</table>\n\t>];\n");
    }

    @Override
    public void visitObjectBegin(ObjectNode objectNode) {
        out.append("\t")
                .append(dotName(objectNode.getValue()))
                .append("[label=<\n")
                .append("\t\t<table border='0' cellborder='1' cellspacing='0'>\n");

        // Adding header row with object class name
        out.append("\t\t\t<tr>\n");
        if (objectNode.getPrimitiveFieldsNum() > 0) {
            out.append("\t\t\t\t<td rowspan='")
                    .append(objectNode.getPrimitiveFieldsNum() + 1)
                    .append("'>");
        } else {
            out.append("\t\t\t\t<td>");
        }
        out.append(objectNode.getClassName())
                .append("</td>\n\t\t\t</tr>\n");
    }

    @Override
    public void visitObjectPrimitiveField(String fieldName, String fieldValueStr) {
        out.append("\t\t\t<tr>\n\t\t\t\t<td>");
        if (ljv.isShowFieldNamesInLabels()) {
            out.append(fieldName).append(": ");
        }
        out.append(Quote.quote(fieldValueStr));
        out.append("</td>\n\t\t\t</tr>\n");
    }

    @Override
    public void visitObjectFieldRelationWithNonPrimitiveObject(Object obj, Node childNode, String ljvFieldAttributes) {
        out.append("\t")
                .append(dotName(obj))
                .append(" -> ")
                .append(dotName(childNode.getValue()))
                .append("[label=\"")
                .append(childNode.getName())
                .append("\",fontsize=12")
                .append(ljvFieldAttributes.isEmpty() ? "" : "," + ljvFieldAttributes)
                .append("];\n");
    }

    @Override
    public void visitObjectEnd(Object obj) {
        out.append("\t\t</table>\n\t>");

        String cabs = ljv.getObjectAttributes(obj);
        if (!cabs.isEmpty()) {
            out.append(",").append(cabs);
        }
        out.append("];\n");
    }

    private String compIfAbsent(Object obj) {
        String value = alreadyDrawnObjectsIds.get(obj);
        if (value == null) {
            String newValue = "n" + (alreadyDrawnObjectsIds.size() + 1);
            alreadyDrawnObjectsIds.put(obj, newValue);
            value = newValue;
        }
        return value;
    }

    private String dotName(Object obj) {
        return obj == null ? "NULL" : compIfAbsent(obj);
    }
}
