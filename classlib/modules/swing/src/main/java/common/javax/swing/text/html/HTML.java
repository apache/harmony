/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Alexey A. Ivanov
*/
package javax.swing.text.html;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyleContext;

import org.apache.harmony.x.swing.Utilities;

/**
 * Attributes and Tags of this class are defined by HTML 4.01 specification,
 * see <a href="http://www.w3.org/TR/html401/">HTML 4.01 Specification</a>.
 */
public class HTML {

    public static final class Attribute {
        public static final Attribute ACTION = new Attribute("action");
        public static final Attribute ALIGN = new Attribute("align");
        public static final Attribute ALINK = new Attribute("alink");
        public static final Attribute ALT = new Attribute("alt");
        public static final Attribute ARCHIVE = new Attribute("archive");
        public static final Attribute BACKGROUND = new Attribute("background");
        public static final Attribute BGCOLOR = new Attribute("bgcolor");
        public static final Attribute BORDER = new Attribute("border");
        public static final Attribute CELLPADDING =
                                            new Attribute("cellpadding");
        public static final Attribute CELLSPACING =
                                            new Attribute("cellspacing");
        public static final Attribute CHECKED = new Attribute("checked");
        public static final Attribute CLASS = new Attribute("class");
        public static final Attribute CLASSID = new Attribute("classid");
        public static final Attribute CLEAR = new Attribute("clear");
        public static final Attribute CODE = new Attribute("code");
        public static final Attribute CODEBASE = new Attribute("codebase");
        public static final Attribute CODETYPE = new Attribute("codetype");
        public static final Attribute COLOR = new Attribute("color");
        public static final Attribute COLS = new Attribute("cols");
        public static final Attribute COLSPAN = new Attribute("colspan");
        public static final Attribute COMMENT = new Attribute("comment");
        public static final Attribute COMPACT = new Attribute("compact");
        public static final Attribute CONTENT = new Attribute("content");
        public static final Attribute COORDS = new Attribute("coords");
        public static final Attribute DATA = new Attribute("data");
        public static final Attribute DECLARE = new Attribute("declare");
        public static final Attribute DIR = new Attribute("dir");
        public static final Attribute DUMMY = new Attribute("dummy");
        public static final Attribute ENCTYPE = new Attribute("enctype");
        public static final Attribute ENDTAG = new Attribute("endtag");
        public static final Attribute FACE = new Attribute("face");
        public static final Attribute FRAMEBORDER =
                                            new Attribute("frameborder");
        public static final Attribute HALIGN = new Attribute("halign");
        public static final Attribute HEIGHT = new Attribute("height");
        public static final Attribute HREF = new Attribute("href");
        public static final Attribute HSPACE = new Attribute("hspace");
        public static final Attribute HTTPEQUIV = new Attribute("http-equiv");
        public static final Attribute ID = new Attribute("id");
        public static final Attribute ISMAP = new Attribute("ismap");
        public static final Attribute LANG = new Attribute("lang");
        public static final Attribute LANGUAGE = new Attribute("language");
        public static final Attribute LINK = new Attribute("link");
        public static final Attribute LOWSRC = new Attribute("lowsrc");
        public static final Attribute MARGINHEIGHT =
                                            new Attribute("marginheight");
        public static final Attribute MARGINWIDTH =
                                            new Attribute("marginwidth");
        public static final Attribute MAXLENGTH = new Attribute("maxlength");
        public static final Attribute METHOD = new Attribute("method");
        public static final Attribute MULTIPLE = new Attribute("multiple");
        public static final Attribute N = new Attribute("n");
        public static final Attribute NAME = new Attribute("name");
        public static final Attribute NOHREF = new Attribute("nohref");
        public static final Attribute NORESIZE = new Attribute("noresize");
        public static final Attribute NOSHADE = new Attribute("noshade");
        public static final Attribute NOWRAP = new Attribute("nowrap");
        public static final Attribute PROMPT = new Attribute("prompt");
        public static final Attribute REL = new Attribute("rel");
        public static final Attribute REV = new Attribute("rev");
        public static final Attribute ROWS = new Attribute("rows");
        public static final Attribute ROWSPAN = new Attribute("rowspan");
        public static final Attribute SCROLLING = new Attribute("scrolling");
        public static final Attribute SELECTED = new Attribute("selected");
        public static final Attribute SHAPE = new Attribute("shape");
        public static final Attribute SHAPES = new Attribute("shapes");
        public static final Attribute SIZE = new Attribute("size");
        public static final Attribute SRC = new Attribute("src");
        public static final Attribute STANDBY = new Attribute("standby");
        public static final Attribute START = new Attribute("start");
        public static final Attribute STYLE = new Attribute("style");
        public static final Attribute TARGET = new Attribute("target");
        public static final Attribute TEXT = new Attribute("text");
        public static final Attribute TITLE = new Attribute("title");
        public static final Attribute TYPE = new Attribute("type");
        public static final Attribute USEMAP = new Attribute("usemap");
        public static final Attribute VALIGN = new Attribute("valign");
        public static final Attribute VALUE = new Attribute("value");
        public static final Attribute VALUETYPE = new Attribute("valuetype");
        public static final Attribute VERSION = new Attribute("version");
        public static final Attribute VLINK = new Attribute("vlink");
        public static final Attribute VSPACE = new Attribute("vspace");
        public static final Attribute WIDTH = new Attribute("width");

