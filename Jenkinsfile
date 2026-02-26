pipeline {
    agent any

    parameters {
        string(name: 'HOST', defaultValue: 'http://localhost', description: 'Base URL for all tools')
        string(
        defaultValue: '',
        description: 'Parameters for build.sh script (optional). If empty, build.sh runs without parameters',
        name: 'Build_parameters',
        trim: true
      )
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code'
                checkout scm
            }
        }

        stage('Show Executors') {
            steps {
                script {
                    def nodeName = env.NODE_NAME
                    echo "Current node: ${nodeName}"
                    def node = jenkins.model.Jenkins.instance.getNode(nodeName)
    
                    if (node == null) {
                        def masterExecutors = jenkins.model.Jenkins.instance.numExecutors
                        echo "Running on master node with ${masterExecutors} executor(s)"
                    } else {
                        echo "Node '${nodeName}' has ${node.numExecutors} executor(s)"
                    }
                }
            }
        }
    }

    post {
    always {
        echo "DONE"
    }
    }
}
