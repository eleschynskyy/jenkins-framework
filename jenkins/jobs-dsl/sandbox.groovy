pipelineJob('sandbox') {
    // Pipeline jobs do not support job-level node restriction (assignedNode is ignored).
    // The Jenkinsfile in the repo MUST start with node('nft') or agent { label 'nft' }.
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