        static final Attribute ACCESSKEY = new Attribute("accesskey");
        static final Attribute DISABLED = new Attribute("disabled");
        static final Attribute LABEL = new Attribute("label");
        static final Attribute READONLY = new Attribute("readonly");

        static final String IMPLIED_NEW_LINE = "CR";

        private final String id;

        private Attribute(final String id) {
            this.id = id;
        }

        public String toString() {
            return id;
        }
    }

    public static class Tag {
        public static final Tag A = new Tag("a", false, false);
        public static final Tag ADDRESS = new Tag("address", false, false);
        public static final Tag APPLET = new Tag("applet", false, false);
        public static final Tag AREA = new Tag("area", false, false);
        public static final Tag B = new Tag("b", false, false);
        public static final Tag BASE = new Tag("base", false, false);
        public static final Tag BASEFONT = new Tag("basefont", false, false);
        public static final Tag BIG = new Tag("big", false, false);
        public static final Tag BLOCKQUOTE = new Tag("blockquote", true, true);
        public static final Tag BODY = new Tag("body", true, true);
        public static final Tag BR = new Tag("br", true, false);
        public static final Tag CAPTION = new Tag("caption", false, false);
        public static final Tag CENTER = new Tag("center", true, false);
        public static final Tag CITE = new Tag("cite", false, false);
        public static final Tag CODE = new Tag("code", false, false);
        public static final Tag COMMENT = new Tag("comment", false, false);
        public static final Tag CONTENT = new Tag("content", false, false);
        public static final Tag DD = new Tag("dd", true, true);
        public static final Tag DFN = new Tag("dfn", false, false);
        public static final Tag DIR = new Tag("dir", true, true);
        public static final Tag DIV = new Tag("div", true, true);
        public static final Tag DL = new Tag("dl", true, true);
        public static final Tag DT = new Tag("dt", true, true);
        public static final Tag EM = new Tag("em", false, false);
        public static final Tag FONT = new Tag("font", false, false);
        public static final Tag FORM = new Tag("form", true, false);
        public static final Tag FRAME = new Tag("frame", false, false);
        public static final Tag FRAMESET = new Tag("frameset", false, false);
        public static final Tag H1 = new Tag("h1", true, true);
        public static final Tag H2 = new Tag("h2", true, true);
        public static final Tag H3 = new Tag("h3", true, true);
        public static final Tag H4 = new Tag("h4", true, true);
        public static final Tag H5 = new Tag("h5", true, true);
        public static final Tag H6 = new Tag("h6", true, true);
        public static final Tag HEAD = new Tag("head", true, true);
        public static final Tag HR = new Tag("hr", true, false);
        public static final Tag HTML = new Tag("html", true, false);
        public static final Tag I = new Tag("i", false, false);
        public static final Tag IMG = new Tag("img", false, false);
        public static final Tag IMPLIED = new Tag("p-implied", false, false);
        public static final Tag INPUT = new Tag("input", false, false);
        public static final Tag ISINDEX = new Tag("isindex", true, false);
        public static final Tag KBD = new Tag("kbd", false, false);
        public static final Tag LI = new Tag("li", true, true);
        public static final Tag LINK = new Tag("link", false, false);
        public static final Tag MAP = new Tag("map", false, false);
        public static final Tag MENU = new Tag("menu", true, true);
        public static final Tag META = new Tag("meta", false, false);
        public static final Tag NOFRAMES = new Tag("noframes", true, true);
        public static final Tag OBJECT = new Tag("object", false, false);
        public static final Tag OL = new Tag("ol", true, true);
        public static final Tag OPTION = new Tag("option", false, false);
        public static final Tag P = new Tag("p", true, true);
        public static final Tag PARAM = new Tag("param", false, false);
        public static final Tag PRE = new Tag("pre", true, true);
        public static final Tag S = new Tag("s", false, false);
        public static final Tag SAMP = new Tag("samp", false, false);
        public static final Tag SCRIPT = new Tag("script", false, false);
        public static final Tag SELECT = new Tag("select", false, false);
        public static final Tag SMALL = new Tag("small", false, false);
        public static final Tag SPAN = new Tag("span", false, false);
        public static final Tag STRIKE = new Tag("strike", false, false);
        public static final Tag STRONG = new Tag("strong", false, false);
        public static final Tag STYLE = new Tag("style", false, false);
        public static final Tag SUB = new Tag("sub", false, false);
        public static final Tag SUP = new Tag("sup", false, false);
        public static final Tag TABLE = new Tag("table", false, true);
        public static final Tag TD = new Tag("td", true, true);
        public static final Tag TEXTAREA = new Tag("textarea", false, false);
        public static final Tag TH = new Tag("th", true, true);
        public static final Tag TITLE = new Tag("title", true, true);
        public static final Tag TR = new Tag("tr", false, true);
        public static final Tag TT = new Tag("tt", false, false);
        public static final Tag U = new Tag("u", false, false);
        public static final Tag UL = new Tag("ul", true, true);
        public static final Tag VAR = new Tag("var", false, false);

