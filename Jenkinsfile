pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        // Registry 설정
        REGISTRY_URL = "registry.momuzzi.site"
        REGISTRY_CREDENTIALS_ID = "depromeet-registry"
        
        // Docker 이미지 설정
        IMAGE_NAME = "depromeet-server-image"
        
        // NCP Server 설정
        NCP_SERVER_CREDENTIALS_ID = "depromeet-server"
        NCP_SERVER_HOST = "api.momuzzi.site"
        NCP_SERVER_USER = "ubuntu"

        DEPLOY_PATH = "/home/ubuntu/17th-team3-Server"
        
        // Kotlin 컴파일 최적화
        GRADLE_OPTS = "-Xmx4g -XX:MaxMetaspaceSize=1g"
    }
    
    tools {
        gradle 'gradle-8.14.3'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                }
            }
        }
        
        stage('Setup Environment') {
            steps {
                script {
                    // Gradle wrapper 실행 권한 부여
                    sh 'chmod +x ./gradlew'
                    
                    // Kotlin daemon 중지 (기존 데몬으로 인한 충돌 방지)
                    sh './gradlew --stop || true'
                    sh 'pkill -f "KotlinCompileDaemon" || true'
                    
                    // Docker 정리
                    sh 'docker system prune -f || true'
                    sh 'docker builder prune -f || true'
                }
            }
        }

        // 추후 사용
        //stage('Test') {
        //    when {
        //        anyOf {
        //            changeRequest()
        //            branch 'dev'
        //        }
        //    }
        //    steps {
        //        sh './gradlew test --no-daemon --stacktrace'
        //    }
        //}
        
        stage('Build Application') {
            steps {
                script {
                    // 브랜치 정보 디버깅
                    echo "Current branch: ${env.BRANCH_NAME}"
                    echo "Git branch: ${env.GIT_BRANCH}"
                    sh 'echo "Git branch from command: $(git branch --show-current)"'
                    sh 'echo "All git branches: $(git branch -a)"'
                    
                    sh '''
                        # Kotlin 컴파일 최적화로 빌드
                            ./gradlew :module-api:clean :module-api:bootJar \
                            --no-daemon \
                            --stacktrace \
                            -x test \
                            -Dorg.gradle.jvmargs="-Xmx4g -XX:MaxMetaspaceSize=1g" \
                            -Dkotlin.daemon.jvm.options="-Xmx2g,-XX:MaxMetaspaceSize=512m" \
                            -Dkotlin.incremental=false
                    '''
                }
            }
        }
        
        stage('Docker Build Test') {
            when {
                changeRequest()
            }
            steps {
                script {
                    sh '''
                        docker build -f module-api/Dockerfile -t test-build .
                        docker rmi test-build || true
                    '''
                }
            }
        }
        
        stage('Docker Build & Push') {
            steps {
                script {
                    def isMainBranch = env.BRANCH_NAME == 'main' || 
                                     env.GIT_BRANCH == 'origin/main' || 
                                     env.GIT_BRANCH == 'main' ||
                                     sh(script: 'git branch --show-current', returnStdout: true).trim() == 'main'
                    
                    if (isMainBranch) {
                        def imageTag = "${env.GIT_COMMIT_SHORT}"
                        def fullImageName = "${REGISTRY_URL}/${IMAGE_NAME}"
                        
                        // Docker 이미지 빌드
                        sh """
                            docker build -f module-api/Dockerfile -t ${fullImageName}:${imageTag} .
                            docker tag ${fullImageName}:${imageTag} ${fullImageName}:latest
                        """
                        
                        // Registry에 로그인 및 이미지 푸시
                        withCredentials([usernamePassword(
                            credentialsId: "${REGISTRY_CREDENTIALS_ID}",
                            usernameVariable: 'REGISTRY_USERNAME',
                            passwordVariable: 'REGISTRY_PASSWORD'
                        )]) {
                            sh """
                                # Docker 로그아웃 후 재로그인
                                docker logout ${REGISTRY_URL} || true
                                
                                echo "Attempting login to ${REGISTRY_URL} with user: \$REGISTRY_USERNAME"
                                echo \$REGISTRY_PASSWORD | docker login ${REGISTRY_URL} -u \$REGISTRY_USERNAME --password-stdin
                                
                                # 이미지 정보 확인
                                docker images | grep ${fullImageName}
                                
                                # Push 시도 (더 안정적인 방식)
                                docker push ${fullImageName}:${imageTag} --disable-content-trust
                                docker push ${fullImageName}:latest --disable-content-trust
                            """
                        }
                        
                        // 로컬 이미지 정리
                        sh """
                            docker rmi ${fullImageName}:${imageTag} || true
                            docker rmi ${fullImageName}:latest || true
                        """
                    } else {
                        echo "Skipping Docker Build & Push - not main branch"
                    }
                }
            }
        }
        
        stage('Deploy to NCP Server') {
            steps {
                script {
                    def isMainBranch = env.BRANCH_NAME == 'main' || 
                                     env.GIT_BRANCH == 'origin/main' || 
                                     env.GIT_BRANCH == 'main' ||
                                     sh(script: 'git branch --show-current', returnStdout: true).trim() == 'main'
                    
                    if (isMainBranch) {
                        withCredentials([usernamePassword(
                            credentialsId: "${REGISTRY_CREDENTIALS_ID}",
                            usernameVariable: 'REGISTRY_USERNAME',
                            passwordVariable: 'REGISTRY_PASSWORD'
                        )]) {
                            sshagent(credentials: ["${NCP_SERVER_CREDENTIALS_ID}"]) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${NCP_SERVER_USER}@${NCP_SERVER_HOST} << 'EOF'
export REGISTRY_USERNAME="${REGISTRY_USERNAME}"
export REGISTRY_PASSWORD="${REGISTRY_PASSWORD}"
export REGISTRY_URL="${REGISTRY_URL}"
export IMAGE_NAME="${IMAGE_NAME}"
export DEPLOY_PATH="${DEPLOY_PATH}"

# 배포 디렉토리로 이동
cd \${DEPLOY_PATH}

# Git 최신 코드 pull
git fetch origin
git reset --hard origin/main

# Docker 네트워크가 없으면 생성
docker network ls | grep depromeet_network || docker network create depromeet_network

# Registry 이미지 존재 여부 확인
REGISTRY_IMAGE_EXISTS=false

# Registry에 로그인 시도
if echo "\${REGISTRY_PASSWORD}" | docker login \${REGISTRY_URL} -u "\${REGISTRY_USERNAME}" --password-stdin; then
    # 이미지 존재 여부 확인
    if docker pull \${REGISTRY_URL}/\${IMAGE_NAME}:latest > /dev/null 2>&1; then
        echo "Registry image found, using registry image"
        REGISTRY_IMAGE_EXISTS=true
    else
        echo "Registry image not found, will build locally"
    fi
else
    echo "Registry login failed, will build locally"
fi

# 기존 컨테이너 종료
docker-compose -f docker-compose.prod.yml down --remove-orphans

if [ "\$REGISTRY_IMAGE_EXISTS" = true ]; then
    # Registry 이미지로 배포
    DOCKER_IMAGE=\${REGISTRY_URL}/\${IMAGE_NAME}:latest docker-compose -f docker-compose.prod.yml up -d
else
    # 로컬 빌드 배포
    docker-compose -f docker-compose.prod.yml up -d --build
fi

# 사용하지 않는 이미지 정리
docker image prune -af --filter "until=24h"

# 배포 상태 확인
sleep 30
docker ps
EOF
                                """
                            }
                        }
                    } else {
                        echo "Skipping Deploy to NCP Server - not main branch"
                    }
                }
            }
        }
        
        stage('Health Check') {
            steps {
                script {
                    def isMainBranch = env.BRANCH_NAME == 'main' || 
                                     env.GIT_BRANCH == 'origin/main' || 
                                     env.GIT_BRANCH == 'main' ||
                                     sh(script: 'git branch --show-current', returnStdout: true).trim() == 'main'
                    
                    if (isMainBranch) {
                        sshagent(credentials: ["${NCP_SERVER_CREDENTIALS_ID}"]) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${NCP_SERVER_USER}@${NCP_SERVER_HOST} << 'EOF'
# 헬스체크 (최대 5분 대기)
for i in {1..10}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "Health check passed!"
        exit 0
    fi
    echo "Waiting for application to start... (attempt \$i/10)"
    sleep 30
done
echo "Health check failed!"
exit 1
EOF
                            """
                        }
                    } else {
                        echo "Skipping Health Check - not main branch"
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Gradle daemon 정리
            sh './gradlew --stop || true'
            cleanWs()
        }
        success {
            echo 'Pipeline succeeded!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
