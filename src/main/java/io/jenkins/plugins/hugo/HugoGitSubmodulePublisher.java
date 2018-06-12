package io.jenkins.plugins.hugo;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author suren
 */
public class HugoGitSubmodulePublisher extends Recorder implements SimpleBuildStep {
    private String publishDir;
    private String publishBranch;
    private String credentialsId;

    private String authorName;
    private String authorEmail;
    private String committerName;
    private String committerEmail;

    private String commitLog;

    @DataBoundConstructor
    public HugoGitSubmodulePublisher() {}

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        // check whether has submodule
        Git git = new Git(listener, null);
        GitClient client = git.in(workspace).getClient();
        PrintStream logger = listener.getLogger();

        boolean hasGitModules = client.hasGitModules();
        if(!hasGitModules) {
            listener.getLogger().println("No git modules found.");
            return;
        }

        logger.println("Prepare to commit and push");

        FilePath publishPath = workspace.child(publishDir);

        client = git.in(publishPath).getClient();
        if(!client.hasGitRepo())
        {
            listener.error("Submodule has not init.");
            return;
        }

        String branch = publishBranch;
        logger.println("create new branch");

        client.checkout().branch(branch).deleteBranchIfExist(true).ref("HEAD").execute();

        client.rebase().setUpstream("origin/" + branch).execute();

        logger.println("prepare to execute hugo");
        copyArtifact(workspace, publishPath);

        logger.println("remote: " + publishPath.getRemote());
        logger.println("add everything.");

        String url = client.getRemoteUrl("origin");

        if(credentialsId != null)
        {
            StandardUsernameCredentials credential = getCredential(logger);
            if(credential != null)
            {
                client.setCredentials(credential);

                if(getAuthorName() != null)
                {
                    client.setAuthor(getAuthorName(), getAuthorEmail());
                }

                if(getCommitterName() != null)
                {
                    client.setCommitter(getCommitterName(), getCommitterEmail());
                }

                logger.println("already set credential : " + credential.getUsername());
            }
            else
            {
                logger.println("can not found credential");
            }
        }
        else
        {
            logger.println("No credential provide.");
        }

        logger.println("remote is " + url);

        client.add(".");
        client.commit(getCommitLog());

        try
        {

            client.push().to(new URIish(url)).ref(branch).force(true).execute();
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Copy artifact from a temp directory to real path, then clean
     * @param tempPath temp public path
     * @param publishPath real public path
     * @throws IOException
     * @throws InterruptedException
     */
    private void copyArtifact(FilePath tempPath, FilePath publishPath) throws IOException, InterruptedException {
        FilePath tempPublic = tempPath.child(HugoBuilder.TEMP_PUBLIC);
        tempPublic.copyRecursiveTo(publishPath);
        tempPublic.deleteRecursive();
    }

    private StandardUsernameCredentials getCredential(PrintStream logger) {
        List<StandardUsernameCredentials> allCredentials = CredentialsProvider.lookupCredentials
                (StandardUsernameCredentials.class, Jenkins.getActiveInstance(), ACL.SYSTEM, new ArrayList<>());

        Credentials credential = CredentialsMatchers.firstOrNull(
                allCredentials, CredentialsMatchers.withId(getCredentialsId()));

        if(credential != null)
        {
            return (StandardUsernameCredentials) credential;
        }
        else
        {
            logger.println("can not found credential");
        }

        return null;
    }

    public String getPublishDir()
    {
        return publishDir;
    }

    @DataBoundSetter
    public void setPublishDir(String publishDir)
    {
        this.publishDir = publishDir;
    }

    public String getPublishBranch()
    {
        return publishBranch;
    }

    @DataBoundSetter
    public void setPublishBranch(String publishBranch)
    {
        this.publishBranch = publishBranch;
    }

    public String getCredentialsId()
    {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId)
    {
        this.credentialsId = credentialsId;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    @DataBoundSetter
    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getAuthorEmail()
    {
        return authorEmail;
    }

    @DataBoundSetter
    public void setAuthorEmail(String authorEmail)
    {
        this.authorEmail = authorEmail;
    }

    public String getCommitterName()
    {
        return committerName;
    }

    @DataBoundSetter
    public void setCommitterName(String committerName)
    {
        this.committerName = committerName;
    }

    public String getCommitterEmail()
    {
        return committerEmail;
    }

    @DataBoundSetter
    public void setCommitterEmail(String committerEmail)
    {
        this.committerEmail = committerEmail;
    }

    public String getCommitLog()
    {
        return commitLog;
    }

    @DataBoundSetter
    public void setCommitLog(String commitLog)
    {
        this.commitLog = commitLog;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Extension
    @Symbol("hugoGitSubmodulePublsh")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>
    {
        public ListBoxModel doFillCredentialsIdItems() {
            FreeStyleProject project = new FreeStyleProject(Jenkins.getActiveInstance(),"fake-" + UUID.randomUUID().toString());

            return new StandardListBoxModel().includeEmptyValue()
                    .includeMatchingAs(ACL.SYSTEM, project,
                            StandardUsernameCredentials.class,
                            new ArrayList<>(),
                            CredentialsMatchers.withScopes(CredentialsScope.GLOBAL));
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType)
        {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName()
        {
            return Messages.hugo_publisher_git_submodule();
        }
    }
}