        static final Tag ABBR = new Tag("abbr", false, false);
        static final Tag ACRONYM = new Tag("acronym", false, false);
        static final Tag BDO = new Tag("bdo", false, false);
        static final Tag BUTTON = new Tag("button", false, false);
        static final Tag COL = new Tag("col", false, false);
        static final Tag COLGROUP = new Tag("colgroup", false, false);
        static final Tag DEL = new Tag("del", false, false);
        static final Tag FIELDSET = new Tag("fieldset", false, false);
        static final Tag IFRAME = new Tag("iframe", true, true);
        static final Tag INS = new Tag("ins", false, false);
        static final Tag LABEL = new Tag("label", false, false);
        static final Tag LEGEND = new Tag("legend", false, false);
        static final Tag NOSCRIPT = new Tag("noscript", true, true);
        static final Tag OPTGROUP = new Tag("optgroup", false, false);
        static final Tag Q = new Tag("q", false, false);
        static final Tag TBODY = new Tag("tbody", true, true);
        static final Tag TFOOT = new Tag("tfoot", true, true);
        static final Tag THEAD = new Tag("thead", true, true);

        String id;
        boolean causesBreak;
        boolean isBlock;

        public Tag() {
            this(null, false, false);
        }

        protected Tag(final String id) {
            this(id, false, false);
        }

        protected Tag(final String id, final boolean causesBreak,
                      final boolean isBlock) {
            this.id = id;
            this.causesBreak = causesBreak;
            this.isBlock = isBlock;
        }

        public boolean breaksFlow() {
            return causesBreak;
        }

        public boolean isBlock() {
            return isBlock;
        }

        public boolean isPreformatted() {
            return this == PRE || this == TEXTAREA;
        }

        public String toString() {
            return id;
        }
    }

