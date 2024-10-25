pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew plugin:build'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/build/libs/*.jar'
        }
        failure {
            echo 'Build failed!'
        }
        success {
            echo 'Build completed successfully!'
        }
    }
}