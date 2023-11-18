pipeline {
    agent any
    
    tools {
        jdk 'Java17'
    }
    

    stages {
        stage('Build') {
            steps {
                sh './setup'
            }
        }
    }
}
