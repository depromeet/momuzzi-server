pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        // 타임존 설정 (한국 시간)
        TZ = 'Asia/Seoul'
        
        // Registry 설정
        REGISTRY_URL = "registry.momuzzi.site"
        REGISTRY_CREDENTIALS_ID = "depromeet-registry"
        
        // Docker 이미지 설정
        IMAGE_NAME = "depromeet-server-image"
        
        // NCP Server 설정
        NCP_SERVER_CREDENTIALS_ID = "depromeet-server"
        NCP_SERVER_HOST = "api.momuzzi.site"
        NCP_SERVER_USER = "ubuntu"

        DEPLOY_PATH = "/home/ubuntu/momuzzi-server"
        
        // Kotlin 컴파일 최적화
        GRADLE_OPTS = "-Xmx4g -XX:MaxMetaspaceSize=512m"
        
        // Gradle 캐시 설정
        GRADLE_USER_HOME = "${env.WORKSPACE}/.gradle"
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
                    sh 'chmod +x ./gradlew'
                    sh './gradlew --stop || true'
                    sh 'pkill -f "KotlinCompileDaemon" || true'
                    
                    // Docker 정리(일시적으로 주석 처리)
                    // sh 'docker system prune -f || true'
                    // sh 'docker builder prune -f || true'
                }
            }
        }

        stage('Build Application') {
            steps {
                script {
                    // 브랜치 정보 디버깅
                    echo "Current branch: ${env.BRANCH_NAME}"
                    echo "Git branch: ${env.GIT_BRANCH}"
                    sh 'echo "Git branch from command: $(git branch --show-current)"'
                    sh 'echo "All git branches: $(git branch -a)"'

                    sh '''
                    ./gradlew :module-api:clean :module-api:bootJar \
                    --no-daemon \
                    --stacktrace \
                    -x test \
                    -Dorg.gradle.jvmargs="-Xmx1g -XX:MaxMetaspaceSize=512m" \
                    -Dkotlin.daemon.jvm.options="-Xmx512m,-XX:MaxMetaspaceSize=256m" \
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
                        // Docker 캐시 정리 (손상된 레이어 제거)
                        sh """
                            docker system prune -af
                            docker builder prune -af
                        """

                        def imageTag = "${env.GIT_COMMIT_SHORT}"
                        def fullImageName = "${REGISTRY_URL}/${IMAGE_NAME}"

                        // Docker 이미지 빌드 (캐시 사용 안함)
                        sh """
                            docker build --no-cache -f module-api/Dockerfile -t ${fullImageName}:${imageTag} .
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

                                # Push 시도
                                docker push ${fullImageName}:${imageTag}
                                docker push ${fullImageName}:latest
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
                        // 인프라 서비스 확인 및 시작
                        sh '''
                            # 서버의 .env 파일을 Jenkins 워크스페이스로 복사
                            cp /home/ubuntu/momuzzi-server/.env .env
                            
                            # CI/CD 서비스 처리
                            CICD_HASH=$(find docker-compose.cicd-infra.yml module-infra/nginx/nginx-cicd.conf Jenkinsfile -type f -exec md5sum {} \\; | sort | md5sum | cut -d' ' -f1)
                            CICD_CACHE_FILE="/var/jenkins_home/cicd-config-hash"
                            
                            if [ ! -f "$CICD_CACHE_FILE" ] || [ "$(cat $CICD_CACHE_FILE)" != "$CICD_HASH" ]; then
                                # 의존 관계를 고려한 순서로 재생성: registry → registry-web → jenkins → nginx
                                docker-compose -f docker-compose.cicd-infra.yml up -d --force-recreate registry
                                docker-compose -f docker-compose.cicd-infra.yml up -d --force-recreate registry-web
                                docker-compose -f docker-compose.cicd-infra.yml up -d --force-recreate jenkins
                                docker-compose -f docker-compose.cicd-infra.yml up -d --force-recreate nginx
                                echo "$CICD_HASH" > "$CICD_CACHE_FILE"
                                echo "$(date): CI/CD 설정 변경됨, 의존 관계 고려하여 재생성됨" >> /var/log/jenkins-deploy.log
                            else
                                docker-compose -f docker-compose.cicd-infra.yml up -d
                            fi
                            
                            # 모니터링 서비스 처리
                            MONITORING_HASH=$(find docker-compose.monitoring.yml module-infra/nginx/nginx-app.conf -type f -exec md5sum {} \\; | sort | md5sum | cut -d' ' -f1)
                            MONITORING_CACHE_FILE="/var/jenkins_home/monitoring-config-hash"
                            
                            if [ ! -f "$MONITORING_CACHE_FILE" ] || [ "$(cat $MONITORING_CACHE_FILE)" != "$MONITORING_HASH" ]; then
                                docker-compose -f docker-compose.monitoring.yml up -d --remove-orphans
                                echo "$MONITORING_HASH" > "$MONITORING_CACHE_FILE"
                                echo "$(date): 모니터링 설정 변경됨, 컨테이너 재생성됨" >> /var/log/jenkins-deploy.log
                            else
                                docker-compose -f docker-compose.monitoring.yml up -d
                            fi
                        '''
                        
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
    # 이미지 존재 여부 확인 (HTTPS 사용)
    if docker pull \${REGISTRY_URL}/\${IMAGE_NAME}:latest > /dev/null 2>&1; then
        echo "Registry image found, using registry image"
        REGISTRY_IMAGE_EXISTS=true
    else
        echo "Registry image not found, will build locally"
    fi
else
    echo "Registry login failed, will build locally"
fi

# 애플리케이션 서비스 재시작
docker-compose -f docker-compose.prod.yml stop backend nginx || true
docker-compose -f docker-compose.prod.yml rm -f backend nginx || true

if [ "\$REGISTRY_IMAGE_EXISTS" = true ]; then
    # Registry 이미지를 backend:latest로 태그 후 배포
    docker pull \${REGISTRY_URL}/\${IMAGE_NAME}:latest
    docker tag \${REGISTRY_URL}/\${IMAGE_NAME}:latest backend:latest
fi

# Backend 먼저 시작
docker-compose -f docker-compose.prod.yml up -d backend

# Backend가 healthy 상태가 될 때까지 대기 (최대 2분)
echo "Waiting for backend to be healthy..."
for i in {1..24}; do
    if docker inspect --format='{{.State.Health.Status}}' backend 2>/dev/null | grep -q "healthy"; then
        echo "Backend is healthy"
        break
    fi
    echo "Waiting... (\$i/24)"
    sleep 5
done

# Nginx 시작
docker-compose -f docker-compose.prod.yml up -d nginx

# 사용하지 않는 이미지 정리
docker image prune -af --filter "until=24h"

# 배포 상태 확인
sleep 10
docker ps
echo "Checking service health..."
docker inspect --format='{{.Name}}: {{.State.Health.Status}}' backend nginx || true
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
    }
    
    post {
        always {
            // Gradle daemon 정리
            sh './gradlew --stop || true'
            // .gradle 캐시는 보존하면서 워크스페이스 정리
            cleanWs patterns: [[pattern: '.gradle/**', type: 'EXCLUDE']]
        }
        success {
            echo 'Pipeline succeeded!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