    public static class UnknownTag extends Tag implements Serializable {
        public UnknownTag(final String id) {
            super(id);
        }

        public boolean equals(final Object obj) {
            return obj instanceof UnknownTag
                   && toString().equals(obj.toString());
        }

        public int hashCode() {
            return toString().hashCode();
        }

        private void writeObject(final ObjectOutputStream out)
            throws IOException {

            out.defaultWriteObject();
            out.writeObject(toString());
            out.writeBoolean(breaksFlow());
            out.writeBoolean(isBlock());
        }

        private void readObject(final ObjectInputStream in)
            throws IOException, ClassNotFoundException {

            in.defaultReadObject();
            id = (String)in.readObject();
            causesBreak = in.readBoolean();
            isBlock = in.readBoolean();
        }
    }

    public static final String NULL_ATTRIBUTE_VALUE = "#DEFAULT";

    private static final Map attrMap = new HashMap();
    private static final Map tagMap = new HashMap();

    private static final Attribute[] attrs = {
        Attribute.ACTION,
        Attribute.ALIGN,
        Attribute.ALINK,
        Attribute.ALT,
        Attribute.ARCHIVE,
        Attribute.BACKGROUND,
        Attribute.BGCOLOR,
        Attribute.BORDER,
        Attribute.CELLPADDING,
        Attribute.CELLSPACING,
        Attribute.CHECKED,
        Attribute.CLASS,
        Attribute.CLASSID,
        Attribute.CLEAR,
        Attribute.CODE,
        Attribute.CODEBASE,
        Attribute.CODETYPE,
        Attribute.COLOR,
        Attribute.COLS,
        Attribute.COLSPAN,
        Attribute.COMMENT,
        Attribute.COMPACT,
        Attribute.CONTENT,
        Attribute.COORDS,
        Attribute.DATA,
        Attribute.DECLARE,
        Attribute.DIR,
        Attribute.DUMMY,
        Attribute.ENCTYPE,
        Attribute.ENDTAG,
        Attribute.FACE,
        Attribute.FRAMEBORDER,
        Attribute.HALIGN,
        Attribute.HEIGHT,
        Attribute.HREF,
        Attribute.HSPACE,
        Attribute.HTTPEQUIV,
        Attribute.ID,
        Attribute.ISMAP,
        Attribute.LANG,
        Attribute.LANGUAGE,
        Attribute.LINK,
        Attribute.LOWSRC,
        Attribute.MARGINHEIGHT,
        Attribute.MARGINWIDTH,
        Attribute.MAXLENGTH,
        Attribute.METHOD,
        Attribute.MULTIPLE,
        Attribute.N,
        Attribute.NAME,
        Attribute.NOHREF,
        Attribute.NORESIZE,
        Attribute.NOSHADE,
        Attribute.NOWRAP,
        Attribute.PROMPT,
        Attribute.REL,
        Attribute.REV,
        Attribute.ROWS,
        Attribute.ROWSPAN,
        Attribute.SCROLLING,
        Attribute.SELECTED,
        Attribute.SHAPE,
        Attribute.SHAPES,
        Attribute.SIZE,
        Attribute.SRC,
        Attribute.STANDBY,
        Attribute.START,
        Attribute.STYLE,
        Attribute.TARGET,
        Attribute.TEXT,
        Attribute.TITLE,
        Attribute.TYPE,
        Attribute.USEMAP,
        Attribute.VALIGN,
        Attribute.VALUE,
        Attribute.VALUETYPE,
        Attribute.VERSION,
        Attribute.VLINK,
        Attribute.VSPACE,
        Attribute.WIDTH,

        // HTML 4.01-specific attributes
        Attribute.ACCESSKEY,
        Attribute.DISABLED,
        Attribute.LABEL,
        Attribute.READONLY
    };

