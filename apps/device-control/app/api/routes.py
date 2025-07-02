"""API routes for Device Control Service."""

from fastapi import APIRouter, Depends, Query
from typing import Optional, List

from ..models.device_state import (
    DeviceStateResponse, DeviceStateUpdate,
    CommandRequest, CommandResponse, CommandStatus,
    DeviceCommand
)
from .handlers import DeviceControlHandlers


def create_routes(handlers: DeviceControlHandlers) -> APIRouter:
    """Create API routes with handlers.
    
    Args:
        handlers: Device control handlers instance
        
    Returns:
        Configured APIRouter
    """
    router = APIRouter(prefix="/api/v1", tags=["device-control"])
    
    # Device State Endpoints
    
    @router.get("/devices/{device_id}/state", response_model=DeviceStateResponse)
    async def get_device_state(device_id: str):
        """Get current device state."""
        return await handlers.get_device_state(device_id)
    
    @router.put("/devices/{device_id}/state", response_model=DeviceStateResponse)
    async def update_device_state(device_id: str, update: DeviceStateUpdate):
        """Update device state."""
        return await handlers.update_device_state(device_id, update)
    
    # Command Endpoints
    
    @router.post("/devices/{device_id}/commands", response_model=CommandResponse)
    async def send_command(device_id: str, request: CommandRequest):
        """Send command to device."""
        return await handlers.send_command(device_id, request)
    
    @router.get("/devices/{device_id}/commands/{command_id}", response_model=DeviceCommand)
    async def get_command_status(device_id: str, command_id: str):
        """Get command status."""
        return await handlers.get_command_status(device_id, command_id)
    
    @router.delete("/devices/{device_id}/commands/{command_id}")
    async def cancel_command(device_id: str, command_id: str):
        """Cancel pending command."""
        return await handlers.cancel_command(device_id, command_id)
    
    @router.post("/devices/{device_id}/ping", response_model=CommandResponse)
    async def ping_device(device_id: str):
        """Send ping command to device."""
        return await handlers.ping_device(device_id)
    
    # Utility Endpoints
    
    @router.get("/devices/{device_id}/commands", response_model=List[DeviceCommand])
    async def get_device_commands(
        device_id: str,
        status: Optional[CommandStatus] = None,
        limit: int = Query(10, ge=1, le=100)
    ):
        """Get commands for a device with optional status filter."""
        return await handlers.get_device_commands(device_id, status, limit)
    
    @router.post("/devices/{device_id}/process-queue")
    async def process_device_queue(device_id: str):
        """Process pending commands for a device (for testing)."""
        return await handlers.process_device_queue(device_id)
    
    # Health Check
    
    @router.get("/health")
    async def health_check():
        """Health check endpoint."""
        return await handlers.health_check()
    
    return router 