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

        stage('Prepare Reports') {
            steps {
                script {
                    env.REPORT_ROOT = "reports/build-${env.BUILD_NUMBER}"
                }
                sh """
                    #!/usr/bin/env bash
                    set -euo pipefail
                    rm -rf "${env.REPORT_ROOT}"
                    mkdir -p "${env.REPORT_ROOT}/gatling" "${env.REPORT_ROOT}/jmeter" "${env.REPORT_ROOT}/lighthouse"
                    echo "PWD"
                    pwd
                """
            }
        }

        stage('DEBUG') {
            steps {
                script {
                    echo "reports/build-${env.BUILD_NUMBER}"
                    echo "${env.JOB_NAME}-------${env.BUILD_NUMBER}"
                    def bp = params.Build_parameters?.trim() ?: ''
                    echo ">>>>>>>>${bp}"
                    withEnv(["BUILD_PARAMS=${bp}"]) {
                        echo "XXXXXXXXXX ${BUILD_PARAMS}"
                    }
                } 
            }
        }

        stage('Run JMeter') {
            steps {
                echo 'Running JMeter'
                sh '''
                    ts_dir='${env.REPORT_ROOT}/jmeter'
                    rm -rf "\${ts_dir}"
                    tree
                    /Users/Yevhen_Leshchynskyy/EPAM/apache-jmeter-5.6.3/bin/jmeter -n \
                      -t test.jmx \
                      -l "\${ts_dir}/results.jtl" \
                      -e -o "\${ts_dir}/report" \
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
        archiveArtifacts artifacts: 'reports/**/*', allowEmptyArchive: true

        publishHTML([
                reportName: 'JMeter Performance Report',
                reportDir: '${env.REPORT_ROOT}/jmeter/report',
                reportFiles: 'index.html',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
        ])
    }
    }
}
