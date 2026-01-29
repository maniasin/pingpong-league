# JDK 17 및 Debian Jammy 기반 이미지를 사용합니다.
FROM eclipse-temurin:17-jdk-jammy

# 작업 디렉토리 설정
WORKDIR /app

# APT 설정 변경:
# 1. 기존 APT 캐시 및 목록 파일 제거
# 2. 다운로드 파이프라인 비활성화 및 리다이렉션 허용하지 않음
# 3. Ubuntu 패키지 미러 서버를 카카오 서버로 변경
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    echo 'Acquire::http::Pipeline-Depth "0";' > /etc/apt/apt.conf.d/99disable-pipelining && \
    echo 'Acquire::http::AllowRedirect "false";' >> /etc/apt/apt.conf.d/99disable-pipelining && \
    mv /etc/apt/sources.list /etc/apt/sources.list.bak && \
    echo "deb http://mirror.kakao.com/ubuntu jammy main restricted universe multiverse" > /etc/apt/sources.list && \
    echo "deb http://mirror.kakao.com/ubuntu jammy-updates main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb http://mirror.kakao.com/ubuntu jammy-backports main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb http://mirror.kakao.com/ubuntu jammy-security main restricted universe multiverse" >> /etc/apt/sources.list

# 시스템 업데이트 및 필요한 패키지 설치
# Google Chrome 설치를 위한 GPG 키 및 저장소 추가 (이 부분이 핵심!)
# headless Chrome 실행에 필요한 라이브러리 설치
RUN apt-get update && \
    apt-get install -yq ca-certificates curl gnupg && \
    # Google Chrome GPG 키 추가
    curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /etc/apt/keyrings/google-chrome.gpg && \
    # Google Chrome 저장소 추가
    echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" | tee /etc/apt/sources.list.d/google-chrome.list > /dev/null && \
    # Node.js 저장소 추가 (기존에 있었으므로 유지)
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list > /dev/null && \
    apt-get update && \
    apt-get install -yq google-chrome-stable --no-install-recommends && \
    apt-get install -yq libglib2.0-0 libnss3 libnspr4 libfontconfig1 libcups2 libxss1 libdbus-1-3 libpangocairo-1.0-0 libatk1.0-0 libatk-bridge2.0-0 libgdk-pixbuf2.0-0 libgtk-3-0 libgbm1 libasound2 \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# WebDriverManager가 ChromeDriver를 다운로드할 경로를 환경 변수로 설정
ENV WDM_CHROMEDRIVER_PATH=/usr/bin/chromedriver

# 애플리케이션 JAR 파일 복사
COPY build/libs/*.jar app.jar

# Spring Boot 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]

# 기존 8080 외에 8082도 추가
EXPOSE 8080 8082
