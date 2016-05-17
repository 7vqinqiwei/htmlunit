/*
 * Copyright (c) 2002-2016 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.javascript.host.dom;

import static com.gargoylesoftware.js.nashorn.internal.objects.annotations.BrowserFamily.CHROME;
import static com.gargoylesoftware.js.nashorn.internal.objects.annotations.BrowserFamily.FF;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlInlineFrame;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptObject;
import com.gargoylesoftware.htmlunit.javascript.host.Element2;
import com.gargoylesoftware.htmlunit.javascript.host.event.EventTarget2;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLHtmlElement2;
import com.gargoylesoftware.js.nashorn.ScriptUtils;
import com.gargoylesoftware.js.nashorn.internal.objects.Global;
import com.gargoylesoftware.js.nashorn.internal.objects.annotations.Function;
import com.gargoylesoftware.js.nashorn.internal.objects.annotations.Getter;
import com.gargoylesoftware.js.nashorn.internal.objects.annotations.WebBrowser;
import com.gargoylesoftware.js.nashorn.internal.runtime.Context;
import com.gargoylesoftware.js.nashorn.internal.runtime.PrototypeObject;
import com.gargoylesoftware.js.nashorn.internal.runtime.ScriptFunction;
import com.gargoylesoftware.js.nashorn.internal.runtime.ScriptObject;

public class Node2 extends EventTarget2 {

    public static Node2 constructor(final boolean newObj, final Object self) {
        final Node2 host = new Node2();
        host.setProto(((Global) self).getPrototype(host.getClass()));
        ScriptUtils.initialize(host);
        return host;
    }

    /**
     * Returns the owner document.
     * @return the document
     */
    @Getter
    public Object getOwnerDocument() {
        final Object document = getDomNodeOrDie().getOwnerDocument();
        if (document != null) {
            return ((SgmlPage) document).getScriptObject2();
        }
        return null;
    }

    /**
     * Gets the JavaScript property {@code parentElement}.
     * @return the parent element
     * @see #getParentNode()
     */
    @Getter({@WebBrowser(FF), @WebBrowser(CHROME)})
    public Element2 getParentElement() {
        final Node2 parent = getParent();
        if (!(parent instanceof Element2)) {
            return null;
        }
        return (Element2) parent;
    }

    /**
     * Returns this node's parent node.
     * @return this node's parent node
     */
    public final Node2 getParent() {
        return getJavaScriptNode(getDomNodeOrDie().getParentNode());
    }

    /**
     * Gets the JavaScript node for a given DomNode.
     * @param domNode the DomNode
     * @return the JavaScript node or null if the DomNode was null
     */
    protected Node2 getJavaScriptNode(final DomNode domNode) {
        if (domNode == null) {
            return null;
        }
        return (Node2) getScriptableFor(domNode);
    }

    /**
     * Returns the JavaScript object that corresponds to the specified object.
     * New JavaScript objects will be created as needed. If a JavaScript object
     * cannot be created for a domNode then NOT_FOUND will be returned.
     *
     * @param object a {@link DomNode} or a {@link WebWindow}
     * @return the JavaScript object or NOT_FOUND
     */
    protected SimpleScriptObject getScriptableFor(final Object object) {
        if (object instanceof WebWindow) {
            return (SimpleScriptObject) ((WebWindow) object).getScriptObject2();
        }

        final DomNode domNode = (DomNode) object;

        final Object scriptObject = domNode.getScriptObject2();
        if (scriptObject != null) {
            return (SimpleScriptObject) scriptObject;
        }
        return makeScriptableFor(domNode);
    }

    /**
     * Adds a DOM node to the node.
     * @param childObject the node to add to this node
     * @return the newly added child node
     */
    @Function
    public Object appendChild(final Object childObject) {
        Object appendedChild = null;
        if (childObject instanceof Node2) {
            final Node2 childNode = (Node2) childObject;

            // is the node allowed here?
            if (!isNodeInsertable(childNode)) {
                throw new RuntimeException("Node cannot be inserted at the specified point in the hierarchy");
//                throw new RuntimeException(
//                    new DOMException("Node cannot be inserted at the specified point in the hierarchy",
//                        DOMException.HIERARCHY_REQUEST_ERR));
            }

            // Get XML node for the DOM node passed in
            final DomNode childDomNode = childNode.getDomNodeOrDie();

            // Get the parent XML node that the child should be added to.
            final DomNode parentNode = getDomNodeOrDie();

            // Append the child to the parent node
            parentNode.appendChild(childDomNode);
            appendedChild = childObject;

            initInlineFrameIfNeeded(childDomNode);
            for (final DomNode domNode : childDomNode.getDescendants()) {
                initInlineFrameIfNeeded(domNode);
            }
        }
        return appendedChild;
    }

    /**
     * If we have added a new iframe that
     * had no source attribute, we have to take care the
     * 'onload' handler is triggered.
     *
     * @param childDomNode
     */
    private static void initInlineFrameIfNeeded(final DomNode childDomNode) {
        if (childDomNode instanceof HtmlInlineFrame) {
            final HtmlInlineFrame frame = (HtmlInlineFrame) childDomNode;
            if (DomElement.ATTRIBUTE_NOT_DEFINED == frame.getSrcAttribute()) {
                frame.loadInnerPage();
            }
        }
    }

    /**
     * Indicates if the node can be inserted.
     * @param childObject the node
     * @return {@code false} if it is not allowed here
     */
    private static boolean isNodeInsertable(final Node2 childObject) {
        if (childObject instanceof HTMLHtmlElement2) {
            final DomNode domNode = childObject.getDomNodeOrDie();
            return !(domNode.getPage().getDocumentElement() == domNode);
        }
        return true;
    }

    /**
     * Gets the JavaScript property {@code firstChild} for the node that
     * contains the current node.
     * @return the first child node or null if the current node has
     * no children.
     */
    @Getter
    public Node2 getFirstChild() {
        return getJavaScriptNode(getDomNodeOrDie().getFirstChild());
    }

    /**
     * Gets the JavaScript property {@code lastChild} for the node that
     * contains the current node.
     * @return the last child node or null if the current node has
     * no children.
     */
    @Getter
    public Node2 getLastChild() {
        return getJavaScriptNode(getDomNodeOrDie().getLastChild());
    }

    private static MethodHandle staticHandle(final String name, final Class<?> rtype, final Class<?>... ptypes) {
        try {
            return MethodHandles.lookup().findStatic(Node2.class,
                    name, MethodType.methodType(rtype, ptypes));
        }
        catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class FunctionConstructor extends ScriptFunction {
        public FunctionConstructor() {
            super("Node", 
                    staticHandle("constructor", Node2.class, boolean.class, Object.class),
                    null);
            final Prototype prototype = new Prototype();
            PrototypeObject.setConstructor(prototype, this);
            setPrototype(prototype);
        }
    }

    public static final class Prototype extends PrototypeObject {
        public ScriptFunction appendChild;

        public ScriptFunction G$appendChild() {
            return appendChild;
        }

        public void S$appendChild(final ScriptFunction function) {
            this.appendChild = function;
        }

        Prototype() {
            ScriptUtils.initialize(this);
        }

        public String getClassName() {
            return "Node";
        }
    }

    public static final class ObjectConstructor extends ScriptObject {
        private ScriptFunction appendChild;
        public ScriptFunction G$appendChild() {
            return appendChild;
        }

        public void S$appendChild(final ScriptFunction function) {
            this.appendChild = function;
        }

        public ObjectConstructor() {
            ScriptUtils.initialize(this);
        }

        public Object getDefaultValue(final Class<?> typeHint) {
            return "[object Node]";
        }

        public String getClassName() {
            return "Node";
        }
    }
}