    private static final Tag[] tags = {
        Tag.A,
        Tag.ADDRESS,
        Tag.APPLET,
        Tag.AREA,
        Tag.B,
        Tag.BASE,
        Tag.BASEFONT,
        Tag.BIG,
        Tag.BLOCKQUOTE,
        Tag.BODY,
        Tag.BR,
        Tag.CAPTION,
        Tag.CENTER,
        Tag.CITE,
        Tag.CODE,
        //Tag.COMMENT,
        //Tag.CONTENT,
        Tag.DD,
        Tag.DFN,
        Tag.DIR,
        Tag.DIV,
        Tag.DL,
        Tag.DT,
        Tag.EM,
        Tag.FONT,
        Tag.FORM,
        Tag.FRAME,
        Tag.FRAMESET,
        Tag.H1,
        Tag.H2,
        Tag.H3,
        Tag.H4,
        Tag.H5,
        Tag.H6,
        Tag.HEAD,
        Tag.HR,
        Tag.HTML,
        Tag.I,
        Tag.IMG,
        //Tag.IMPLIED,
        Tag.INPUT,
        Tag.ISINDEX,
        Tag.KBD,
        Tag.LI,
        Tag.LINK,
        Tag.MAP,
        Tag.MENU,
        Tag.META,
        Tag.NOFRAMES,
        Tag.OBJECT,
        Tag.OL,
        Tag.OPTION,
        Tag.P,
        Tag.PARAM,
        Tag.PRE,
        Tag.S,
        Tag.SAMP,
        Tag.SCRIPT,
        Tag.SELECT,
        Tag.SMALL,
        Tag.SPAN,
        Tag.STRIKE,
        Tag.STRONG,
        Tag.STYLE,
        Tag.SUB,
        Tag.SUP,
        Tag.TABLE,
        Tag.TD,
        Tag.TEXTAREA,
        Tag.TH,
        Tag.TITLE,
        Tag.TR,
        Tag.TT,
        Tag.U,
        Tag.UL,
        Tag.VAR,
        // HTML 4.01-specific tags
        Tag.ABBR,
        Tag.ACRONYM,
        Tag.BDO,
        Tag.BUTTON,
        Tag.COL,
        Tag.COLGROUP,
        Tag.DEL,
        Tag.FIELDSET,
        Tag.IFRAME,
        Tag.INS,
        Tag.LABEL,
        Tag.LEGEND,
        Tag.NOSCRIPT,
        Tag.OPTGROUP,
        Tag.Q,
        Tag.TBODY,
        Tag.TFOOT,
        Tag.THEAD
    };

    static {
        for (int i = 0; i < attrs.length; i++) {
            attrMap.put(attrs[i].toString(), attrs[i]);
            StyleContext.registerStaticAttributeKey(attrs[i]);
        }

        for (int i = 0; i < tags.length; i++) {
            tagMap.put(tags[i].toString(), tags[i]);
            StyleContext.registerStaticAttributeKey(tags[i]);
        }
    }

    public HTML() {
    }

    public static Tag[] getAllTags() {
        return tags;
    }

    public static Tag getTag(final String tagName) {
        return (Tag)tagMap.get(tagName);
    }

    public static Attribute[] getAllAttributeKeys() {
        return attrs;
    }

    public static Attribute getAttributeKey(final String attrName) {
        return (Attribute)attrMap.get(attrName);
    }

    public static int getIntegerAttributeValue(final AttributeSet attr,
                                               final Attribute key,
                                               final int def) {
        final Object value = attr.getAttribute(key);
        if (value != null) {
            try {
                return Integer.parseInt((String)value);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    static URL resolveURL(final URL url, final URL base) {
        if (url == null) {
            return null;
        }

        try {
            return ((base != null) ? new URL(base, url.toString()) : url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    static URL resolveURL(final String url, final URL base) {
        if (Utilities.isEmptyString(url)) {
            return null;
        }

        try {
            return ((base != null) ? new URL(base, url) : new URL(url));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    static URL resolveURL(final URL url, final String base) {
        if (url == null) {
            return null;
        }

        try {
            return ((base != null) ? new URL(new URL(base), url.toString()) : url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    static URL resolveURL(final String url, final String base) {
        if (Utilities.isEmptyString(url)) {
            return null;
        }

        try {
            return ((base != null) ? new URL(new URL(base), url) : new URL(url));
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
