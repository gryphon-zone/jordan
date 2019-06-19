pipeline {

    agent {
        docker {
            image 'maven:3-jdk-11'
            args '-v /root/.m2:/root/.m2'
        }
    }

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder logRotator(daysToKeepStr: '30', numToKeepStr: '100')
        disableConcurrentBuilds()
        disableResume()
        timeout(activity: true, time: 20)
        durabilityHint 'PERFORMANCE_OPTIMIZED'
    }

    stages {

        stage('Log Maven and Java versions'){
            steps {
                sh 'mvn --version'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean install'
            }
        }
    }
}
