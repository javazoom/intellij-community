/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package git4idea.push;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.ui.UIUtil;
import git4idea.GitBranch;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kirill Likhodedov
 */
public class GitPushDialog extends DialogWrapper {

  private JComponent myRootPanel;
  private Project myProject;
  private final Collection<GitRepository> myRepositories;
  private final GitPusher myPusher;
  private final GitLogMultiRepoMultiBranch myListPanel;

  public GitPushDialog(@NotNull Project project, @NotNull Collection<GitRepository> repositories) {
    super(project);
    myProject = project;
    myRepositories = repositories;
    myPusher = new GitPusher(myProject);

    myListPanel = new GitLogMultiRepoMultiBranch(myProject);

    init();
    setOKButtonText("Push");
    setTitle("Git Push");
  }

  @Override
  protected JComponent createCenterPanel() {
    myRootPanel = new JPanel(new BorderLayout());
    myRootPanel.add(createCommitListPanel(), BorderLayout.CENTER);
    myRootPanel.add(createManualRefspecPanel(), BorderLayout.SOUTH);
    return myRootPanel;
  }
  
  private JComponent createCommitListPanel() {
    JPanel commitListPanel = new JPanel(new BorderLayout());

    final JBLoadingPanel loadingPanel = new JBLoadingPanel(new BorderLayout(), this.getDisposable());
    loadingPanel.add(myListPanel, BorderLayout.CENTER);
    loadingPanel.startLoading();

    loadCommitsInBackground(myListPanel, loadingPanel);

    commitListPanel.add(loadingPanel, BorderLayout.CENTER);
    return commitListPanel;
  }

  private void loadCommitsInBackground(final GitLogMultiRepoMultiBranch myListPanel, final JBLoadingPanel loadingPanel) {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      public void run() {
        Map<GitRepository, Collection<GitBranch>> repositoriesToBranches = prepareRepositoriesAndBranchesToPush();
        final GitCommitsByRepoAndBranch gitCommits = myPusher.collectCommitsToPush(repositoriesToBranches);
        UIUtil.invokeLaterIfNeeded(new Runnable() {
          @Override
          public void run() {
            myListPanel.setCommits(gitCommits);
            loadingPanel.stopLoading();
          }
        });
      }
    });
  }

  private Map<GitRepository, Collection<GitBranch>> prepareRepositoriesAndBranchesToPush() {
    Map<GitRepository, Collection<GitBranch>> res = new HashMap<GitRepository, Collection<GitBranch>>();
    for (GitRepository repository : myRepositories) {
      res.put(repository, Collections.singleton(repository.getCurrentBranch()));
    }
    return res;
  }

  private JComponent createManualRefspecPanel() {
    // TODO
    //GitPushRefspecPanel refspecPanel = new GitPushRefspecPanel(myRepositories);
    //refspecPanel.addChangeListener(new MyRefspecChanged(refspecPanel));
    //
    //JBLabel commentLabel = new JBLabel("This command will be applied as is for all selected repositories", UIUtil.ComponentStyle.SMALL);
    //
    //JComponent detailsPanel = new JPanel(new BorderLayout());
    //detailsPanel.add(refspecPanel, BorderLayout.WEST);
    //detailsPanel.add(commentLabel, BorderLayout.SOUTH);
    //
    //JPanel hiddenPanel = new ShowHidePanel("Manually specify refspec", detailsPanel);
    //return hiddenPanel;
    return new JPanel();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myListPanel.getPreferredFocusComponent();
  }

  @Override
  protected String getDimensionServiceKey() {
    return GitPushDialog.class.getName();
  }

}
