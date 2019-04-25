# hugo-plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/hugo-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/hugo-plugin/job/master/)

# Features

* Auto release via git submodule

# Docker Agent

If you have a Docker environment, you can use [hugo-jnlp-agent](https://github.com/LinuxSuRen/hugo-jnlp-agent) to build your Hugo site.

# Pipeline

```
hugo baseUrl: '', destination: '', hugoHome: '/usr/local/bin/'
hugo baseUrl: 'https://surenpi.gitee.io/blog', destination: 'gitee', hugoHome: '/usr/local/bin/'

```

# Users

Here are part of who're using this plugin to build their Hugo site.

* [LinuxSuRen' blog](https://github.com/linuxsuren/surenpi)
* [Jenkins Community website](https://github.com/jenkins-zh/jenkins-zh)
