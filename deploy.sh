#!/bin/bash

# --- [0] 설정 변수 ---
BACKEND_DIR="backend" 
BRANCH_NAME="deployment" 
DJANGO_PORT=8000 
# Python 실행 파일 경로 (backend/venv 내부)
PYTHON_EXECUTABLE="$BACKEND_DIR/venv/bin/python" 
# PIP 실행 파일 경로
PIP_EXECUTABLE="$BACKEND_DIR/venv/bin/pip" 

echo "--- 배포 시작: $(date) ---"

# --- [1] Git Pull (코드 업데이트) ---
echo "--- Git Pull 시작 ---"
git checkout $BRANCH_NAME
git pull origin $BRANCH_NAME
if [ $? -ne 0 ]; then
    echo "Git Pull 실패. 배포 중단."
    exit 1
fi
echo "--- Git Pull 완료 ---"


# --- [2] 의존성 설치 및 마이그레이션 (안정성 개선) ---
echo "--- 의존성 설치 및 마이그레이션 시작 ---"

# 의존성 설치 (venv의 pip 실행 파일을 직접 사용)
"$PIP_EXECUTABLE" install -r "$BACKEND_DIR/requirements.txt"

# 데이터베이스 마이그레이션
"$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" migrate
if [ $? -ne 0 ]; then
    echo "마이그레이션 실패. 배포는 계속하지만 확인 필요."
fi
echo "--- 환경 설정 및 마이그레이션 완료 ---"


# --- [3] 기존 서버 프로세스 종료 ---
echo "--- 기존 서버 프로세스 종료 ---"
PID=$(lsof -t -i:$DJANGO_PORT) 

if [ -n "$PID" ]; then
    kill -9 $PID
    echo "PID $PID (포트 $DJANGO_PORT) 종료됨"
else
    echo "실행 중인 Django 서버 없음"
fi


# --- [4] 새 서버 프로세스 실행 ---
echo "--- 새 서버 프로세스 실행 ---"
# cd 명령 없이, venv의 python과 manage.py 경로를 모두 명확하게 지정하여 실행
nohup "$PYTHON_EXECUTABLE" "$BACKEND_DIR/manage.py" runserver 0.0.0.0:$DJANGO_PORT &
echo "--- 서버 재시작 완료 ---"

echo "--- 배포 종료: $(date) ---"