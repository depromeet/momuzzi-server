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
        stage('CI Test (PR to dev)') {
            when {
                allOf {
                    changeRequest()
                    changeRequest target: 'dev'
                }
            }
            steps {
                echo "Running CI for PR → dev"
                sh './gradlew test --no-daemon --stacktrace'
            }
        }

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
        stage('Test') {
            when {
                anyOf {
                    allOf {
                        changeRequest()
                        changeRequest target: 'dev'
                    }
                    branch 'main'
                }
            }
            environment {
                GRADLE_USER_HOME = "${env.WORKSPACE}/.gradle"
            }
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    retry(2) {
                        sh '''#!/bin/bash -eo pipefail
./gradlew test --parallel --no-daemon --stacktrace --build-cache | tee test-result.log
'''
                    }
                }
            }
            post {
                always {
                    // 테스트 결과 리포트 저장
                    junit '**/build/test-results/test/*.xml'
                    // 테스트 요약 출력
                    sh 'grep -E "Test result|BUILD SUCCESSFUL|BUILD FAILED" -A 5 test-result.log || true'
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

# 애플리케이션 서비스만 시작
docker-compose -f docker-compose.prod.yml up -d

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
