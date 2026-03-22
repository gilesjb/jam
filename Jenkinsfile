pipeline {
    agent any
    
    tools {
        jdk 'Java19'
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
