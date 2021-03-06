/*
 * Copyright © 2016-2017 European Support Limited
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

package com.amdocs.zusammen.plugin.collaborationstore.impl;

import com.amdocs.zusammen.plugin.collaborationstore.dao.api.SourceControlDao;
import com.amdocs.zusammen.plugin.collaborationstore.dao.api.SourceControlDaoFactory;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.plugin.collaborationstore.git.util.TestUtil;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElementCollaborationStoreTest {
  private static final String PRIVATE_PATH = "/git/test/private" + File.separator
      + "users" + File.separator + "COLLABORATION_TEST" + File.separator;

  @Spy
  private ElementCollaborationStore elementCollaborationStore = new ElementCollaborationStore(
      SourceControlDaoFactory.getInstance());
  @Mock
  private SourceControlDao sourceControlDaoMock;

  private static final Id ITEM_ID = new Id();
  private static final Id VERSION_ID = new Id();
  private static final Id ELEMENT_ID = new Id();
  private static final String NAME_SPACE = (new Id()).toString() + File.separator + (new Id());
  private static final SessionContext context = TestUtil.createSessionContext();


  @BeforeMethod
  public void init() {
    MockitoAnnotations.initMocks(this);
    Mockito.doReturn(true).when(elementCollaborationStore).addFileContent(anyObject(), anyObject(),

        anyObject(),
        anyObject(),
        anyObject());
    Collection<String> files = new ArrayList<>();
    files.add("info.json");
    Mockito.doReturn(files).when(elementCollaborationStore)
        .updateCollaborationElement(anyObject(), anyObject(), anyObject(), anyObject(),
            anyObject());

    when(elementCollaborationStore.getSourceControlDao(anyObject())).thenReturn
        (sourceControlDaoMock);
    when(sourceControlDaoMock.cloneRepository
        (anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null);
    when(sourceControlDaoMock.initRepository(anyObject(), anyObject())).thenReturn(null);

    /*when(sourceControlDaoMock.getHead
        (anyObject(), anyObject())).thenReturn(null);
*/
    doNothing().when(sourceControlDaoMock).store
        (anyObject(), anyObject(), anyObject());

    when(sourceControlDaoMock.cloneRepository(anyObject(), anyObject(), anyObject(), anyObject()))
        .thenReturn(null);
  }


  @Test
  public void testCreate() throws Exception {

    Namespace namespace = new Namespace();
    namespace.setValue(NAME_SPACE);
    CollaborationElement element =
        new CollaborationElement(ITEM_ID, VERSION_ID, namespace, ELEMENT_ID);
    elementCollaborationStore.create(context, element);

    verify(sourceControlDaoMock).initRepository(context,
        ITEM_ID);

    verify(elementCollaborationStore)
        .updateCollaborationElement(context, PRIVATE_PATH + ITEM_ID.toString(),
            NAME_SPACE + File.separator + ELEMENT_ID, element, Action.CREATE);

  }

  @Test
  public void testSave() throws Exception {
    Namespace namespace = new Namespace();
    namespace.setValue(NAME_SPACE);

    CollaborationElement element =
        new CollaborationElement(ITEM_ID, VERSION_ID, namespace, ELEMENT_ID);


    elementCollaborationStore.update(context, element);

    verify(sourceControlDaoMock).initRepository(context,
        ITEM_ID);

    verify(elementCollaborationStore)
        .updateCollaborationElement(context, PRIVATE_PATH + ITEM_ID.toString(),
            NAME_SPACE + File.separator + ELEMENT_ID,
            element, Action.UPDATE);
  }

  @Test
  public void testDelete() throws Exception {

    Namespace namespace = new Namespace();
    namespace.setValue(NAME_SPACE);
    CollaborationElement element =
        new CollaborationElement(ITEM_ID, VERSION_ID, namespace, ELEMENT_ID);
    elementCollaborationStore.delete(context, element);

    verify(sourceControlDaoMock).initRepository(context,
        ITEM_ID);

    verify(sourceControlDaoMock).delete(context, null,
         NAME_SPACE + File.separator +
            ELEMENT_ID);
  }

  @Test
  public void testGet() throws Exception {

  }
}