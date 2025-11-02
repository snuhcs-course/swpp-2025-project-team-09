#!/bin/bash

# --- [0] 설정 변수 ---
BACKEND_DIR="backend" 
BRANCH_NAME="deployment" 
DJANGO_PORT=8000 
PYTHON_EXECUTABLE="./$BACKEND_DIR/venv/bin/python" 
PIP_EXECUTABLE="./$BACKEND_DIR/venv/bin/pip"
SERVER_LOG="$BACKEND_DIR/server.log" 

echo "--- 배포 시작: $(date) ---"

# --- [1] Git Pull (코드 업데이트) ---
echo "--- Git Pull 시작 ---"
git checkout $BRANCH_NAME
git pull origin $BRANCH_NAME
if [ $? -ne 0 ]; then
    echo "::error::Git Pull 실패. 배포 중단."
    exit 1
fi
echo "--- Git Pull 완료 ---"


# --- [2] 의존성 설치 ---
echo "--- 의존성 설치 시작 ---"
"$PIP_EXECUTABLE" install -r "$BACKEND_DIR/requirements.txt"
if [ $? -ne 0 ]; then
    echo "::warning::의존성 설치 실패. 이전 의존성으로 실행."
fi
echo "--- 의존성 설치 완료 ---"


# --- [3] DB 초기화 (형식 변경 대응) ---
echo "--- ⚠️ DB 초기화 시작 (모든 데이터 삭제) ---"

# SQLite인 경우 (db.sqlite3 파일 삭제)
if [ -f "$BACKEND_DIR/db.sqlite3" ]; then
    rm -f "$BACKEND_DIR/db.sqlite3"
    echo "SQLite DB 파일 삭제 완료"
fi

# PostgreSQL 등 다른 DB를 사용하는 경우 예시:
# "$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" flush --noinput
# 또는 django-extensions 설치 시:
# "$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" reset_db --noinput

# 마이그레이션 다시 적용
"$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" migrate --noinput
if [ $? -ne 0 ]; then
    echo "::error::마이그레이션 실패. DB 스키마 확인 필요."
    exit 1
fi
echo "--- DB 재생성 완료 ---"


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
