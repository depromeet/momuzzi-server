pipeline {
    agent any
    options {
        skipDefaultCheckout(true)
    }

    triggers {
        githubPush()
    }

    environment {
        // 타임존 설정 (한국 시간)
        TZ = 'Asia/Seoul'
        
        // Registry 설정 (외부 접근용 HTTPS, 내부 푸시용 HTTP 분리)
        REGISTRY_URL = "registry.momuzzi.site:4430"          // Public pull용
        REGISTRY_PUSH_URL = "registry:5000"                  // Internal push용 (컨테이너 이름)
        REGISTRY_CREDENTIALS_ID = "depromeet-registry"
        
        // Docker 이미지 설정
        IMAGE_NAME = "depromeet-server-image"
        
        // NCP Server 설정
        NCP_SERVER_CREDENTIALS_ID = "depromeet-server"
        NCP_SERVER_HOST = "api.momuzzi.site"
        NCP_SERVER_USER = "ubuntu"

        DEPLOY_PATH = "/home/ubuntu/momuzzi-server"

        // SonarQube 설정
//         SONARQUBE_SERVER_NAME = "depromeet-sonarqube"
//         SONARQUBE_TOKEN_CREDENTIALS_ID = "sonarqube-token"
//         SONARQUBE_PROJECT_KEY = "depromeet-team3-server"
//         SONARQUBE_PROJECT_NAME = "Depromeet Team 3 Server"
//         SONARQUBE_SCANNER_IMAGE = "sonarsource/sonar-scanner-cli:5.0.1"
//         SONARQUBE_BINARY_PATH_JAVA = "module-api/build/classes/java/main"
//         SONARQUBE_BINARY_PATH_KOTLIN = "module-api/build/classes/kotlin/main"

        // Main 브랜치 감지 로직 통합
        IS_MAIN_BRANCH = "false"
        
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
                deleteDir()
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                    
                    // 현재 커밋 해시
                    def currentCommit = sh(
                        script: "git rev-parse HEAD",
                        returnStdout: true
                    ).trim()
                    
                    // origin/main의 HEAD 커밋 해시
                    def mainCommit = sh(
                        script: "git rev-parse origin/main",
                        returnStdout: true
                    ).trim()
                    
                    // 브랜치 확인
                    def branchCheckResult = sh(
                        script: """
                            if [ "${currentCommit}" = "${mainCommit}" ] || \
                               [ "\${BRANCH_NAME}" = "main" ] || \
                               [ "\${GIT_BRANCH}" = "origin/main" ] || \
                               [ "\${GIT_BRANCH}" = "main" ] || \
                               [ "\$(git branch --show-current)" = "main" ]; then
                                echo "Detected as main branch"
                                exit 0
                            else
                                echo "Not main branch"
                                exit 1
                            fi
                        """,
                        returnStatus: true
                    )
                    
                    echo "Current commit: ${currentCommit}"
                    echo "Main commit: ${mainCommit}"
                    echo "Branch check result: ${branchCheckResult}"
                    echo "Branch check result type: ${branchCheckResult.class}"
                    
                    // 명시적으로 문자열로 설정
                    if (branchCheckResult == 0) {
                        env.IS_MAIN_BRANCH = 'true'
                        echo "Setting IS_MAIN_BRANCH to true"
                    } else {
                        env.IS_MAIN_BRANCH = 'false'
                        echo "Setting IS_MAIN_BRANCH to false"
                    }
                    
                    echo "IS_MAIN_BRANCH: ${env.IS_MAIN_BRANCH}"
                }
            }
        }
        
        stage('Setup Environment') {
            steps {
                script {
                    sh 'chmod +x ./gradlew'
                    sh './gradlew --stop || true'
                    sh 'pkill -f "KotlinCompileDaemon" || true'
                    
                    // docker-compose 설치 (컨테이너 재생성 시마다 필요)
                    sh '''
                    if ! command -v docker-compose >/dev/null 2>&1; then
                        echo "Installing docker-compose..."
                        curl -L "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
                        chmod +x /usr/local/bin/docker-compose
                    fi
                    docker-compose --version
                    '''
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

        // stage('SonarQube Analysis (PR)') {
        //     when {
        //         changeRequest()
        //     }
        //     steps {
        //         script {
        //             withSonarQubeEnv("${SONARQUBE_SERVER_NAME}") {
        //                 withCredentials([string(
        //                     credentialsId: "${SONARQUBE_TOKEN_CREDENTIALS_ID}",
        //                     variable: 'SONARQUBE_TOKEN'
        //                 )]) {
        //                     sh """
        //                         mkdir -p module-api/build
        //                         docker run --rm \
        //                           -e SONAR_HOST_URL=$SONAR_HOST_URL \
        //                           -e SONAR_LOGIN=$SONARQUBE_TOKEN \
        //                           -v ${env.WORKSPACE}:/usr/src \
        //                           -w /usr/src \
        //                           ${SONARQUBE_SCANNER_IMAGE} sonar-scanner \
        //                             -Dsonar.projectKey=${SONARQUBE_PROJECT_KEY} \
        //                             -Dsonar.projectName="${SONARQUBE_PROJECT_NAME}" \
        //                             -Dsonar.sources=module-api/src/main/java,module-api/src/main/kotlin \
        //                             -Dsonar.tests=module-api/src/test/java,module-api/src/test/kotlin \
        //                             -Dsonar.java.binaries=${SONARQUBE_BINARY_PATH_JAVA} \
        //                             -Dsonar.kotlin.binaries=${SONARQUBE_BINARY_PATH_KOTLIN} \
        //                             -Dsonar.sourceEncoding=UTF-8 \
        //                             -Dsonar.pullrequest.key=${env.CHANGE_ID} \
        //                             -Dsonar.pullrequest.branch=${env.BRANCH_NAME} \
        //                             -Dsonar.pullrequest.base=${env.CHANGE_TARGET}
        //                     """
        //                 }
        //             }
        //         }
        //     }
        // }

        // stage('SonarQube Quality Gate (PR)') {
        //     when {
        //         changeRequest()
        //     }
        //     steps {
        //         script {
        //             withSonarQubeEnv("${SONARQUBE_SERVER_NAME}") {
        //                 timeout(time: 10, unit: 'MINUTES') {
        //                     waitForQualityGate abortPipeline: true
        //                 }
        //             }
        //         }
        //     }
        // }

        // stage('SonarQube Analysis (Main)') {
        //     when {
        //         expression {
        //             env.IS_MAIN_BRANCH == 'true'
        //         }
        //     }
        //     steps {
        //         script {
        //             withSonarQubeEnv("${SONARQUBE_SERVER_NAME}") {
        //                 withCredentials([string(
        //                     credentialsId: "${SONARQUBE_TOKEN_CREDENTIALS_ID}",
        //                     variable: 'SONARQUBE_TOKEN'
        //                 )]) {
        //                     sh """
        //                         mkdir -p module-api/build
        //                         docker run --rm \
        //                           -e SONAR_HOST_URL=$SONAR_HOST_URL \
        //                           -e SONAR_LOGIN=$SONARQUBE_TOKEN \
        //                           -v ${env.WORKSPACE}:/usr/src \
        //                           -w /usr/src \
        //                           ${SONARQUBE_SCANNER_IMAGE} sonar-scanner \
        //                             -Dsonar.projectKey=${SONARQUBE_PROJECT_KEY} \
        //                             -Dsonar.projectName="${SONARQUBE_PROJECT_NAME}" \
        //                             -Dsonar.sources=module-api/src/main/java,module-api/src/main/kotlin \
        //                             -Dsonar.tests=module-api/src/test/java,module-api/src/test/kotlin \
        //                             -Dsonar.java.binaries=${SONARQUBE_BINARY_PATH_JAVA} \
        //                             -Dsonar.kotlin.binaries=${SONARQUBE_BINARY_PATH_KOTLIN} \
        //                             -Dsonar.sourceEncoding=UTF-8
        //                     """
        //                 }
        //             }
        //         }
        //     }
        // }

        // stage('SonarQube Quality Gate (Main)') {
        //     when {
        //         expression {
        //             env.IS_MAIN_BRANCH == 'true'
        //         }
        //     }
        //     steps {
        //         script {
        //             withSonarQubeEnv("${SONARQUBE_SERVER_NAME}") {
        //                 timeout(time: 10, unit: 'MINUTES') {
        //                     waitForQualityGate abortPipeline: true
        //                 }
        //             }
        //         }
        //     }
        // }

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
                    // 현재 커밋과 origin/main 비교 (매번 직접 확인)
                    def currentCommit = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    def mainCommit = sh(script: "git rev-parse origin/main", returnStdout: true).trim()
                    def isMainBranch = (currentCommit == mainCommit)
                    
                    echo "Docker Build & Push: currentCommit=${currentCommit}, mainCommit=${mainCommit}, isMainBranch=${isMainBranch}"
                    
                    if (isMainBranch) {
                        // Docker 캐시 정리 (손상된 레이어 제거)
                        sh """
                            docker system prune -af
                            docker builder prune -af
                        """

                        def imageTag = "${env.GIT_COMMIT_SHORT}"
                        def fullImageName = "${REGISTRY_PUSH_URL}/${IMAGE_NAME}"

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
                                # Registry IP를 직접 사용 (DNS 문제 우회)
                                REGISTRY_IP=\$(docker inspect registry --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
                                echo "Using Registry IP: \$REGISTRY_IP"
                                
                                # Docker 로그아웃 후 재로그인 (IP 직접 사용)
                                docker logout \${REGISTRY_IP}:5000 || true

                                echo "Attempting login to \${REGISTRY_IP}:5000 with user: \$REGISTRY_USERNAME"
                                echo \$REGISTRY_PASSWORD | docker login \${REGISTRY_IP}:5000 -u \$REGISTRY_USERNAME --password-stdin

                                # 이미지 태그를 IP로 변경
                                docker tag ${fullImageName}:${imageTag} \${REGISTRY_IP}:5000/${IMAGE_NAME}:${imageTag}
                                docker tag ${fullImageName}:latest \${REGISTRY_IP}:5000/${IMAGE_NAME}:latest

                                # 이미지 정보 확인
                                docker images | grep \${REGISTRY_IP}:5000/${IMAGE_NAME}

                                # Push 시도 (IP 직접 사용)
                                docker push \${REGISTRY_IP}:5000/${IMAGE_NAME}:${imageTag}
                                docker push \${REGISTRY_IP}:5000/${IMAGE_NAME}:latest
                            """
                        }

                        // 로컬 이미지 정리
                        sh """
                            REGISTRY_IP=\$(docker inspect registry --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
                            docker rmi \${REGISTRY_IP}:5000/${IMAGE_NAME}:${imageTag} || true
                            docker rmi \${REGISTRY_IP}:5000/${IMAGE_NAME}:latest || true
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
                    // 현재 커밋과 origin/main 비교 (매번 직접 확인)
                    def currentCommit = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    def mainCommit = sh(script: "git rev-parse origin/main", returnStdout: true).trim()
                    def isMainBranch = (currentCommit == mainCommit)
                    
                    echo "Deploy to NCP: currentCommit=${currentCommit}, mainCommit=${mainCommit}, isMainBranch=${isMainBranch}"
                    
                    if (isMainBranch) {
                        // 서버의 .env 파일을 Jenkins 워크스페이스로 복사
                        sh '''
                            cp /home/ubuntu/momuzzi-server/.env .env
                            echo "환경 설정 파일 복사 완료"
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
# Backend 컨테이너만 제거
docker rm -f backend 2>/dev/null || true

if [ "\$REGISTRY_IMAGE_EXISTS" = true ]; then
    # Registry 이미지를 backend:latest로 태그 후 배포
    docker pull \${REGISTRY_URL}/\${IMAGE_NAME}:latest
    docker tag \${REGISTRY_URL}/\${IMAGE_NAME}:latest backend:latest
fi

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

# Nginx 컨테이너 재시작 (기존 컨테이너 제거 후 시작)
docker-compose -f docker-compose.prod.yml rm -f nginx 2>/dev/null || true
docker-compose -f docker-compose.prod.yml up -d nginx

# 사용하지 않는 Docker 리소스 정리 (디스크 절약)
echo "=== Cleaning up Docker resources ==="

# 1. 사용하지 않는 컨테이너 제거
docker container prune -f

# 2. 오래된 이미지 정리
# 주의: githubPush 트리거 사용 시 푸시 빈도에 따라 조정 필요
# 현재: 72시간(3일) 이상 사용 안 한 dangling 이미지만 삭제
docker image prune -af --filter "until=72h"

# 3. 사용하지 않는 볼륨 정리 - 안전하게
PROTECTED_VOLUMES="loki-data|grafana-data|prometheus-data|jenkins_home|registry_data"
echo "Protected volumes: \${PROTECTED_VOLUMES}"
docker volume ls -q | grep -vE "\${PROTECTED_VOLUMES}" | xargs -r docker volume rm 2>/dev/null || true

# 4. 빌드 캐시 정리 (3일 이상 된 것)
docker builder prune -af --filter "until=72h" || true

# 5. 디스크 사용량 확인
echo "=== Docker disk usage ==="
docker system df

# 6. 서버 디스크 사용량 확인
echo "=== Server disk usage ==="
df -h /

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
            script {
                // Gradle daemon 정리
                sh './gradlew --stop || true'
                
                // Jenkins 워크스페이스 정리 (디스크 절약)
                sh '''
                    echo "=== Cleaning up Jenkins workspace ==="
                    # 빌드 산출물 정리
                    find . -type d -name "build" -exec rm -rf {} + 2>/dev/null || true
                    find . -type f -name "*.jar" -mtime +1 -delete 2>/dev/null || true
                    
                    # 오래된 로그 파일 정리
                    find . -type f -name "*.log" -mtime +1 -delete 2>/dev/null || true
                '''
                
                // .gradle 캐시는 보존하면서 워크스페이스 정리
                cleanWs patterns: [[pattern: '.gradle/**', type: 'EXCLUDE']]
                
                // Jenkins 내부 디스크 정리
                sh '''
                    echo "=== Cleaning up old Jenkins builds ==="
                    # 7일 이상 된 빌드 로그 삭제
                    find /var/jenkins_home/jobs/*/builds/*/log -type f -mtime +7 -delete 2>/dev/null || true
                    
                    # 오래된 워크스페이스 정리 (현재 작업 중인 것은 제외)
                    CURRENT_WORKSPACE=$(pwd)
                    find /var/jenkins_home/workspace/* -maxdepth 0 -type d -mtime +3 2>/dev/null | while read workspace; do
                        if [ "$workspace" != "$CURRENT_WORKSPACE" ]; then
                            rm -rf "$workspace" 2>/dev/null || true
                        fi
                    done
                    
                    # 시스템 로그 정리
                    journalctl --vacuum-time=3d 2>/dev/null || true
                ''' 
            }
        }
        success {
            echo 'Pipeline succeeded!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
