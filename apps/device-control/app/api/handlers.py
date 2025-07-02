"""API handlers for device control operations."""

import logging
from typing import Optional, List
from fastapi import HTTPException, Query
import uuid

from ..models.device_state import (
    DeviceState, DeviceStateUpdate, DeviceStateResponse,
    CommandRequest, CommandResponse, CommandStatus,
    DeviceCommand
)
from ..services.redis_service import RedisService
from ..services.command_handler import CommandHandler

logger = logging.getLogger(__name__)


class DeviceControlHandlers:
    """Handlers for device control API endpoints."""
    
    def __init__(self, redis_service: RedisService, command_handler: CommandHandler):
        """Initialize handlers.
        
        Args:
            redis_service: Redis service instance
            command_handler: Command handler instance
        """
        self.redis_service = redis_service
        self.command_handler = command_handler
        logger.info("Device control handlers initialized")
    
    # Device State Endpoints
    
    async def get_device_state(self, device_id: str) -> DeviceStateResponse:
        """Get current device state.
        
        Args:
            device_id: UUID of the device
            
        Returns:
            Device state response
            
        Raises:
            HTTPException: If device not found
        """
        try:
            # Validate UUID format
            try:
                uuid.UUID(device_id)
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid device ID format")
            
            # Get device state
            state = await self.redis_service.get_device_state(device_id)
            
            if not state:
                # Try to validate device in registry
                if await self.command_handler.validate_device(device_id):
                    # Device exists but no state yet, get the initialized state
                    state = await self.redis_service.get_device_state(device_id)
                    if state:
                        return DeviceStateResponse(**state.model_dump())
                
                raise HTTPException(status_code=404, detail="Device not found")
            
            return DeviceStateResponse(**state.model_dump())
            
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Error getting device state {device_id}: {e}")
            raise HTTPException(status_code=500, detail="Internal server error")
    
    async def update_device_state(
        self, 
        device_id: str, 
        update: DeviceStateUpdate
    ) -> DeviceStateResponse:
        """Update device state.
        
        Args:
            device_id: UUID of the device
            update: State update data
            
        Returns:
            Updated device state
            
        Raises:
            HTTPException: If device not found or update fails
        """
        try:
            # Validate UUID format
            try:
                uuid.UUID(device_id)
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid device ID format")
            
            # Check device exists
            current_state = await self.redis_service.get_device_state(device_id)
            if not current_state:
                # Try to validate device in registry
                if not await self.command_handler.validate_device(device_id):
                    raise HTTPException(status_code=404, detail="Device not found")
                # Get the initialized state
                current_state = await self.redis_service.get_device_state(device_id)
            
            # Prepare updates
            updates = update.model_dump(exclude_unset=True)
            
            # Update device state
            updated_state = await self.redis_service.update_device_state(device_id, updates)
            
            if not updated_state:
                raise HTTPException(status_code=500, detail="Failed to update device state")
            
            return DeviceStateResponse(**updated_state.model_dump())
            
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Error updating device state {device_id}: {e}")
            raise HTTPException(status_code=500, detail="Internal server error")
    
    # Command Endpoints
    
    async def send_command(
        self,
        device_id: str,
        request: CommandRequest
    ) -> CommandResponse:
        """Send command to device.
        
        Args:
            device_id: UUID of the device
            request: Command request data
            
        Returns:
            Command response with ID and status
            
        Raises:
            HTTPException: If device not found or command creation fails
        """
        try:
            # Validate UUID format
            try:
                uuid.UUID(device_id)
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid device ID format")
            
            # Create command
            command = await self.command_handler.create_command(
                device_id=device_id,
                command_type=request.command_type,
                parameters=request.parameters,
                priority=request.priority,
                requested_by=request.requested_by
            )
            
            if not command:
                raise HTTPException(
                    status_code=400, 
                    detail="Failed to create command. Device may not exist or is in maintenance mode."
                )
            
            # Return response
            return CommandResponse(
                command_id=command.command_id,
                device_id=command.device_id,
                command_type=command.command_type,
                status=command.status,
                created_at=command.created_at,
                message=f"Command '{command.command_type}' queued for device {device_id}"
            )
            
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Error sending command to device {device_id}: {e}")
            raise HTTPException(status_code=500, detail="Internal server error")
    
    async def get_command_status(
        self,
        device_id: str,
        command_id: str
    ) -> DeviceCommand:
        """Get command status.
        
        Args:
            device_id: UUID of the device
            command_id: UUID of the command
            
        Returns:
            Command details
            
        Raises:
            HTTPException: If command not found
        """
        try:
            # Validate UUID formats
            try:
                uuid.UUID(device_id)
                uuid.UUID(command_id)
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid ID format")
            
            # Get command
            command = await self.redis_service.get_command(command_id)
            
            if not command:
                raise HTTPException(status_code=404, detail="Command not found")
            
            # Verify command belongs to device
            if command.device_id != device_id:
                raise HTTPException(status_code=404, detail="Command not found for this device")
            
            return command
            
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Error getting command {command_id}: {e}")
            raise HTTPException(status_code=500, detail="Internal server error")
    
    async def cancel_command(
        self,
        device_id: str,
        command_id: str
    ) -> dict:
        """Cancel a pending command.
        
        Args:
            device_id: UUID of the device
            command_id: UUID of the command
            
        Returns:
            Success message
            
        Raises:
            HTTPException: If command not found or cannot be cancelled
        """
        try:
            # Validate UUID formats
            try:
                uuid.UUID(device_id)
                uuid.UUID(command_id)
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid ID format")
            
            # Get command to verify device
            command = await self.redis_service.get_command(command_id)
            
            if not command:
                raise HTTPException(status_code=404, detail="Command not found")
            
            # Verify command belongs to device
            if command.device_id != device_id:
                raise HTTPException(status_code=404, detail="Command not found for this device")
            
            # Cancel command
            if await self.redis_service.cancel_command(command_id):
                return {"message": f"Command {command_id} cancelled successfully"}
            else:
                raise HTTPException(
                    status_code=400, 
                    detail="Cannot cancel command. It may not be in pending status."
                )
                
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Error cancelling command {command_id}: {e}")
            raise HTTPException(status_code=500, detail="Internal server error")
    
    async def ping_device(self, device_id: str) -> CommandResponse:
        """Send ping command to device.
        
        Args:
            device_id: UUID of the device
            
        Returns:
            Command response
            
        Raises:
            HTTPException: If device not found
        """
        try:
            # Create ping command with high priority
            request = CommandRequest(
                command_type="ping",
                parameters={},
                priority="high"
            )
            
            return await self.send_command(device_id, request)
            
        except Exception as e:
            logger.error(f"Error pinging device {device_id}: {e}")
            raise
    
    # Utility Endpoints
    
    async def get_device_commands(
        self,
        device_id: str,
        status: Optional[CommandStatus] = None,
        limit: int = Query(10, ge=1, le=100)
    ) -> List[DeviceCommand]:
        """Get commands for a device.
        
        Args:
            device_id: UUID of the device
            status: Optional filter by status
            limit: Maximum number of commands
            
        Returns:
            List of commands
            
        Raises:
            HTTPException: If device not found
        """
        try:
            # Validate UUID format
            try:
                uuid.UUID(device_id)
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid device ID format")
            
            # Check device exists
            if not await self.redis_service.get_device_state(device_id):
                if not await self.command_handler.validate_device(device_id):
                    raise HTTPException(status_code=404, detail="Device not found")
            
            # Get commands
            commands = await self.redis_service.get_device_commands(
                device_id, 
                status=status, 
                limit=limit
            )
            
            return commands
            
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Error getting device commands for {device_id}: {e}")
            raise HTTPException(status_code=500, detail="Internal server error")
    
    async def process_device_queue(self, device_id: str) -> dict:
        """Process pending commands for a device (for testing).
        
        Args:
            device_id: UUID of the device
            
        Returns:
            Processing result
            
        Raises:
            HTTPException: If device not found
        """
        try:
            # Validate UUID format
            try:
                uuid.UUID(device_id)
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid device ID format")
            
            # Check device exists
            if not await self.redis_service.get_device_state(device_id):
                if not await self.command_handler.validate_device(device_id):
                    raise HTTPException(status_code=404, detail="Device not found")
            
            # Process queue
            processed = await self.command_handler.process_device_queue(device_id)
            
            return {
                "device_id": device_id,
                "commands_processed": processed,
                "message": f"Processed {processed} commands for device {device_id}"
            }
            
        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Error processing device queue for {device_id}: {e}")
            raise HTTPException(status_code=500, detail="Internal server error")
    
    async def health_check(self) -> dict:
        """Health check endpoint.
        
        Returns:
            Health status
        """
        try:
            # Check Redis connectivity
            redis_status = await self.redis_service.ping()
            
            return {
                "status": "healthy" if redis_status else "unhealthy",
                "redis_status": "connected" if redis_status else "disconnected"
            }
            
        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return {
                "status": "unhealthy",
                "redis_status": "error",
                "error": str(e)
            } 