/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider;
import org.hippoecm.repository.api.HippoNodeType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class GroupDataProvider extends SearchableDataProvider<Group> {

    private static final long serialVersionUID = 1L;

    private static final String QUERY_ALL_GROUP_LIST = "SELECT * " +
                " FROM " + HippoNodeType.NT_GROUP +
                " WHERE (hipposys:system <> 'true' OR hipposys:system IS NULL)";

    private static final String QUERY_GROUP_LIST_TEMPLATE = "SELECT * " +
                    " FROM " + HippoNodeType.NT_GROUP +
                    " WHERE (hipposys:system <> 'true' OR hipposys:system IS NULL) OR " +
                          " (  " +
                              " fn:name() = '{}' OR " +
                              " contains(hipposys:description, '{}' ) " +
                              " contains(hipposys:members, '{}') OR " +
                              " contains(hipposys:groups, '{}') " +
                           " ) ";

    public GroupDataProvider() {
        super(QUERY_ALL_GROUP_LIST, QUERY_GROUP_LIST_TEMPLATE, "/hippo:configuration/hippo:groups", HippoNodeType.NT_GROUP, HippoNodeType.NT_GROUPFOLDER);
        setSort("groupname", true);
    }

    @Override
    protected Group createBean(final Node node) throws RepositoryException {
        return new Group(node);
    }

    @Override
    public IModel<Group> model(final Group group) {
        return new DetachableGroup(group);
    }

    @Override
    public Iterator<Group> iterator(int first, int count) {
        List<Group> groupList = new ArrayList<Group>(getList());

        Collections.sort(groupList, new Comparator<Group>() {
            public int compare(Group group1, Group group2) {
                int direction = getSort().isAscending() ? 1 : -1;

                return direction * (group1.compareTo(group2));
            }
        });

        return groupList.subList(first, Math.min(first + count, groupList.size())).iterator();
    }
}
