pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code'
                checkout scm
            }
        }

        stage('Run JMeter') {
            steps {
                sh '''
                    mkdir -p results
                    jmeter -n \
                      -t test.jmx \
                      -l results/results.jtl \
                      -e -o results/report
                '''
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished'
            archiveArtifacts artifacts: 'results/**', fingerprint: true
        }
    }
}
