pipelineJob('sandbox') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/eleschynskyy/test_template.git')
                    }
                    branch('*/main')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
}