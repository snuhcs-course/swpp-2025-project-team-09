#!/bin/bash

# --- [0] 설정 변수 ---
BACKEND_DIR="backend" 
BRANCH_NAME="deployment" 
DJANGO_PORT=8000 
PYTHON_EXECUTABLE="./$BACKEND_DIR/venv/bin/python" 
PIP_EXECUTABLE="./$BACKEND_DIR/venv/bin/pip"
SERVER_LOG="$BACKEND_DIR/server.log" 

echo "--- 배포 시작: $(date) ---"

# --- [1] Git Pull (무조건 최신 상태로 덮어쓰기) ---
echo "--- Git Pull (Force Sync) ---"

git fetch origin $BRANCH_NAME
git checkout $BRANCH_NAME 
git reset --hard origin/$BRANCH_NAME
git clean -fd

echo "--- ✅ Git Repository 완전 동기화 완료 ---"



# --- [2] 의존성 설치 ---
echo "--- 의존성 설치 시작 ---"
"$PIP_EXECUTABLE" install -r "$BACKEND_DIR/requirements.txt"
if [ $? -ne 0 ]; then
    echo "::warning::의존성 설치 실패. 이전 의존성으로 실행."
fi
echo "--- 의존성 설치 완료 ---"

# --- [3] DB 초기화 (형식 변경 대응) ---
echo "--- ⚠️ DB 형식 변경 대응: 기존 DB 삭제 후 새로 생성 ---"

if [ -f "$BACKEND_DIR/db.sqlite3" ]; then
    rm -f "$BACKEND_DIR/db.sqlite3"
    echo "SQLite DB 파일 삭제 완료 (형식 변경 대응)"
fi

# 새로운 스키마로 DB 재생성
"$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" makemigrations --noinput
"$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" migrate --noinput

echo "--- 새 DB 스키마 생성 완료 ---"



# --- [4] 기존 서버 프로세스 종료 ---
echo "--- 기존 서버 프로세스 종료 ---"
lsof -t -i:$DJANGO_PORT | while read PID; do
    echo "DEBUG: 기존 PID $PID 종료 시도 중..."
    kill -9 $PID
    if [ $? -eq 0 ]; then
        echo "PID $PID (포트 $DJANGO_PORT) 종료됨"
    else
        echo "WARNING: PID $PID 종료 실패."
    fi
done
sleep 2 


# --- [5] 새 서버 실행 ---
echo "--- 새 서버 프로세스 실행 ---"
rm -f "$SERVER_LOG"
nohup "$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" runserver 0.0.0.0:$DJANGO_PORT > "$SERVER_LOG" 2>&1 &

sleep 5 
echo "--- 서버 재시작 완료 ---"
echo "--- 배포 종료: $(date) ---"
