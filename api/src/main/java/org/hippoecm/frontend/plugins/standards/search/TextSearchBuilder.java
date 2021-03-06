/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.search;

import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSearchBuilder implements IClusterable {

    static final Logger log = LoggerFactory.getLogger(TextSearchBuilder.class);

    public static final String TEXT_QUERY_NAME = "text";

    private static final String DEFAULT_IGNORED_CHARS = "&|!(){}[]^\"~*?:\\";
    private static final char MINUS_SIGN = '-';

    private String text;
    private String ignoredChars = DEFAULT_IGNORED_CHARS;
    private String[] scope;
    private String[] includePrimaryTypes;
    private String[] excludedPrimaryTypes = ArrayUtils.EMPTY_STRING_ARRAY;
    private boolean wildcardSearch;
    private int limit = -1;
    private int minimalLength = 3;
    private final String queryName;

    public TextSearchBuilder() {
        this(TEXT_QUERY_NAME);
    }

    public TextSearchBuilder(final String queryName) {
        this.queryName = queryName;
        scope = new String[]{"/"};
    }

    public void setScope(final String[] paths) {
        scope = paths;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * Sets the JCR primary types to search for.
     *
     * @param includePrimaryTypes {@link String}[] of primary types
     */
    public void setIncludePrimaryTypes(final String[] includePrimaryTypes) {
        this.includePrimaryTypes = includePrimaryTypes;
    }

    public void setExcludedPrimaryTypes(final String[] excludedPrimaryTypes) {
        this.excludedPrimaryTypes = excludedPrimaryTypes;
    }

    public void setWildcardSearch(final boolean wildcardSearch) {
        this.wildcardSearch = wildcardSearch;
    }

    public void setIgnoredChars(final String ignoredChars) {
        this.ignoredChars = DEFAULT_IGNORED_CHARS + ignoredChars;
    }

    public void setText(final String value) {
        text = value;
    }

    public TextSearchResultModel getResultModel() {
        final String value = text.trim();
        if ("".equals(value)) {
            return null;
        }

        final StringBuilder querySb = getQueryStringBuilder();
        if (querySb == null) {
            return null;
        }

        final String query = querySb.toString();
        final IModel<QueryResult> resultModel = new QueryResultModel(query, limit);
        return new TextSearchResultModel(value, new BrowserSearchResult(queryName, resultModel), scope);
    }

    /**
     * Makes the JCR Xpath query string
     *
     * @return StringBuilder that represents the JCR Xpath query
     */
    StringBuilder getQueryStringBuilder() {
        final String value = isoLatin1AccentReplacer(text.trim());

        boolean valid = false;
        final StringBuilder querySb = new StringBuilder();
        querySb.append(getIncludedPrimaryTypeFilter()).append('[');
        final StringBuilder scope = getScope();
        if (scope != null) {
            querySb.append(scope);
        }

        if (!StringUtils.isBlank(value)) {
            if (scope != null) {
                querySb.append(" and ");
            }
            final String whereClause = getWhereClause(value, false);
            if (wildcardSearch) {
                final String whereClauseWildCards = getWhereClause(value, true);
                if (!whereClauseWildCards.isEmpty()) {
                    valid = true;
                    if (!whereClause.isEmpty()) {
                        querySb.append("(");
                        querySb.append("jcr:contains(.,'").append(whereClause).append("')");
                        querySb.append(" or ");
                        querySb.append("jcr:contains(.,'").append(whereClauseWildCards).append("')");
                        querySb.append(")");
                    } else {
                        querySb.append("jcr:contains(.,'").append(whereClauseWildCards).append("')");
                    }
                } else if (!whereClause.isEmpty()) {
                    valid = true;
                    querySb.append("jcr:contains(.,'").append(whereClause).append("')");
                }
            } else if (!whereClause.isEmpty()) {
                valid = true;
                querySb.append("jcr:contains(.,'").append(whereClause).append("')");
            }
        }

        querySb.append(']').append(" order by @jcr:score descending");

        if (!valid) {
            log.debug("Cannot create a Xpath query for '{}'. Return null", value);
            return null;
        }
        log.debug("Xpath query = {} ", querySb.toString());
        return querySb;
    }

    private String getWhereClause(final String value, final boolean wildcardPostfix) {
        final StringBuilder whereClauseBuilder = new StringBuilder();
        boolean isOperatorToken;
        String peekedToken = null;
        for (final StringTokenizer st = new StringTokenizer(value, " "); st.hasMoreTokens() || peekedToken != null; ) {
            final String token;
            if (peekedToken != null) {
                token = peekedToken;
                peekedToken = null;
            } else {
                token = st.nextToken();
            }
            final StringBuilder tb = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                final char c = token.charAt(i);
                if (ignoredChars.indexOf(c) == -1) {
                    if (c == '\'') {
                        // According to JCR 1.0 (JSR-170), section 6.6.4.9, the apostrophe(') and quotation mark (")
                        // are escaped by the two adjacent marks, i.e. ('') and ("").
                        tb.append('\'');
                    }
                    if (c == MINUS_SIGN) {
                        // we do not allow minus sign followed by a space or ignored char
                        if (token.length() > i + 1) {
                            final char nextChar = token.charAt(i + 1);
                            if (nextChar == ' ' || ignoredChars.indexOf(nextChar) > -1) {
                                // not allowed position for -
                            } else {
                                tb.append(c);
                            }
                        }
                    } else {
                        tb.append(c);
                    }
                }
            }
            if (tb.length() == 0) {
                continue;
            }

            isOperatorToken = "OR".equals(token) || "AND".equals(token);

            if (wildcardPostfix && tb.length() < getMinimalLength() && !isOperatorToken) {
                // for wildcard postfixing we demand the term to be at least as long as #getMinimalLength()
                continue;
            }

            // add a space (this defaults to AND)
            if (isOperatorToken && !st.hasMoreTokens()) {
                // we do not allow an operator AND or OR as last token
                continue;
            }

            // now we could still have that the problem that the query ends with AND AND : thus we need to peek the next token 
            // if there are more to double check it is not a operator
            if (isOperatorToken && st.hasMoreTokens()) {
                peekedToken = st.nextToken();
                if ("OR".equals(peekedToken) || "AND".equals(peekedToken)) {
                    // the next token is an operator. Skip current one
                    continue;
                }
            }

            if (isOperatorToken && whereClauseBuilder.length() == 0) {
                // first term is not allowed to be an operator hence skip
                continue;
            }

            if (whereClauseBuilder.length() > 0) {
                whereClauseBuilder.append(" ");
            }

            whereClauseBuilder.append(tb);

            // we only append a wildcard IF and only IF
            // 1: WildcardSearch is set to true
            // 2: The term length is at least equal to minimal length: This is to avoid expensive kind of a* searches
            // 3: The term is not an operator token like AND or OR
            if (wildcardPostfix && tb.length() >= getMinimalLength() && !isOperatorToken) {
                whereClauseBuilder.append('*');
            }
        }
        return whereClauseBuilder.toString();
    }

    private StringBuilder getScope() {
        final Session session = UserSession.get().getJcrSession();

        final StringBuilder sb = new StringBuilder();
        boolean haveTypeRestriction = false;
        if (excludedPrimaryTypes.length > 0) {
            sb.append("not(");
            for (final String exclude : excludedPrimaryTypes) {
                if (haveTypeRestriction) {
                    sb.append(" or ");
                } else {
                    haveTypeRestriction = true;
                }
                sb.append("@jcr:primaryType='").append(exclude).append('\'');
            }
            sb.append(")");
        }
        boolean haveScope = false;
        for (final String path : scope) {
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException("Search path should be absolute: " + path);
            }
            try {
                final Node content = (Node) session.getItem(path);
                if (content.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                    final String uuid = content.getIdentifier();
                    if (haveScope) {
                        sb.append(" or ");
                    } else {
                        if (haveTypeRestriction) {
                            sb.append(" and ");
                        }
                        sb.append("(");
                        haveScope = true;
                    }
                    sb.append("hippo:paths = '").append(uuid).append('\'');
                } else {
                    log.info("Skipping non-referenceable node at path {}", path);
                }
            } catch (final PathNotFoundException e) {
                log.warn("Search path not found: " + path);
            } catch (final RepositoryException e) {
                throw new IllegalStateException(
                        "An error occurred while constructing the default search where-clause part", e);
            }
        }
        if (haveScope) {
            sb.append(')');
        }
        if (haveScope || haveTypeRestriction) {
            return sb;
        }
        return null;
    }

    /**
     * Translates the included primary type(s) to a filter for a JCR xpath query.
     *
     * @return xpath condition with configured document types or a clause that queries {@literal hippo:document} and all
     * its subtypes if no document type filter is configured
     */
    private StringBuilder getIncludedPrimaryTypeFilter() {
        final StringBuilder sb = new StringBuilder();
        if (includePrimaryTypes == null || includePrimaryTypes.length == 0) {
            sb.append("//element(*, " + HippoNodeType.NT_DOCUMENT + ")");
        } else {
            sb.append("//node()[");

            int i = 0;
            final int size = includePrimaryTypes.length;
            while (i < size) {
                if (i > 0) {
                    sb.append(" or ");
                }
                sb.append("@jcr:primaryType='").append(includePrimaryTypes[i]).append('\'');
                i++;
            }
            sb.append(']');
        }
        return sb;
    }

    public void setMinimalLength(final int minimalLength) {
        this.minimalLength = minimalLength;
    }

    public int getMinimalLength() {
        return minimalLength;
    }

    public static String isoLatin1AccentReplacer(final String input) {
        if (input == null) {
            return null;
        }

        final char[] inputChars = input.toCharArray();
        // Worst-case length required:
        final char[] output = new char[inputChars.length * 2];

        int outputPos = 0;

        int pos = 0;
        final int length = inputChars.length;

        for (int i = 0; i < length; i++, pos++) {
            final char c = inputChars[pos];

            // Quick test: if it's not in range then just keep
            // current character
            if (c < '\u00c0') {
                output[outputPos++] = c;
            } else {
                switch (c) {
                    case '\u00C0': // À
                    case '\u00C1': // Á
                    case '\u00C2': // Â
                    case '\u00C3': // Ã
                    case '\u00C4': // Ä
                    case '\u00C5': // Å
                        output[outputPos++] = 'A';
                        break;
                    case '\u00C6': // Æ
                        output[outputPos++] = 'A';
                        output[outputPos++] = 'E';
                        break;
                    case '\u00C7': // Ç
                        output[outputPos++] = 'C';
                        break;
                    case '\u00C8': // È
                    case '\u00C9': // É
                    case '\u00CA': // Ê
                    case '\u00CB': // Ë
                        output[outputPos++] = 'E';
                        break;
                    case '\u00CC': // Ì
                    case '\u00CD': // Í
                    case '\u00CE': // Î
                    case '\u00CF': // Ï
                        output[outputPos++] = 'I';
                        break;
                    case '\u00D0': // Ð
                        output[outputPos++] = 'D';
                        break;
                    case '\u00D1': // Ñ
                        output[outputPos++] = 'N';
                        break;
                    case '\u00D2': // Ò
                    case '\u00D3': // Ó
                    case '\u00D4': // Ô
                    case '\u00D5': // Õ
                    case '\u00D6': // Ö
                    case '\u00D8': // Ø
                        output[outputPos++] = 'O';
                        break;
                    case '\u0152': // Œ
                        output[outputPos++] = 'O';
                        output[outputPos++] = 'E';
                        break;
                    case '\u00DE': // Þ
                        output[outputPos++] = 'T';
                        output[outputPos++] = 'H';
                        break;
                    case '\u00D9': // Ù
                    case '\u00DA': // Ú
                    case '\u00DB': // Û
                    case '\u00DC': // Ü
                        output[outputPos++] = 'U';
                        break;
                    case '\u00DD': // Ý
                    case '\u0178': // Ÿ
                        output[outputPos++] = 'Y';
                        break;
                    case '\u00E0': // à
                    case '\u00E1': // á
                    case '\u00E2': // â
                    case '\u00E3': // ã
                    case '\u00E4': // ä
                    case '\u00E5': // å
                        output[outputPos++] = 'a';
                        break;
                    case '\u00E6': // æ
                        output[outputPos++] = 'a';
                        output[outputPos++] = 'e';
                        break;
                    case '\u00E7': // ç
                        output[outputPos++] = 'c';
                        break;
                    case '\u00E8': // è
                    case '\u00E9': // é
                    case '\u00EA': // ê
                    case '\u00EB': // ë
                        output[outputPos++] = 'e';
                        break;
                    case '\u00EC': // ì
                    case '\u00ED': // í
                    case '\u00EE': // î
                    case '\u00EF': // ï
                        output[outputPos++] = 'i';
                        break;
                    case '\u00F0': // ð
                        output[outputPos++] = 'd';
                        break;
                    case '\u00F1': // ñ
                        output[outputPos++] = 'n';
                        break;
                    case '\u00F2': // ò
                    case '\u00F3': // ó
                    case '\u00F4': // ô
                    case '\u00F5': // õ
                    case '\u00F6': // ö

                    case '\u00F8': // ø
                        output[outputPos++] = 'o';
                        break;
                    case '\u0153': // œ
                        output[outputPos++] = 'o';
                        output[outputPos++] = 'e';
                        break;
                    case '\u00DF': // ß
                        output[outputPos++] = 's';
                        output[outputPos++] = 's';
                        break;
                    case '\u00FE': // þ
                        output[outputPos++] = 't';
                        output[outputPos++] = 'h';
                        break;
                    case '\u00F9': // ù
                    case '\u00FA': // ú
                    case '\u00FB': // û
                    case '\u00FC': // ü
                        output[outputPos++] = 'u';
                        break;
                    case '\u00FD': // ý
                    case '\u00FF': // ÿ
                        output[outputPos++] = 'y';
                        break;
                    default:
                        output[outputPos++] = c;
                        break;
                }
            }
        }

        // now take only the populated chars from output
        final char[] outputChars = new char[outputPos];
        System.arraycopy(output, 0, outputChars, 0, outputPos);
        return new String(outputChars);
    }
}
