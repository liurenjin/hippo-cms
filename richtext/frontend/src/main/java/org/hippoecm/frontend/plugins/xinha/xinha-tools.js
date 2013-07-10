/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*global DOMParser */
(function(XinhaTools) {

    "use strict";

    if (XinhaTools.log === undefined) {
        XinhaTools.log = function() {
        };
    }

    /**
     * Return a new function and ensure the provided function is always executed within the provided context
     * with the arguments passed to the wrapping function.
     */
    XinhaTools.proxy = function(func, context) {
        return function() {
            func.apply(context, arguments);
        };
    };

    /**
     * Return the instance of a plugin or null of not found.
     */
    XinhaTools.getPlugin = function(editor, id) {
        return XinhaTools.nullOrValue(editor, ['plugins', id, 'instance']);
    };

    /**
     * Return the object referenced by the path array or null if not found.
     */
    XinhaTools.nullOrValue = function(ref, path) {
        var i;

        if (ref === null) {
            return null;
        }
        for (i = 0; i < path.length; i++) {
            if (XinhaTools.isValue(ref[path[i]])) {
                ref = ref[path[i]];
            } else {
                return null;
            }
        }
        return ref;
    };

    /**
     * Check whether the ref value is not undefined and not null
     */
    XinhaTools.isValue = function(ref) {
        return ref !== undefined && ref !== null;
    };

    /**
     * Return first text node in provided node
     */
    XinhaTools.findFirstTextNode = function(node) {
        var i, child, value;

        for (i=0; i<node.childNodes.length; i++) {
            child = node.childNodes[i];
            if (child.nodeType === 3) {
                return child;
            }
            if (child.nodeType === 1) {
                value = XinhaTools.findFirstTextNode(child);
                if (value !== child) {
                    return value;
                }
            }
        }
        return node;
    };

    /**
     * Check if browser is at least Ie9 :
     * NOTE not checking document.documentMode
     * 10 = window.atob
     * 9 = document.addEventListener
     * 8 = document.querySelector
     * 7 = window.XMLHttpRequest
     * 6 = document.compatMode
     */
    XinhaTools.isIe9 = function () {
        // document.documentMode &&  document.documentMode > 8;
        return document.all && document.addEventListener;
    };


    /**
     * Create a collection for easier lookup. e.g.:
     * <code>if(x in collection(1,2,3,4){doSomething()};</code>
     * <code>if(x in collection([1,2,3,4]){doSomething()};</code>
     * @param optional optional parameter  (type of array)
     * @returns {{}}
     */
    XinhaTools.collection = function (optional) {
        var i, obj = {}, arr = arguments;
        if (optional instanceof Array) {
            arr = optional;
        }
        for (i = 0; i < arr.length; i++) {
            obj[arr[i]] = true;
        }
        return obj;
    };


    /**
     * given two arrays, get same values
     * @param array1
     * @param array2
     * @returns {Array}
     */
    XinhaTools.intersect = function (array1, array2) {
        var result = [],
            c = XinhaTools.collection(array2),
            i;
        for (i = 0; i < array1.length; i++) {
            if (c.indexOf(array1[i]) >= 0) {
                result.push(array1[i]);
            }
        }
        return result;
    };

    /**
     *  given two arrays, get elements that are in second one but not in first one
     * @param array1
     * @param array2
     * @returns {Array}
     */
    XinhaTools.complement = function (array1, array2) {
        var result = [],
            c = XinhaTools.collection(array2),
            i;
        for (i = 0; i < array1.length; i++) {
            if (c.indexOf(array1[i]) < 0) {
                result.push(array1[i]);
            }
        }
        return result;
    };

    XinhaTools.isEmpty = function (str) {
        return str === undefined || str === null || str.trim().length === 0;
    };

    XinhaTools.removeAttribute = function (el, name) {
        el.removeAttribute(name);
    };

    XinhaTools.setAttribute = function (el, name, value) {
        el.setAttribute(name, value);
    };


    /**
     * Unwraps given element (node)
     * @param editor editor instance
     * @param el  element to unwrap
     */
    XinhaTools.unwrap = function (editor, el) {
        var parent, html, range, fragment, Range = document.Range;

        if (el === null || el.parentNode === null) {
            return;
        }

        parent = el.parentNode;
        html = el.innerHTML;
        if (document.createRange) {
            range = document.createRange();
            range.selectNodeContents(el);
        }
        // IE 9
        if ((Range !== undefined) && !Range.prototype.createContextualFragment) {
            Range.prototype.createContextualFragment = function (html) {
                var frag = document.createDocumentFragment(),
                    div = document.createElement('div');
                frag.appendChild(div);
                div.outerHTML = html;
                return frag;
            };
        }
        if (document.createRange) {
            fragment = range.createContextualFragment(html);
            parent.replaceChild(fragment, el);
        } else {
            // IE 8
            el.outerHTML = html;
        }
    };


    /**
     * Wrap node into <code>wrapperName</code> and apply supplied classes
     * @param el node
     * @param wrapperName tag name
     * @param classes classes string to apply
     * @param skipTextNodes skip wrapping if text node
     */
    XinhaTools.wrap = function (el, wrapperName, classes, skipTextNodes) {
        var nodeType, parent, wrapper, classesString;

        if (el === null || el.parentNode === null) {
            XinhaTools.log("Empty element, skipping");
            return;
        }
        nodeType = el.nodeType;
        if (skipTextNodes && nodeType === 3) {
            XinhaTools.log("skipping text node", el);
            return;
        }
        if(nodeType === 3 && el.nodeValue.trim().length === 0){
            XinhaTools.log("skipping *empty* text node", el);
            return;
        }
        parent = el.parentNode;
        wrapper = document.createElement(wrapperName);
        parent.insertBefore(wrapper, el);
        parent.removeChild(el);
        wrapper.appendChild(el);
        classesString = classes;
        if (classes instanceof Array) {
            classesString = classes.join(' ');
        }
        wrapper.setAttribute("class", classesString);
    };

    XinhaTools.wrapKid = function (el, wrapperName, classes, skipText) {
        var i, kids;

        if (el === null || el.parentNode === null) {
            XinhaTools.log("Empty element, skipping");
            return;
        }
        if (el.nodeType === 3) {
            XinhaTools.wrap(el, wrapperName, classes, false);
            return;
        }
        kids = el.childNodes;
        if (kids !== null) {
            for (i = 0; i < kids.length; i++) {
                XinhaTools.wrap(kids[i], wrapperName, classes, skipText);
            }
        }
    };

    /**
     * Walk through given node tree
     * @param node
     * @param callback function callback
     */
    XinhaTools.walk = function (node, callback) {
        var stopTraversing, tmp, depth = 0;
        do {
            if (!stopTraversing) {
                stopTraversing = callback.call(node, depth) === false;
            }
            if (!stopTraversing && (tmp = node.firstChild) !== undefined) {
                depth++;
            } else if ((tmp = node.nextSibling) !== undefined) {
                stopTraversing = false;
            } else {
                tmp = node.parentNode;
                depth--;
                stopTraversing = true;
            }
            node = tmp;
        } while (depth > 0);
    };


    XinhaTools.findElements = function (node, name) {
        var el = [];
        name = name.toUpperCase();
        XinhaTools.walk(node, function () {
            if (this.nodeType === 1 && this.nodeName.toUpperCase() === name) {
                el.push(this);
            }
            return true;
        });
        return el;
    };

    XinhaTools.containsSameNodes = function (node, name) {
        name = name.toUpperCase();
        XinhaTools.walk(node, function () {
            return !(this.nodeType === 1 && this.nodeName.toUpperCase() !== name);

        });
        return true;
    };

    XinhaTools.findElement = function (node, name) {
        var el = null;
        name = name.toUpperCase();
        XinhaTools.walk(node, function () {
            if (this.nodeType === 1 && this.nodeName.toUpperCase() === name) {
                el = this;
                return false;
            }
            return true;
        });
        return el;
    };


    XinhaTools.wrapKidsOfNodes = function (node, tagCollection) {
        XinhaTools.walk(node, function () {
            return !(this.nodeType === 1 && tagCollection.indexOf(this.nodeName.toUpperCase()) >= 0);
        });
        return true;
    };

}(window.XinhaTools = window.XinhaTools || {}));

// https://developer.mozilla.org/en-US/docs/Web/API/DOMParser?redirectlocale=en-US&redirectslug=DOM%2FDOMParser
(function (DOMParser) {
    if(!DOMParser){
        DOMParser = {};
    }
    var DOMParser_proto = DOMParser.prototype, real_parseFromString = DOMParser_proto.parseFromString;

    // Firefox/Opera/IE throw errors on unsupported types
    try {
        // WebKit returns null on unsupported types
        if ((new DOMParser()).parseFromString("", "text/html")) {
            // text/html parsing is natively supported
            return;
        }
    } catch (ex) {
    }

    DOMParser_proto.parseFromString = function (markup, type) {
        if (/^\s*text\/html\s*(?:;|$)/i.test(type)) {
            var doc = document.implementation.createHTMLDocument("");
            if (markup.toLowerCase().indexOf('<!doctype') > -1) {
                doc.documentElement.innerHTML = markup;
            }
            else {
                doc.body.innerHTML = markup;
            }
            return doc;
        } else {
            return real_parseFromString.apply(this, arguments);
        }
    };
}(DOMParser));
