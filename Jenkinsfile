/*
 * Copyright 2019-2019 Gryphon Zone
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        stage('Log Maven and Java versions') {
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
