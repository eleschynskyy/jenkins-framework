FROM jenkins/jenkins:lts

USER root

COPY ./plugins.txt /usr/share/jenkins/plugins.txt
COPY ./casc_configs /var/jenkins_home/casc_configs
COPY ./jobs-dsl /var/jenkins_home/jobs-dsl

RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/plugins.txt

USER jenkins
