#!/bin/bash

# --- [0] 설정 변수 ---
BACKEND_DIR="backend" 
BRANCH_NAME="deployment" 
DJANGO_PORT=8000 
PYTHON_EXECUTABLE="./$BACKEND_DIR/venv/bin/python" 
PIP_EXECUTABLE="./$BACKEND_DIR/venv/bin/pip"
# 서버 로그 파일 경로 (nohup 출력 리다이렉션용)
SERVER_LOG="$BACKEND_DIR/server.log" 

echo "--- 배포 시작: $(date) ---"

# --- [1] Git Pull (코드 업데이트) ---
echo "--- Git Pull 시작 ---"
git checkout $BRANCH_NAME
git pull origin $BRANCH_NAME
if [ $? -ne 0 ]; then
    echo "::error::Git Pull 실패. 배포 중단." # <--- 🌟 디버깅: GitHub Actions 오류 출력
    exit 1
fi
echo "--- Git Pull 완료 ---"


# --- [2] 의존성 설치 및 마이그레이션 (안정성 개선) ---
echo "--- 의존성 설치 및 마이그레이션 시작 ---"

# 의존성 설치 (venv의 pip 실행 파일을 직접 사용)
"$PIP_EXECUTABLE" install -r "$BACKEND_DIR/requirements.txt"
if [ $? -ne 0 ]; then
    echo "::warning::의존성 설치 실패. 이전 의존성으로 실행." # <--- 🌟 디버깅: GitHub Actions 경고 출력
fi

# 데이터베이스 마이그레이션
"$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" migrate
if [ $? -ne 0 ]; then
    echo "::warning::마이그레이션 실패. DB 스키마 확인 필요." # <--- 🌟 디버깅: GitHub Actions 경고 출력
fi
echo "--- 환경 설정 및 마이그레이션 완료 ---"


# --- [3] 기존 서버 프로세스 종료 (확실하게) ---
echo "--- 기존 서버 프로세스 종료 ---"
# lsof 대신 pidof를 사용하거나, lsof의 결과를 여러 개 처리하기 위한 Bash 배열/루프를 사용해야 합니다.
# 여기서는 lsof 결과를 한 줄씩 읽어와 처리하는 루프를 사용합니다.
lsof -t -i:$DJANGO_PORT | while read PID; do
    echo "DEBUG: 기존 PID $PID 종료 시도 중..." # <--- 🌟 디버깅: 종료할 PID 출력
    kill -9 $PID
    if [ $? -eq 0 ]; then
        echo "PID $PID (포트 $DJANGO_PORT) 종료됨"
    else
        echo "WARNING: PID $PID 종료 실패." # <--- 🌟 디버깅: 종료 실패 시 메시지
    fi
done

# 종료 후 잠시 대기 (포트가 완전히 해제되도록)
sleep 2 


# --- [4] 새 서버 프로세스 실행 (출력 리다이렉션으로 블록킹 방지) ---
echo "--- 새 서버 프로세스 실행 ---"
# 기존 로그를 삭제하고, 서버의 모든 출력을 로그 파일로 리다이렉션하여 SSH 세션이 블록되는 것을 방지
rm -f "$SERVER_LOG"
nohup "$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" runserver 0.0.0.0:$DJANGO_PORT > "$SERVER_LOG" 2>&1 &

# 서버가 시작될 시간을 잠시 대기
sleep 5 

# 서버가 제대로 시작되었는지 확인 (선택 사항: 8000번이 다시 LISTENING 상태인지 확인)
# lsof -i :$DJANGO_PORT
# echo "DEBUG: 새 서버 상태 확인 명령어 실행됨 (로그에서 확인 필요)"

echo "--- 서버 재시작 완료 ---"

echo "--- 배포 종료: $(date) ---"