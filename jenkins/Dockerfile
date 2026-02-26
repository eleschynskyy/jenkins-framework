FROM jenkins/jenkins:lts

USER root
COPY ./plugins.txt /usr/share/jenkins/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/plugins.txt

USER jenkins