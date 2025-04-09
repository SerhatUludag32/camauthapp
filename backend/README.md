# Camera Auth App Backend

This is the backend server for the Camera Auth App. It provides authentication and real-time overlay management for the Android client app.

## Features

- Access code validation
- WebSocket support for real-time communication
- Web-based control panel for sending overlays
- Support for text and image overlays

## Setup

1. Create a Python virtual environment:
```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

2. Install dependencies:
```bash
pip3 install -r requirements.txt
```

3. Run the server:
```bash
python3 run.py
```

The server will start on http://localhost:8000

## Usage

1. Access the control panel at http://localhost:8000
2. Use the Android app with access code: "123456"
3. Once connected, you can send overlays from the control panel

## API Endpoints

- `POST /api/auth/validate` - Validate access code
- `WS /ws/{session_id}` - WebSocket endpoint for real-time communication
- `POST /api/overlay/{session_id}` - Send overlay to a connected client

## Android App Configuration

Update the following in your Android app:
1. Set the base URL to `http://your_computer_ip:8000`
2. Use the WebSocket URL `ws://your_computer_ip:8000/ws/{session_id}`
3. Use access code "123456" for testing 