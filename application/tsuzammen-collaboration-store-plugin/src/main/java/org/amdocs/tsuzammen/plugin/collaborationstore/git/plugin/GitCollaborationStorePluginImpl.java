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

package org.amdocs.tsuzammen.plugin.collaborationstore.git.plugin;

import org.amdocs.tsuzammen.commons.configuration.impl.ConfigurationAccessor;
import org.amdocs.tsuzammen.datatypes.Id;
import org.amdocs.tsuzammen.datatypes.Namespace;
import org.amdocs.tsuzammen.datatypes.SessionContext;
import org.amdocs.tsuzammen.datatypes.collaboration.MergeResponse;
import org.amdocs.tsuzammen.datatypes.collaboration.PublishResult;
import org.amdocs.tsuzammen.datatypes.item.ElementContext;
import org.amdocs.tsuzammen.datatypes.item.Info;
import org.amdocs.tsuzammen.datatypes.item.ItemVersion;
import org.amdocs.tsuzammen.plugin.collaborationstore.git.GitSourceControlDao;
import org.amdocs.tsuzammen.plugin.collaborationstore.git.SourceControlDaoFactory;
import org.amdocs.tsuzammen.plugin.collaborationstore.git.utils.ElementDataUtil;
import org.amdocs.tsuzammen.plugin.collaborationstore.git.utils.PluginConstants;
import org.amdocs.tsuzammen.plugin.collaborationstore.git.utils.SourceControlUtil;
import org.amdocs.tsuzammen.sdk.CollaborationStore;
import org.amdocs.tsuzammen.sdk.types.ChangedElementData;
import org.amdocs.tsuzammen.sdk.types.ElementData;
import org.amdocs.tsuzammen.sdk.types.SyncResult;
import org.amdocs.tsuzammen.sdk.utils.SdkConstants;
import org.amdocs.tsuzammen.utils.fileutils.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.ObjectId;

import java.io.File;
import java.util.Collection;

import static org.amdocs.tsuzammen.plugin.collaborationstore.git.utils.PluginConstants.INFO_FILE_NAME;
import static org.amdocs.tsuzammen.plugin.collaborationstore.git.utils.PluginConstants.ITEM_VERSION_INFO_FILE_NAME;

public class GitCollaborationStorePluginImpl implements CollaborationStore {

  private static final String ADD_ITEM_VERSION_INFO_MESSAGE = "Add Item Version Info";
  private static final String SAVE_ITEM_VERSION_MESSAGE = "Save Item Version";
  private static final String DELETE_ITEM_VERSION_MESSAGE = "Delete Item Version";
  private static final String ADD_ITEM_INFO_MESSAGE = "Add Item Info";
  private static String PUBLIC_PATH = ConfigurationAccessor.getPluginProperty(SdkConstants
      .TSUZAMMEN_COLLABORATIVE_STORE, PluginConstants.PUBLIC_PATH);
  private static String PRIVATE_PATH = ConfigurationAccessor.getPluginProperty(
      SdkConstants.TSUZAMMEN_COLLABORATIVE_STORE, PluginConstants.PRIVATE_PATH);
  private static String PUBLIC_URL = ConfigurationAccessor.getPluginProperty(
      SdkConstants.TSUZAMMEN_COLLABORATIVE_STORE, PluginConstants.PUBLIC_URL);
  private static String BP_PATH = ConfigurationAccessor.getPluginProperty(
      SdkConstants.TSUZAMMEN_COLLABORATIVE_STORE, PluginConstants.BP_PATH);
  private static String TENANT = "{tenant}";

  @Override
  public void createItem(SessionContext context, Id itemId, Info info) {

    GitSourceControlDao dao = getSourceControlDao(context);
    String itemPublicPath = PUBLIC_PATH +
        File.separator + itemId;
    itemPublicPath = resolveTenantPath(context, itemPublicPath);
    String itemPrivatePath = PRIVATE_PATH +
        File.separator +
        "users" +
        File.separator +
        context.getUser().getUserName() +
        File.separator + itemId;
    itemPrivatePath = resolveTenantPath(context, itemPrivatePath);
    String itemPublicUrl = PUBLIC_URL + "/" + itemId;
    itemPublicUrl = resolveTenantPath(context, itemPublicUrl);
    String bluePrintPath = resolveTenantPath(context, BP_PATH); /*todo - add item type to the blue print*/

    String initialVersion = ConfigurationAccessor.getPluginProperty(SdkConstants
        .TSUZAMMEN_COLLABORATIVE_STORE, PluginConstants.MASTER_BRANCH);
    Git git = dao.clone(context, bluePrintPath, itemPublicPath, initialVersion);
    dao.clone(context, itemPublicUrl, itemPrivatePath, null);
    if (info != null) {
      addFileContent(context, git, itemPrivatePath, INFO_FILE_NAME, info);
      dao.commit(context, git, ADD_ITEM_INFO_MESSAGE);
    }

    dao.close(context, git);
  }

