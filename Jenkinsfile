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
                echo 'Running JMeter'
                sh '''
                    rm -rf results/report
                    mkdir -p results
                    /Users/Yevhen_Leshchynskyy/EPAM/apache-jmeter-5.6.3/bin/jmeter -n \
                      -t test.jmx \
                      -l results/results.jtl \
                      -e -o results/report \
                      -f
                '''
            }
        }
    }

    post {
    always {
        perfReport(
            sourceDataFiles: 'results/results.jtl',
            errorFailedThreshold: 0,
            errorUnstableThreshold: 0
        )
        archiveArtifacts artifacts: 'results/**'
    }
    }
}
