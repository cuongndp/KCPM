pipeline {
    agent any
    
    environment {
        DOCKER_CREDENTIALS = credentials('docker-credentials')
        KUBE_CONFIG = credentials('kube-config')
        SONAR_TOKEN = credentials('sonar-token')
        SONAR_HOST_URL = 'http://sonarqube:9000'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Backend') {
            steps {
                sh 'cd backend && mvn clean compile'
            }
        }
        
        stage('Test Backend') {
            steps {
                sh 'cd backend && mvn test'
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'cd backend && mvn sonar:sonar -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.login=${SONAR_TOKEN}'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                sh 'cd frontend && npm install'
                sh 'cd frontend && npm run build'
            }
        }
        
        stage('Package Backend') {
            steps {
                sh 'cd backend && mvn package -DskipTests'
            }
        }
        
        stage('Build Docker Images') {
            steps {
                sh 'docker build -t hospital-backend:${BUILD_NUMBER} ./backend'
                sh 'docker build -t hospital-frontend:${BUILD_NUMBER} ./frontend'
            }
        }
        
        stage('Push to Nexus') {
            steps {
                sh 'docker tag hospital-backend:${BUILD_NUMBER} nexus-repository:8082/hospital-backend:${BUILD_NUMBER}'
                sh 'docker tag hospital-frontend:${BUILD_NUMBER} nexus-repository:8083/hospital-frontend:${BUILD_NUMBER}'
                sh 'docker push nexus-repository:8082/hospital-backend:${BUILD_NUMBER}'
                sh 'docker push nexus-repository:8083/hospital-frontend:${BUILD_NUMBER}'
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/postgres-deployment.yaml'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/redis-deployment.yaml'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/kafka-deployment.yaml'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} set image deployment/hospital-backend hospital-backend=nexus-repository:8082/hospital-backend:${BUILD_NUMBER}'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} set image deployment/hospital-frontend hospital-frontend=nexus-repository:8083/hospital-frontend:${BUILD_NUMBER}'
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to Production?', ok: 'Deploy'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/postgres-deployment.yaml'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/redis-deployment.yaml'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/kafka-deployment.yaml'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} set image deployment/hospital-backend hospital-backend=nexus-repository:8082/hospital-backend:${BUILD_NUMBER}'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} set image deployment/hospital-frontend hospital-frontend=nexus-repository:8083/hospital-frontend:${BUILD_NUMBER}'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/hpa.yaml'
                sh 'kubectl --kubeconfig=${KUBE_CONFIG} apply -f k8s/ingress.yaml'
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline executed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}