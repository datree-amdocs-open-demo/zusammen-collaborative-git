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

package com.amdocs.tsuzammen;


import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.plugin.collaborationstore.git.main.GitCollaborationStorePluginImpl;

import java.util.ArrayList;
import java.util.List;

public class GitTest_toDelete {


  public static void main(String[] args) {

    SessionContext contextA = new SessionContext();
    contextA.setUser(new UserInfo("testA"));
    Id itemId = initItem(contextA);

    Id versionId = createItemVersion(contextA, itemId, null);
    publishItemVersion(contextA, itemId, versionId, "publish new version");
    SessionContext contextB = new SessionContext();
    contextB.setUser(new UserInfo("testB"));

    syncItem(contextB, itemId, versionId);


  }

  private static void publishItemVersion(SessionContext context, Id itemId, Id versionId,
                                         String message) {
    GitCollaborationStorePluginImpl gitCollaborationStorePlugin =
        new GitCollaborationStorePluginImpl();

    gitCollaborationStorePlugin.publishItemVersion(context, itemId, versionId, message);
    System.out.println("publish item version:" + itemId + ":" + versionId);
  }

  private static void syncItem(SessionContext context, Id itemId, Id versionId) {
    GitCollaborationStorePluginImpl gitCollaborationStorePlugin =
        new GitCollaborationStorePluginImpl();

    gitCollaborationStorePlugin.syncItemVersion(context, itemId, versionId);
    System.out.println("sync item version:" + itemId + ":" + versionId);
  }

  private static Id createItemVersion(SessionContext context, Id itemId, ItemVersionData info) {
    GitCollaborationStorePluginImpl gitCollaborationStorePlugin =
        new GitCollaborationStorePluginImpl();

    ItemVersionData versionInfo = createItemVersionInfo(info);

    Id versionId = new Id();
    gitCollaborationStorePlugin.createItemVersion(context, itemId, null, versionId,
        versionInfo);
    System.out.println("create item version:" + itemId + ":" + versionId);
    return versionId;
  }

  private static ItemVersionData createItemVersionInfo(ItemVersionData itemVersionData) {
    if (itemVersionData != null) {
      return itemVersionData;
    }
    itemVersionData = new ItemVersionData();

    Info versionInfo = new Info();
    itemVersionData.setInfo(versionInfo);
    versionInfo.addProperty("type", "testProp");
    List<String> listProp = new ArrayList<>();
    listProp.add("prop1");
    listProp.add("prop2");
    versionInfo.addProperty("listType", listProp);
    return itemVersionData;
  }

  private static Id initItem(SessionContext context) {
    GitCollaborationStorePluginImpl gitCollaborationStorePlugin =
        new GitCollaborationStorePluginImpl();

    Id itemId = new Id();
    String versionId = new String("main");
    gitCollaborationStorePlugin.createItem(context, itemId, null);
    //gitCollaborationStorePlugin.createItemVersion(context, itemId, null, versionId, null);
    //gitCollaborationStorePlugin.publishItemVersion(context, itemId, versionId, "");
    System.out.println("init item:" + itemId);
    return itemId;
  }
}
