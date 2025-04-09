from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, Depends, Request
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.middleware.cors import CORSMiddleware
from typing import Dict, Set, Optional, List
import json
import uuid
from datetime import datetime, timedelta
from pydantic import BaseModel
import os

app = FastAPI()

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

# Get the directory of the current file
current_dir = os.path.dirname(os.path.abspath(__file__))

# Serve static files and templates
app.mount("/static", StaticFiles(directory=os.path.join(current_dir, "static")), name="static")
templates = Jinja2Templates(directory=os.path.join(current_dir, "templates"))

# In-memory storage (replace with database in production)
active_sessions: Dict[str, dict] = {}  # session_id -> session_data
active_connections: Dict[str, WebSocket] = {}  # session_id -> websocket
valid_access_codes = {"123456"}  # Replace with your access codes

class AccessCodeValidationRequest(BaseModel):
    code: str
    device_info: dict

class OverlayData(BaseModel):
    type: str
    content: str
    width: int = 200
    height: int = 200
    position_x: int = 0
    position_y: int = 0

class SessionInfo(BaseModel):
    session_id: str
    device_info: dict
    created_at: datetime
    is_connected: bool

@app.get("/", response_class=HTMLResponse)
async def get_control_panel(request: Request):
    return templates.TemplateResponse("control_panel.html", {"request": request})

@app.get("/api/sessions", response_model=List[SessionInfo])
async def get_active_sessions():
    sessions = []
    for session_id, session_data in active_sessions.items():
        sessions.append(SessionInfo(
            session_id=session_id,
            device_info=session_data["device_info"],
            created_at=session_data["created_at"],
            is_connected=session_id in active_connections
        ))
    return sessions

@app.post("/api/auth/validate")
async def validate_access_code(request: AccessCodeValidationRequest):
    if request.code not in valid_access_codes:
        raise HTTPException(status_code=401, detail="Invalid access code")
    
    # Generate session
    session_id = str(uuid.uuid4())
    token = str(uuid.uuid4())  # In production, use proper JWT tokens
    
    # Store session
    active_sessions[session_id] = {
        "device_info": request.device_info,
        "created_at": datetime.now(),
        "token": token
    }
    
    print(f"New session created: {session_id} for device: {request.device_info}")
    
    return {
        "token": token,
        "sessionId": session_id
    }

@app.websocket("/ws/{session_id}")
async def websocket_endpoint(websocket: WebSocket, session_id: str):
    if session_id not in active_sessions:
        await websocket.close(code=4001, reason="Invalid session")
        return
    
    # Get the token from the query parameters
    token = websocket.query_params.get("token")
    if not token or token != active_sessions[session_id]["token"]:
        await websocket.close(code=4003, reason="Invalid token")
        return
    
    await websocket.accept()
    active_connections[session_id] = websocket
    print(f"WebSocket connected: {session_id}")
    
    try:
        while True:
            # Keep connection alive and wait for messages
            data = await websocket.receive_text()
            # You can handle incoming messages from the client here if needed
    except WebSocketDisconnect:
        if session_id in active_connections:
            del active_connections[session_id]
            print(f"WebSocket disconnected: {session_id}")

@app.post("/api/overlay/{session_id}")
async def send_overlay(session_id: str, overlay: OverlayData):
    if session_id not in active_connections:
        raise HTTPException(status_code=404, detail="Session not found or disconnected")
    
    # Log the received overlay data
    print(f"Received overlay data: {overlay.dict()}")
    
    websocket = active_connections[session_id]
    message = {
        "type": "overlay",
        "data": overlay.dict()
    }
    
    # Log the message being sent
    print(f"Sending WebSocket message: {message}")
    
    await websocket.send_text(json.dumps(message))
    return {"status": "success"}

# Cleanup expired sessions periodically (in production, use background tasks)
@app.on_event("startup")
async def startup_event():
    # Clean up sessions older than 24 hours
    expired = datetime.now() - timedelta(hours=24)
    expired_sessions = [
        session_id for session_id, data in active_sessions.items()
        if data["created_at"] < expired
    ]
    for session_id in expired_sessions:
        del active_sessions[session_id]
        print(f"Cleaned up expired session: {session_id}") 