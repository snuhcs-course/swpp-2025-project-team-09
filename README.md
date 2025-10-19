# StoryBridge Iteration 2 demo

## How to run
To demonstrate the app with a local backend, we used **ngrok** to expose the backend server to your mobile emulator. Make sure the mobile device is connected to the same Wi-Fi as your local machine.

### 1. Start Django Backend
Open a terminal and run:

```bash
cd backend
python manage.py runserver 0.0.0.0:8000
```
This will start the Django development server accessible on your local network.

### 2. Start ngrok
In a separate terminal, run:
```bash
cd backend
python ngrok.py
```
If you encounter an authentication error, follow these steps:

1. Sign up at https://ngrok.com/.  
2. Go to **Your Authtoken** from the left menu and copy your token.  
3. Add your token via terminal and restart ngrok:
```bash
ngrok config add-authtoken <YOUR_TOKEN>
python ngrok.py
```

Then run:
```bash
ngrok http 8000
```
It opens UI:
![image.png](attachment:90f8bf33-9d89-4f66-9b22-649c8f2af480:image.png)

### 3. Accessing the Web Interface
- ngrok provides a **Web Interface** where you can monitor incoming HTTP requests.
- The **Forwarding URL** should be used as the `BASE_URL` in `RetrofitClient` in your Android app.
  ![Screenshot 2025-10-19 at 8.17.26 PM.png](attachment:b0402190-50d2-4ea9-ad0a-d4c9041ce549:Screenshot_2025-10-19_at_8.17.26_PM.png)

### 4. Demo
Once ngrok is running, you can run the Android emulator or connect a physical device to the same Wi-Fi. All API calls will be forwarded to your local backend via the ngrok URL.
