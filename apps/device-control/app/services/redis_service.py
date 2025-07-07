"""Redis service for device state and command management."""

import json
import logging
from typing import Dict, List, Optional, Set
from datetime import datetime, timedelta
import redis
from redis import Redis
import uuid

from ..models.device_state import (
    DeviceState, DeviceCommand, CommandStatus, CommandPriority
)

logger = logging.getLogger(__name__)


class RedisService:
    """Service for managing device states and commands in Redis."""
    
    # Redis key prefixes
    DEVICE_STATE_PREFIX = "device:state:"
    DEVICE_COMMAND_PREFIX = "device:command:"
    COMMAND_QUEUE_PREFIX = "device:queue:"
    DEVICE_SET_KEY = "devices:all"
    ONLINE_DEVICES_KEY = "devices:online"
    
    # TTL settings
    COMMAND_TTL = 3600  # 1 hour
    STATE_TTL = 86400   # 24 hours
    
    def __init__(self, redis_client: Redis):
        """Initialize Redis service.
        
        Args:
            redis_client: Redis client instance
        """
        self.redis = redis_client
        logger.info("Redis service initialized")
    
    # Device State Management
    
    async def get_device_state(self, device_id: str) -> Optional[DeviceState]:
        """Get device state from Redis.
        
        Args:
            device_id: UUID of the device
            
        Returns:
            DeviceState if found, None otherwise
        """
        try:
            key = f"{self.DEVICE_STATE_PREFIX}{device_id}"
            data = self.redis.hgetall(key)
            
            if not data:
                logger.warning(f"Device state not found: {device_id}")
                return None
            
            # Convert bytes to strings
            string_data = {k.decode(): v.decode() for k, v in data.items()}
            return DeviceState.from_redis_dict(string_data)
            
        except Exception as e:
            logger.error(f"Error getting device state {device_id}: {e}")
            return None
    
    async def set_device_state(self, state: DeviceState) -> bool:
        """Set device state in Redis.
        
        Args:
            state: DeviceState object
            
        Returns:
            True if successful, False otherwise
        """
        try:
            key = f"{self.DEVICE_STATE_PREFIX}{state.device_id}"
            data = state.to_redis_dict()
            
            # Use pipeline for atomic operations
            pipe = self.redis.pipeline()
            pipe.hset(key, mapping=data)
            pipe.expire(key, self.STATE_TTL)
            pipe.sadd(self.DEVICE_SET_KEY, state.device_id)
            
            # Update online devices set if device is online
            if state.status == "online":
                pipe.sadd(self.ONLINE_DEVICES_KEY, state.device_id)
            else:
                pipe.srem(self.ONLINE_DEVICES_KEY, state.device_id)
            
            pipe.execute()
            
            logger.info(f"Device state updated: {state.device_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error setting device state {state.device_id}: {e}")
            return False
    
    async def update_device_state(self, device_id: str, updates: Dict[str, any]) -> Optional[DeviceState]:
        """Update specific fields in device state.
        
        Args:
            device_id: UUID of the device
            updates: Dictionary of fields to update
            
        Returns:
            Updated DeviceState if successful, None otherwise
        """
        try:
            # Get current state
            current_state = await self.get_device_state(device_id)
            if not current_state:
                return None
            
            # Apply updates
            state_dict = current_state.model_dump()
            state_dict.update(updates)
            state_dict['last_seen'] = datetime.utcnow()
            
            # Create updated state
            updated_state = DeviceState(**state_dict)
            
            # Save updated state
            if await self.set_device_state(updated_state):
                return updated_state
            
            return None
            
        except Exception as e:
            logger.error(f"Error updating device state {device_id}: {e}")
            return None
    
    async def get_all_devices(self) -> List[str]:
        """Get all device IDs.
        
        Returns:
            List of device IDs
        """
        try:
            device_ids = self.redis.smembers(self.DEVICE_SET_KEY)
            return [device_id.decode() for device_id in device_ids]
        except Exception as e:
            logger.error(f"Error getting all devices: {e}")
            return []
    
    async def get_online_devices(self) -> List[str]:
        """Get all online device IDs.
        
        Returns:
            List of online device IDs
        """
        try:
            device_ids = self.redis.smembers(self.ONLINE_DEVICES_KEY)
            return [device_id.decode() for device_id in device_ids]
        except Exception as e:
            logger.error(f"Error getting online devices: {e}")
            return []
    
    # Command Management
    
    async def create_command(self, command: DeviceCommand) -> bool:
        """Create a new command and add to queue.
        
        Args:
            command: DeviceCommand object
            
        Returns:
            True if successful, False otherwise
        """
        try:
            # Store command details
            command_key = f"{self.DEVICE_COMMAND_PREFIX}{command.command_id}"
            command_data = command.to_redis_dict()
            
            # Calculate priority score (higher priority = lower score)
            priority_scores = {
                CommandPriority.CRITICAL: 0,
                CommandPriority.HIGH: 1,
                CommandPriority.NORMAL: 2,
                CommandPriority.LOW: 3
            }
            score = priority_scores.get(command.priority, 2)
            
            # Add timestamp to ensure FIFO within same priority
            score = score + (command.created_at.timestamp() / 1e10)
            
            # Queue key for the device
            queue_key = f"{self.COMMAND_QUEUE_PREFIX}{command.device_id}"
            
            # Use pipeline for atomic operations
            pipe = self.redis.pipeline()
            pipe.hset(command_key, mapping=command_data)
            pipe.expire(command_key, self.COMMAND_TTL)
            pipe.zadd(queue_key, {command.command_id: score})
            pipe.execute()
            
            logger.info(f"Command created: {command.command_id} for device {command.device_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error creating command {command.command_id}: {e}")
            return False
    
    async def get_command(self, command_id: str) -> Optional[DeviceCommand]:
        """Get command by ID.
        
        Args:
            command_id: UUID of the command
            
        Returns:
            DeviceCommand if found, None otherwise
        """
        try:
            command_key = f"{self.DEVICE_COMMAND_PREFIX}{command_id}"
            data = self.redis.hgetall(command_key)
            
            if not data:
                return None
            
            # Convert bytes to strings
            string_data = {k.decode(): v.decode() for k, v in data.items()}
            return DeviceCommand.from_redis_dict(string_data)
            
        except Exception as e:
            logger.error(f"Error getting command {command_id}: {e}")
            return None
    
    async def update_command_status(
        self, 
        command_id: str, 
        status: CommandStatus,
        result: Optional[Dict[str, any]] = None,
        error_message: Optional[str] = None
    ) -> bool:
        """Update command status.
        
        Args:
            command_id: UUID of the command
            status: New command status
            result: Optional execution result
            error_message: Optional error message
            
        Returns:
            True if successful, False otherwise
        """
        try:
            command = await self.get_command(command_id)
            if not command:
                return False
            
            # Update command fields
            command.status = status
            if result:
                command.result = result
            if error_message:
                command.error_message = error_message
            
            # Update timestamps
            if status == CommandStatus.EXECUTING:
                command.started_at = datetime.utcnow()
            elif status in [CommandStatus.COMPLETED, CommandStatus.FAILED, CommandStatus.CANCELLED]:
                command.completed_at = datetime.utcnow()
            
            # Save updated command
            command_key = f"{self.DEVICE_COMMAND_PREFIX}{command_id}"
            command_data = command.to_redis_dict()
            
            # Update in Redis
            pipe = self.redis.pipeline()
            pipe.hset(command_key, mapping=command_data)
            
            # Remove from queue if completed/failed/cancelled
            if status in [CommandStatus.COMPLETED, CommandStatus.FAILED, CommandStatus.CANCELLED]:
                queue_key = f"{self.COMMAND_QUEUE_PREFIX}{command.device_id}"
                pipe.zrem(queue_key, command_id)
            
            pipe.execute()
            
            logger.info(f"Command {command_id} status updated to {status}")
            return True
            
        except Exception as e:
            logger.error(f"Error updating command status {command_id}: {e}")
            return False
    
    async def get_next_command(self, device_id: str) -> Optional[DeviceCommand]:
        """Get next command from device queue.
        
        Args:
            device_id: UUID of the device
            
        Returns:
            Next DeviceCommand if available, None otherwise
        """
        try:
            queue_key = f"{self.COMMAND_QUEUE_PREFIX}{device_id}"
            
            # Get command with highest priority (lowest score)
            command_ids = self.redis.zrange(queue_key, 0, 0)
            
            if not command_ids:
                return None
            
            command_id = command_ids[0].decode()
            command = await self.get_command(command_id)
            
            if command and command.status == CommandStatus.PENDING:
                return command
            
            # If command is not pending, remove from queue
            if command:
                self.redis.zrem(queue_key, command_id)
            
            # Try next command
            return await self.get_next_command(device_id)
            
        except Exception as e:
            logger.error(f"Error getting next command for device {device_id}: {e}")
            return None
    
    async def get_device_commands(
        self, 
        device_id: str, 
        status: Optional[CommandStatus] = None,
        limit: int = 10
    ) -> List[DeviceCommand]:
        """Get commands for a device.
        
        Args:
            device_id: UUID of the device
            status: Optional filter by status
            limit: Maximum number of commands to return
            
        Returns:
            List of DeviceCommand objects
        """
        try:
            # Get command IDs from queue
            queue_key = f"{self.COMMAND_QUEUE_PREFIX}{device_id}"
            command_ids = self.redis.zrange(queue_key, 0, limit - 1)
            
            commands = []
            for command_id in command_ids:
                command = await self.get_command(command_id.decode())
                if command:
                    if status is None or command.status == status:
                        commands.append(command)
            
            return commands
            
        except Exception as e:
            logger.error(f"Error getting device commands for {device_id}: {e}")
            return []
    
    async def cancel_command(self, command_id: str) -> bool:
        """Cancel a pending command.
        
        Args:
            command_id: UUID of the command
            
        Returns:
            True if successful, False otherwise
        """
        try:
            command = await self.get_command(command_id)
            if not command:
                return False
            
            if command.status != CommandStatus.PENDING:
                logger.warning(f"Cannot cancel command {command_id} with status {command.status}")
                return False
            
            return await self.update_command_status(
                command_id, 
                CommandStatus.CANCELLED,
                error_message="Command cancelled by user"
            )
            
        except Exception as e:
            logger.error(f"Error cancelling command {command_id}: {e}")
            return False
    
    # Utility methods
    
    async def ping(self) -> bool:
        """Check Redis connectivity.
        
        Returns:
            True if Redis is accessible, False otherwise
        """
        try:
            return self.redis.ping()
        except Exception as e:
            logger.error(f"Redis ping failed: {e}")
            return False
    
    async def cleanup_expired_commands(self) -> int:
        """Clean up expired commands from queues.
        
        Returns:
            Number of commands cleaned up
        """
        try:
            cleaned = 0
            
            # Get all device IDs
            device_ids = await self.get_all_devices()
            
            for device_id in device_ids:
                queue_key = f"{self.COMMAND_QUEUE_PREFIX}{device_id}"
                command_ids = self.redis.zrange(queue_key, 0, -1)
                
                for command_id in command_ids:
                    command_key = f"{self.DEVICE_COMMAND_PREFIX}{command_id.decode()}"
                    
                    # Check if command still exists
                    if not self.redis.exists(command_key):
                        self.redis.zrem(queue_key, command_id)
                        cleaned += 1
            
            logger.info(f"Cleaned up {cleaned} expired commands")
            return cleaned
            
        except Exception as e:
            logger.error(f"Error cleaning up expired commands: {e}")
            return 0
            
    async def cleanup_device_data(self, device_id: str) -> bool:
        """Clean up all data for a specific device.
        
        Args:
            device_id: UUID of the device to clean up
            
        Returns:
            True if successful, False otherwise
        """
        try:
            queue_key = f"{self.COMMAND_QUEUE_PREFIX}{device_id}"
            
            # First, get all command IDs from the queue for this device
            command_ids_bytes = self.redis.zrange(queue_key, 0, -1)
            command_ids = [cmd_id.decode() for cmd_id in command_ids_bytes]

            # Now, create a pipeline to delete everything atomically
            pipe = self.redis.pipeline()

            # 1. Delete all individual command keys
            if command_ids:
                command_keys = [f"{self.DEVICE_COMMAND_PREFIX}{cmd_id}" for cmd_id in command_ids]
                pipe.delete(*command_keys)
                logger.debug(f"Queued deletion for {len(command_keys)} command keys")

            # 2. Delete the command queue itself
            pipe.delete(queue_key)
            logger.debug(f"Queued deletion for command queue: {queue_key}")
            
            # 3. Delete device state
            state_key = f"{self.DEVICE_STATE_PREFIX}{device_id}"
            pipe.delete(state_key)
            logger.debug(f"Queued deletion for state key: {state_key}")

            # 4. Remove from global device sets
            pipe.srem(self.DEVICE_SET_KEY, device_id)
            pipe.srem(self.ONLINE_DEVICES_KEY, device_id)
            logger.debug(f"Queued removal of {device_id} from global sets")

            # Execute all commands in the pipeline
            pipe.execute()
            
            logger.info(f"Successfully cleaned up all data for device: {device_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error cleaning up data for device {device_id}: {e}")
            return False 