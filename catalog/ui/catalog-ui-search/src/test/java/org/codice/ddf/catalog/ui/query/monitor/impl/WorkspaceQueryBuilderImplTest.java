/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.filter.FilterBuilder;
import java.util.Collections;
import org.codice.ddf.catalog.ui.query.monitor.api.FilterService;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceQueryBuilder;
import org.junit.Test;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;

public class WorkspaceQueryBuilderImplTest {

  @Test
  public void testCreateFilter() {

    String id = "1234";

    FilterBuilder filterBuilder = mock(FilterBuilder.class);
    FilterService filterService = mock(FilterService.class);

    Filter workspaceTagFilter = mock(Filter.class);

    when(filterService.buildWorkspaceTagFilter()).thenReturn(workspaceTagFilter);

    Filter metacardIdFilter = mock(Filter.class);

    when(filterService.buildMetacardIdFilter(id)).thenReturn(metacardIdFilter);

    Or orOfMetacardIdsFilter = mock(Or.class);

    when(filterBuilder.anyOf(Collections.singletonList(metacardIdFilter)))
        .thenReturn(orOfMetacardIdsFilter);

    And finalFilter = mock(And.class);

    when(filterBuilder.allOf(workspaceTagFilter, orOfMetacardIdsFilter)).thenReturn(finalFilter);

    WorkspaceQueryBuilder workspaceQueryBuilder =
        new WorkspaceQueryBuilderImpl(filterBuilder, filterService);

    Filter filter = workspaceQueryBuilder.createFilter(Collections.singleton(id));

    assertThat(filter, is(finalFilter));
  }
}