  @Override
  public void deleteItem(SessionContext context, Id itemId) {
    /*todo*/

  }

  @Override
  public void createItemVersion(SessionContext context, Id itemId, Id baseVersionId,
                                Id versionId,
                                Info versionInfo) {
    String baseBranchId = baseVersionId == null ?
        ConfigurationAccessor.getPluginProperty(SdkConstants.TSUZAMMEN_COLLABORATIVE_STORE,
            PluginConstants.MASTER_BRANCH) : baseVersionId.getValue().toString();
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        SourceControlUtil.getPrivateRepositoryPath(context, PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git = dao.openRepository(context, repositoryPath);
    createItemVersionInt(context, git, baseBranchId, versionId.getValue().toString(), versionInfo);
    boolean commitRequired = storeItemVersionInfo(context, git, itemId, versionInfo);
    if (commitRequired) {
      dao.commit(context, git, SAVE_ITEM_VERSION_MESSAGE);
    }
    dao.close(context, git);
  }

  @Override
  public void saveItemVersion(SessionContext context, Id itemId, Id versionId,
                              Info versionInfo) {
    String repositoryPath =
        SourceControlUtil.getPrivateRepositoryPath(context, PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    GitSourceControlDao dao = getSourceControlDao(context);
    Git git = dao.openRepository(context, repositoryPath);
    dao.checkoutBranch(context, git, versionId.getValue().toString());
    boolean commitRequired = storeItemVersionInfo(context, git, itemId, versionInfo);
    if (commitRequired) {
      dao.commit(context, git, SAVE_ITEM_VERSION_MESSAGE);
    }
    dao.close(context, git);
  }

  @Override
  public void createElement(SessionContext context, ElementContext elementContext,
                            Namespace namespace, ElementData elementData) {
    String repositoryPath = SourceControlUtil.getPrivateRepositoryPath(context, PRIVATE_PATH,
        elementContext.getItemId());
    repositoryPath = resolveTenantPath(context, repositoryPath);
    GitSourceControlDao dao = getSourceControlDao(context);
    Git git = dao.openRepository(context, repositoryPath);
    dao.checkoutBranch(context, git, elementContext.getVersionId().toString());

    String elementPath =
        namespace.getValue().replace(Namespace.NAMESPACE_DELIMITER, File.separator);
    String fullPath = repositoryPath + File.separator + elementPath;

    File elementPathFile = new File(fullPath);
    elementPathFile.mkdirs();

    updateElementData(context, git, fullPath, elementData);
    dao.commit(context, git, SAVE_ITEM_VERSION_MESSAGE);
    dao.close(context, git);
    //return new CollaborationNamespace(elementPath);
  }

  @Override
  public void saveElement(SessionContext context, ElementContext elementContext,
                          Namespace namespace, ElementData elementData) {
    GitSourceControlDao dao = getSourceControlDao(context);
    String elementPath = namespace.getValue();
    String repositoryPath = SourceControlUtil.getPrivateRepositoryPath(context,
        PRIVATE_PATH.replace(TENANT, context.getTenant()),
        elementContext.getItemId());
    String fullPath = repositoryPath + File.separator + elementPath;
    Git git = dao.openRepository(context, repositoryPath);
    dao.checkoutBranch(context, git, elementContext.getVersionId().toString());
    updateElementData(context, git, fullPath, elementData);
    dao.commit(context, git, SAVE_ITEM_VERSION_MESSAGE);
    dao.close(context, git);
  }

  @Override
  public void deleteElement(SessionContext context, ElementContext elementContext,
                            Namespace namespace, ElementData elementData) {
    GitSourceControlDao dao = getSourceControlDao(context);
    String elementPath = namespace.getValue();
    String repositoryPath =
        SourceControlUtil
            .getPrivateRepositoryPath(context, PRIVATE_PATH, elementContext.getItemId());
    repositoryPath = resolveTenantPath(context, repositoryPath);
    String fullPath = repositoryPath + File.separator + elementPath;
    Git git = dao.openRepository(context, repositoryPath);
    dao.checkoutBranch(context, git, elementContext.getVersionId().toString());
    dao.delete(context, git, fullPath);
    dao.commit(context, git, DELETE_ITEM_VERSION_MESSAGE);
    dao.close(context, git);
  }

  @Override
  public void deleteItemVersion(SessionContext sessionContext, Id itemId, Id versionId) {
    /*todo*/
  }

  @Override
  public PublishResult publishItemVersion(SessionContext context, Id itemId, Id versionId,
                                          String message) {
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        SourceControlUtil.getPrivateRepositoryPath(context, PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git = dao.openRepository(context, repositoryPath);
    String branchId = versionId.getValue().toString();
    dao.publish(context, git, branchId);
    dao.checkoutBranch(context, git, branchId);
//    dao.inComing(context,git,versionId.getValue());
    dao.close(context, git);
    return null;
  }

  @Override
  public SyncResult syncItemVersion(SessionContext context, Id itemId, Id versionId) {
    SyncResult result;
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        SourceControlUtil.getPrivateRepositoryPath(context, PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git;
    String branch = versionId.getValue().toString();
    ObjectId oldId = null;
    if (FileUtils.exists(repositoryPath)) {
      git = dao.openRepository(context, repositoryPath);
      dao.checkoutBranch(context, git, branch);
      oldId = dao.getHead(context, git);
      PullResult syncResult = dao.sync(context, git, branch);
      result = SourceControlUtil.handleSyncResponse(context, git, syncResult);
    } else {
      String publicPath = resolveTenantPath(context, PUBLIC_PATH);
      git = dao.clone(context,
          SourceControlUtil.getPublicRepositoryPath(context, publicPath, itemId),
          repositoryPath, branch);
      result = new SyncResult();
    }
    Collection<ChangedElementData> changedElementDataCollection = SourceControlUtil
        .handleSyncFileDiff
            (context, dao, git, itemId, versionId, oldId);

    result.setChangedElementDataCollection(changedElementDataCollection);
    dao.close(context, git);
    return result;
  }

  @Override
  public SyncResult mergeItemVersion(SessionContext context, Id itemId, Id versionId, Id
      sourceVersionId) {
    SyncResult result;
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        SourceControlUtil.getPrivateRepositoryPath(context, PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git;
    String branch = versionId.getValue().toString();
    String sourceBranch = sourceVersionId.getValue().toString();
    ObjectId oldId = null;
    if (FileUtils.exists(repositoryPath)) {
      git = dao.openRepository(context, repositoryPath);
      dao.checkoutBranch(context, git, branch);
      oldId = dao.getHead(context, git);
      MergeResult mergeResult =
          dao.merge(context, git, sourceBranch, MergeCommand.FastForwardMode.FF);
      result = SourceControlUtil.handleMergeResponse(context,git, mergeResult);
    } else {
      String publicPath = resolveTenantPath(context, PUBLIC_PATH);
      git = dao.clone(context,
          SourceControlUtil.getPublicRepositoryPath(context, publicPath, itemId),
          repositoryPath, branch);
      result = new SyncResult();
    }

    Collection<ChangedElementData> changedElementDataCollection = SourceControlUtil
        .handleSyncFileDiff
            (context, dao, git, itemId, versionId, oldId);

    result.setChangedElementDataCollection(changedElementDataCollection);

    dao.close(context, git);
    return result;
  }


  @Override
  public ItemVersion getItemVersion(SessionContext sessionContext, Id itemId, Id versionId,
                                    ItemVersion itemVersion) {
    return null;
  }

  @Override
  public ElementData getElement(SessionContext context, ElementContext elementContext,
                                Namespace namespace, Id elementId) {
    GitSourceControlDao dao = getSourceControlDao(context);
    Git git;

    String elementPath = namespace.getValue();
    String repositoryPath = SourceControlUtil.getPrivateRepositoryPath(context,
        PRIVATE_PATH.replace(TENANT, context.getTenant()),
        elementContext.getItemId());
    String fullPath = repositoryPath + File.separator + elementPath;

    git = dao.openRepository(context, repositoryPath);
    try {
      dao.checkoutBranch(context, git, elementContext.getVersionId().toString());
      ElementData elementData = uploadElementData(context, git, fullPath);
      return elementData;
    } finally {
      if (git != null) {
        dao.close(context, git);
      }
    }

  }


  protected void createItemVersionInt(SessionContext context, Git git, String baseBranch,
                                      String branch, Info versionInfo) {
    GitSourceControlDao dao = getSourceControlDao(context);
    dao.createBranch(context, git, baseBranch, branch);

  }


  protected GitSourceControlDao getSourceControlDao(SessionContext context) {
    return SourceControlDaoFactory.getInstance().createInterface(context);
  }


  private String resolveTenantPath(SessionContext context, String path) {
    String tenant = context.getTenant() != null ? context.getTenant() : "tsuzammen";
    return path.replace(TENANT, tenant);
  }

  private boolean storeItemVersionInfo(SessionContext context, Git git, Id itemId, Info info) {
    if (info == null) {
      return false;
    }

    addFileContent(
        context,
        git,
        SourceControlUtil.getPrivateRepositoryPath(context, PRIVATE_PATH, itemId) + File.separator,
        ITEM_VERSION_INFO_FILE_NAME,
        info);
    return true;
  }

  public void addFileContent(SessionContext context, Git git, String path,
                             String fileName, Object fileContent) {
    ElementDataUtil.init().addFileContent(context, git, path, fileName, fileContent);
  }


  protected void updateElementData(SessionContext context, Git git, String elementPath,
                                   ElementData elementData) {
    ElementDataUtil.init().updateElementData(context, git, elementPath, elementData);
  }

  protected ElementData uploadElementData(SessionContext context, Git git, String elementPath
  ) {
    return ElementDataUtil.init().uploadElementData(context, git, elementPath);

  }


}