pipeline {
    agent {
        label 'nft'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code'
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'echo Running inside dynamic Kubernetes agent'
            }
        }
    }

    post {
    always {
        echo "✅ DONE"
    }
    }
}
