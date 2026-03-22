pipeline {
    agent any
    
    tools {
        jdk 'Java23'
    }
    
    stages {
        stage('Build') {
            steps {
                sh './setup || exit 1'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'build/*.jar', followSymlinks: false
            }
        }
    }
}
