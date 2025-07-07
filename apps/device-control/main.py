"""Device Control Service - Main Application."""

import os
import logging
import redis
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from dotenv import load_dotenv

from app.api.routes import create_routes
from app.api.handlers import DeviceControlHandlers
from app.api.cleanup import router as cleanup_router
from app.services.redis_service import RedisService
from app.services.command_handler import CommandHandler
from app.core.dependencies import set_redis_service
from app.events.listener import DeviceEventListener

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Global instances
redis_service = None
command_handler = None
event_listener = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    global redis_service, command_handler, event_listener
    
    # Startup
    logger.info("Starting Device Control Service...")
    
    # Initialize Redis connection
    redis_host = os.getenv('DEVICE_CONTROL_REDIS_HOST', 'redis-device-control')
    redis_port = int(os.getenv('DEVICE_CONTROL_REDIS_PORT', '6379'))
    redis_password = os.getenv('DEVICE_CONTROL_REDIS_PASSWORD', '')
    
    try:
        redis_client = redis.Redis(
            host=redis_host,
            port=redis_port,
            password=redis_password if redis_password else None,
            decode_responses=False,
            socket_connect_timeout=5
        )
        
        # Test connection
        redis_client.ping()
        logger.info(f"Connected to Redis at {redis_host}:{redis_port}")
        
        # Initialize services
        redis_service = RedisService(redis_client)
        
        # Set global redis service for dependency injection
        set_redis_service(redis_service)
        
        device_registry_url = os.getenv(
            'DEVICE_REGISTRY_URL', 
            'http://device-registry:8082'
        )
        command_handler = CommandHandler(redis_service, device_registry_url)
        
        # Initialize and start event listener
        event_listener = DeviceEventListener(redis_service)
        await event_listener.start()
        
        # Setup routes
        handlers = DeviceControlHandlers(redis_service, command_handler)
        router = create_routes(handlers)
        app.include_router(router)
        
        # Add cleanup routes
        app.include_router(cleanup_router)
        logger.info("API routes configured")
        
        logger.info("Device Control Service started successfully")
        
    except Exception as e:
        logger.error(f"Failed to initialize services: {e}")
        raise
    
    yield
    
    # Shutdown
    logger.info("Shutting down Device Control Service...")
    
    if event_listener:
        await event_listener.stop()
    
    if command_handler:
        await command_handler.close()
    
    logger.info("Device Control Service stopped")


# Create FastAPI application
app = FastAPI(
    title="Device Control Service",
    description="Service for managing device states and commands using Redis",
    version="1.0.0",
    lifespan=lifespan
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify actual origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Root endpoint
@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "Device Control Service",
        "version": "1.0.0",
        "status": "running"
    }


if __name__ == "__main__":
    import uvicorn
    
    host = os.getenv('DEVICE_CONTROL_HOST', '0.0.0.0')
    port = int(os.getenv('DEVICE_CONTROL_PORT', '8083'))
    
    uvicorn.run(
        app,
        host=host,
        port=port,
        log_level="info"
    ) 