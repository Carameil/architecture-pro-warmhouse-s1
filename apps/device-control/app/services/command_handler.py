"""Command handler service for processing device commands."""

import logging
import httpx
from typing import Dict, Optional, Any
import uuid
from datetime import datetime

from ..models.device_state import (
    DeviceCommand, CommandStatus, CommandPriority, DeviceState
)
from .redis_service import RedisService

logger = logging.getLogger(__name__)


class CommandHandler:
    """Service for handling device commands."""
    
    def __init__(self, redis_service: RedisService, device_registry_url: str):
        """Initialize command handler.
        
        Args:
            redis_service: Redis service instance
            device_registry_url: URL of Device Registry API
        """
        self.redis_service = redis_service
        self.device_registry_url = device_registry_url.rstrip('/')
        self.http_client = httpx.AsyncClient(timeout=10.0)
        logger.info("Command handler initialized")
    
    async def validate_device(self, device_id: str) -> bool:
        """Validate device exists in Device Registry.
        
        Args:
            device_id: UUID of the device
            
        Returns:
            True if device exists, False otherwise
        """
        try:
            url = f"{self.device_registry_url}/api/v1/devices/{device_id}"
            response = await self.http_client.get(url)
            
            if response.status_code == 200:
                device_data = response.json()
                
                # Initialize device state if not exists
                state = await self.redis_service.get_device_state(device_id)
                if not state:
                    # Create initial state from registry data
                    state = DeviceState(
                        device_id=device_id,
                        house_id=device_data.get("house_id"),
                        location_id=device_data.get("location_id"),
                        status="offline",
                        attributes={}
                    )
                    await self.redis_service.set_device_state(state)
                
                return True
            
            logger.warning(f"Device not found in registry: {device_id}")
            return False
            
        except Exception as e:
            logger.error(f"Error validating device {device_id}: {e}")
            return False
    
    async def create_command(
        self,
        device_id: str,
        command_type: str,
        parameters: Dict[str, Any],
        priority: CommandPriority = CommandPriority.NORMAL,
        requested_by: Optional[str] = None
    ) -> Optional[DeviceCommand]:
        """Create and queue a new command.
        
        Args:
            device_id: Target device UUID
            command_type: Type of command
            parameters: Command parameters
            priority: Command priority
            requested_by: User ID who requested the command
            
        Returns:
            DeviceCommand if created successfully, None otherwise
        """
        try:
            # Validate device exists
            if not await self.validate_device(device_id):
                logger.error(f"Cannot create command for invalid device: {device_id}")
                return None
            
            # Check device state
            device_state = await self.redis_service.get_device_state(device_id)
            if device_state and device_state.status == "maintenance":
                logger.warning(f"Device {device_id} is in maintenance mode")
                return None
            
            # Create command
            command = DeviceCommand(
                command_id=str(uuid.uuid4()),
                device_id=device_id,
                command_type=command_type,
                parameters=parameters,
                priority=priority,
                status=CommandStatus.PENDING,
                requested_by=requested_by,
                created_at=datetime.utcnow()
            )
            
            # Queue command
            if await self.redis_service.create_command(command):
                logger.info(f"Command {command.command_id} created for device {device_id}")
                return command
            
            return None
            
        except Exception as e:
            logger.error(f"Error creating command for device {device_id}: {e}")
            return None
    
    async def execute_command(self, command_id: str) -> bool:
        """Execute a command (placeholder for actual device communication).
        
        Args:
            command_id: UUID of the command
            
        Returns:
            True if execution started successfully, False otherwise
        """
        try:
            # Get command
            command = await self.redis_service.get_command(command_id)
            if not command:
                logger.error(f"Command not found: {command_id}")
                return False
            
            # Check if command can be executed
            if command.status != CommandStatus.PENDING:
                logger.warning(f"Command {command_id} is not pending: {command.status}")
                return False
            
            # Update status to executing
            await self.redis_service.update_command_status(
                command_id,
                CommandStatus.EXECUTING
            )
            
            # Get device state
            device_state = await self.redis_service.get_device_state(command.device_id)
            if not device_state:
                await self.redis_service.update_command_status(
                    command_id,
                    CommandStatus.FAILED,
                    error_message="Device state not found"
                )
                return False
            
            # Check device is online
            if device_state.status != "online":
                await self.redis_service.update_command_status(
                    command_id,
                    CommandStatus.FAILED,
                    error_message=f"Device is {device_state.status}"
                )
                return False
            
            # Simulate command execution based on command type
            result = await self._simulate_command_execution(command, device_state)
            
            if result["success"]:
                # Update command status
                await self.redis_service.update_command_status(
                    command_id,
                    CommandStatus.COMPLETED,
                    result=result["data"]
                )
                
                # Update device state if needed
                if result.get("state_updates"):
                    await self.redis_service.update_device_state(
                        command.device_id,
                        result["state_updates"]
                    )
                
                return True
            else:
                # Command failed
                await self.redis_service.update_command_status(
                    command_id,
                    CommandStatus.FAILED,
                    error_message=result.get("error", "Command execution failed")
                )
                return False
                
        except Exception as e:
            logger.error(f"Error executing command {command_id}: {e}")
            
            # Update command status
            try:
                await self.redis_service.update_command_status(
                    command_id,
                    CommandStatus.FAILED,
                    error_message=str(e)
                )
            except:
                pass
                
            return False
    
    @staticmethod
    async def _simulate_command_execution(
            command: DeviceCommand,
        device_state: DeviceState
    ) -> Dict[str, Any]:
        """Simulate command execution for demo purposes.
        
        Args:
            command: Command to execute
            device_state: Current device state
            
        Returns:
            Execution result dictionary
        """
        try:
            # Simulate different command types
            if command.command_type == "turn_on":
                return {
                    "success": True,
                    "data": {"power": "on"},
                    "state_updates": {
                        "attributes": {**device_state.attributes, "power": "on"}
                    }
                }
                
            elif command.command_type == "turn_off":
                return {
                    "success": True,
                    "data": {"power": "off"},
                    "state_updates": {
                        "attributes": {**device_state.attributes, "power": "off"}
                    }
                }
                
            elif command.command_type == "set_temperature":
                temp = command.parameters.get("temperature", 20)
                return {
                    "success": True,
                    "data": {"temperature": temp},
                    "state_updates": {
                        "attributes": {**device_state.attributes, "temperature": temp}
                    }
                }
                
            elif command.command_type == "set_brightness":
                brightness = command.parameters.get("brightness", 50)
                return {
                    "success": True,
                    "data": {"brightness": brightness},
                    "state_updates": {
                        "attributes": {**device_state.attributes, "brightness": brightness}
                    }
                }
                
            elif command.command_type == "lock":
                return {
                    "success": True,
                    "data": {"locked": True},
                    "state_updates": {
                        "attributes": {**device_state.attributes, "locked": True}
                    }
                }
                
            elif command.command_type == "unlock":
                return {
                    "success": True,
                    "data": {"locked": False},
                    "state_updates": {
                        "attributes": {**device_state.attributes, "locked": False}
                    }
                }
                
            elif command.command_type == "ping":
                return {
                    "success": True,
                    "data": {"response": "pong", "timestamp": datetime.utcnow().isoformat()},
                    "state_updates": None
                }
                
            else:
                return {
                    "success": False,
                    "error": f"Unknown command type: {command.command_type}"
                }
                
        except Exception as e:
            logger.error(f"Error simulating command execution: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    async def process_device_queue(self, device_id: str) -> int:
        """Process all pending commands for a device.
        
        Args:
            device_id: UUID of the device
            
        Returns:
            Number of commands processed
        """
        try:
            processed = 0
            
            while True:
                # Get next command
                command = await self.redis_service.get_next_command(device_id)
                if not command:
                    break
                
                # Execute command
                if await self.execute_command(command.command_id):
                    processed += 1
                else:
                    # If command fails, check retry
                    if command.retry_count < command.max_retries:
                        command.retry_count += 1
                        command.status = CommandStatus.PENDING
                        await self.redis_service.create_command(command)
                        logger.info(f"Command {command.command_id} queued for retry ({command.retry_count}/{command.max_retries})")
                
            logger.info(f"Processed {processed} commands for device {device_id}")
            return processed
            
        except Exception as e:
            logger.error(f"Error processing device queue for {device_id}: {e}")
            return processed
    
    async def close(self):
        """Close HTTP client."""
        await self.http_client.aclose() 