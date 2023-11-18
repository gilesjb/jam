pipeline {
    agent any
    
    tools {
        jdk 'Java17'
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'javac -sourcepath src/main -d classes src/main/**.java || exit 1'
                sh 'java -classpath classes src/scripts/JamProject.java clean release || exit 1'
            }
        }
    }
}
