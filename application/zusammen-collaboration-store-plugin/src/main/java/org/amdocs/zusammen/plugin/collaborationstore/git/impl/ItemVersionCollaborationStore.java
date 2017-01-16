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

package org.amdocs.zusammen.plugin.collaborationstore.git.impl;

import org.amdocs.zusammen.commons.configuration.impl.ConfigurationAccessor;
import org.amdocs.zusammen.datatypes.Id;
import org.amdocs.zusammen.datatypes.SessionContext;
import org.amdocs.zusammen.datatypes.item.Info;
import org.amdocs.zusammen.plugin.collaborationstore.git.dao.GitSourceControlDao;
import org.amdocs.zusammen.plugin.collaborationstore.git.utils.PluginConstants;
import org.amdocs.zusammen.sdk.types.ChangedElementData;
import org.amdocs.zusammen.sdk.types.ElementsMergeResult;
import org.amdocs.zusammen.sdk.types.ElementsPublishResult;
import org.amdocs.zusammen.sdk.utils.SdkConstants;
import org.amdocs.zusammen.utils.fileutils.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.PushResult;

import java.io.File;
import java.util.Collection;

import static org.amdocs.zusammen.plugin.collaborationstore.git.utils.PluginConstants.ITEM_VERSION_INFO_FILE_NAME;

public class ItemVersionCollaborationStore extends CollaborationStore {
  public void create(SessionContext context, Id itemId, Id baseVersionId, Id versionId,
                     Info versionInfo) {
    String baseBranchId = baseVersionId == null ?
        ConfigurationAccessor.getPluginProperty(SdkConstants.ZUSAMMEN_COLLABORATIVE_STORE,
            PluginConstants.MASTER_BRANCH_PROP) : baseVersionId.toString();
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        sourceControlUtil.getPrivateRepositoryPath(context, PluginConstants.PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git = dao.openRepository(context, repositoryPath);
    createInt(context, git, baseBranchId, versionId.toString(), versionInfo);
    boolean commitRequired = storeItemVersionInfo(context, git, itemId, versionInfo);
    if (commitRequired) {
      dao.commit(context, git, PluginConstants.SAVE_ITEM_VERSION_MESSAGE);
    }
    dao.close(context, git);
  }


  protected void createInt(SessionContext context, Git git, String baseBranch,
                           String branch, Info versionInfo) {
    GitSourceControlDao dao = getSourceControlDao(context);
    dao.createBranch(context, git, baseBranch, branch);

  }

  protected boolean storeItemVersionInfo(SessionContext context, Git git, Id itemId, Info info) {
    if (info == null) {
      return false;
    }

    addFileContent(
        context,
        git,
        sourceControlUtil.getPrivateRepositoryPath(context, PluginConstants.PRIVATE_PATH, itemId) +
            File.separator,
        ITEM_VERSION_INFO_FILE_NAME,
        info);
    return true;
  }


  public void save(SessionContext context, Id itemId, Id versionId, Info versionInfo) {
    String repositoryPath =
        sourceControlUtil.getPrivateRepositoryPath(context, PluginConstants.PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    GitSourceControlDao dao = getSourceControlDao(context);
    Git git = dao.openRepository(context, repositoryPath);
    dao.checkoutBranch(context, git, versionId.toString());
    boolean commitRequired = storeItemVersionInfo(context, git, itemId, versionInfo);
    if (commitRequired) {
      dao.commit(context, git, PluginConstants.SAVE_ITEM_VERSION_MESSAGE);
    }
    dao.close(context, git);
  }


  public void delete(SessionContext sessionContext, Id itemId, Id versionId) {
    /*todo*/
  }


  public ElementsPublishResult publish(SessionContext context, Id itemId, Id versionId,
                                       String message) {
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        sourceControlUtil.getPrivateRepositoryPath(context, PluginConstants.PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git = dao.openRepository(context, repositoryPath);
    String branchId = versionId.toString();
    ObjectId oldId = null;
    dao.getRemoteHead(context, git);
    Collection<PushResult> pushResult = dao.publish(context, git, branchId);
    dao.checkoutBranch(context, git, branchId);
//    dao.inComing(context,git,versionId.getValue());
    dao.close(context, git);
    Collection<ChangedElementData> changedElements = sourceControlUtil
        .handlePublishResponse(context, dao, git, pushResult);
    ElementsPublishResult publishResult = new ElementsPublishResult();
    publishResult.setChangedElements(changedElements);
    return publishResult;
  }


  public ElementsMergeResult sync(SessionContext context, Id itemId, Id versionId) {
    ElementsMergeResult result;
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        sourceControlUtil.getPrivateRepositoryPath(context, PluginConstants.PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git;
    String branch = versionId.toString();
    ObjectId oldId = null;
    if (FileUtils.exists(repositoryPath)) {
      git = dao.openRepository(context, repositoryPath);
      dao.checkoutBranch(context, git, branch);
      oldId = dao.getHead(context, git);
      PullResult syncResult = dao.sync(context, git, branch);
      result = sourceControlUtil.handleSyncResponse(context, git, syncResult);
    } else {
      String publicPath = resolveTenantPath(context, PluginConstants.PUBLIC_PATH);
      git = dao.clone(context,
          sourceControlUtil.getPublicRepositoryPath(context, publicPath, itemId),
          repositoryPath, branch);
      result = new ElementsMergeResult();
    }
    Collection<ChangedElementData> changedElementDataCollection =
        sourceControlUtil.handleSyncFileDiff(context, dao, git, oldId, null);

    result.setChangedElements(changedElementDataCollection);
    dao.close(context, git);
    return result;
  }


  public ElementsMergeResult merge(SessionContext context, Id itemId, Id
      versionId, Id sourceVersionId) {
    ElementsMergeResult result;
    GitSourceControlDao dao = getSourceControlDao(context);
    String repositoryPath =
        sourceControlUtil.getPrivateRepositoryPath(context, PluginConstants.PRIVATE_PATH, itemId);
    repositoryPath = resolveTenantPath(context, repositoryPath);
    Git git;
    String branch = versionId.toString();
    String sourceBranch = sourceVersionId.toString();
    ObjectId oldId = null;
    if (FileUtils.exists(repositoryPath)) {
      git = dao.openRepository(context, repositoryPath);
      dao.checkoutBranch(context, git, branch);
      oldId = dao.getHead(context, git);
      MergeResult mergeResult =
          dao.merge(context, git, sourceBranch, MergeCommand.FastForwardMode.FF);
      result = sourceControlUtil.handleMergeResponse(context, git, mergeResult);
    } else {
      String publicPath = resolveTenantPath(context, PluginConstants.PUBLIC_PATH);
      git = dao.clone(context,
          sourceControlUtil.getPublicRepositoryPath(context, publicPath, itemId),
          repositoryPath, branch);
      result = new ElementsMergeResult();
    }

    Collection<ChangedElementData> changedElements =
        sourceControlUtil.handleSyncFileDiff(context, dao, git, oldId, null);

    result.setChangedElements(changedElements);

    dao.close(context, git);
    return result;
  }


}