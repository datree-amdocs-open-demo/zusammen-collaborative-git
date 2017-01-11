/*
 * Copyright © 2016 Amdocs Software Systems Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.amdocs.tsuzammen.plugin.collaborationstore.git.impl;

import org.amdocs.tsuzammen.datatypes.Id;
import org.amdocs.tsuzammen.datatypes.SessionContext;
import org.amdocs.tsuzammen.datatypes.item.Info;
import org.amdocs.tsuzammen.plugin.collaborationstore.git.dao.GitSourceControlDao;
import org.amdocs.tsuzammen.plugin.collaborationstore.git.util.TestUtil;
import org.amdocs.tsuzammen.sdk.types.ElementData;
import org.eclipse.jgit.api.Git;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class ItemCollaborationStoreTest {

  private final ItemCollaborationStore itemCollaborationStore = spy(new ItemCollaborationStore());
  @Mock
  private GitSourceControlDao gitSourceControlDaoMock;

  private static final Id ITEM_ID = new Id();

  @BeforeClass
  public void init() {

    MockitoAnnotations.initMocks(this);


    Mockito.doNothing().when(itemCollaborationStore).addFileContent(anyObject(),anyObject(),
        anyObject(),
        anyObject(),
        anyObject());

    when(itemCollaborationStore.getSourceControlDao(anyObject())).thenReturn
        (gitSourceControlDaoMock);
    when(gitSourceControlDaoMock.clone
        (anyObject(),anyObject(),anyObject(),anyObject())).thenReturn(null);

    when(gitSourceControlDaoMock.clone
        (anyObject(),anyObject(),anyObject())).thenReturn(null);
  }

  @Test
  public void testCreate() throws Exception {
    SessionContext context = TestUtil.createSessionContext();

    Info info = new Info();
    info.setName("test_name");
    itemCollaborationStore.create(context, ITEM_ID, info);

    verify(itemCollaborationStore.getSourceControlDao(context)).clone(context,
        "/git/test/public/BP",
        "/git/test/public\\" + ITEM_ID.toString(), "main");


    verify(itemCollaborationStore).addFileContent(context,null,
        "/git/test/private\\users\\COLLABORATION_TEST\\"+ITEM_ID.getValue().toString(),
        "info.json",
        info);
  }

  @Test
  public void testDelete() throws Exception {
      //todo - method not implemented
  }

}