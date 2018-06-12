package io.jenkins.plugins.hugo;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class HugoGitPublisherTest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Test
    @Ignore
    public void test() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();

        GitSCM scm = new GitSCM("https://gitee.com/arch2surenpi/hugo-plugin-test");
        project.setScm(scm);

        HugoBuilder hugoBuilder = new HugoBuilder();
        hugoBuilder.setHugoHome("/home/surenpi/go/bin/");
        hugoBuilder.setBaseUrl("https://gitee.com/arch2surenpi/linuxsuren.github.io");
        hugoBuilder.setDestination("public-gitee");
        project.getBuildersList().add(hugoBuilder);

        HugoGitPublisher publisher = new HugoGitPublisher("https://gitee.com/arch2surenpi/linuxsuren.github.io");
        publisher.setPublishDir("public-gitee");
        publisher.setPublishBranch("master");
        publisher.setCommitLog("hello");

        StandardUsernameCredentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, null,
                null, "", "");
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

        publisher.setCredentialsId(credentials.getId());
        project.getPublishersList().add(publisher);

        rule.buildAndAssertSuccess(project);
    }
}