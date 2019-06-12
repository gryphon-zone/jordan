pipeline {

    agent {
        docker {
            image 'maven:3-jdk-11'
            args '-v /root/.m2:/root/.m2'
        }
    }

    options {
        buildDiscarder logRotator(daysToKeepStr: '30', numToKeepStr: '10')
        disableConcurrentBuilds()
        disableResume()
        timeout(activity: true, time: 20)
        timestamps()
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
