pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew plugin:build'
            }
        }
    }
}