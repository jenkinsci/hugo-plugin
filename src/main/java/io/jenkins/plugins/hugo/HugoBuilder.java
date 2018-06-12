package io.jenkins.plugins.hugo;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author suren
 */
public class HugoBuilder extends Builder implements SimpleBuildStep
{
    public static final String TEMP_PUBLIC = ".public";

    private String hugoHome;
    private String baseUrl;
    private String destination;
    private boolean verbose;

    @DataBoundConstructor
    public HugoBuilder() {}

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException
    {
        hugoBuild(run, launcher, listener, workspace);
    }

    /**
     * Build the Hugo site through hugo cmd line
     * @param run Job Run
     * @param launcher Launcher
     * @param listener Job Listener
     * @param workspace Job Workspace
     * @throws IOException In case of io error
     * @throws InterruptedException In case of job running be interrupt
     */
    private void hugoBuild(@Nonnull Run<?, ?> run, Launcher launcher, TaskListener listener, @Nonnull FilePath workspace)
            throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        EnvVars env = run.getEnvironment(listener);

        String hugoCmd = buildCmd();

        int exitCode = launcher.launch().pwd(workspace)
                .cmdAsSingleString(hugoCmd).envs(env).stdout(logger).stderr(logger).start().join();
        if(exitCode != 0) {
            listener.fatalError("Hugo build error, exit code: " + exitCode);
        }
    }

    private String buildCmd() {
        String hugoCmd;
        if(getHugoHome() == null || "".equals(getHugoHome().trim())) {
            hugoCmd = "hugo";
        } else {
            hugoCmd = getHugoHome() + "hugo";
        }

        if(destination == null || "".equals(destination.trim())) {
            hugoCmd += " --destination " + HugoBuilder.TEMP_PUBLIC;
        } else {
            hugoCmd += " --destination " + destination;
        }

        if(baseUrl != null && !"".equals(baseUrl.trim())) {
            hugoCmd += " --baseURL " + baseUrl;
        }

        if(verbose) {
            hugoCmd += " --verbose";
        }

        return hugoCmd;
    }

    public String getHugoHome()
    {
        return hugoHome;
    }

    @DataBoundSetter
    public void setHugoHome(String hugoHome)
    {
        this.hugoHome = hugoHome;
    }

    @Extension
    @Symbol("hugo")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
    {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType)
        {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName()
        {
            return Messages.hugo_builder_name();
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @DataBoundSetter
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDestination() {
        return destination;
    }

    @DataBoundSetter
    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isVerbose() {
        return verbose;
    }

    @DataBoundSetter
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}