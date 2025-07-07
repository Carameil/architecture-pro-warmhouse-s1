"""Device state models for Device Control Service."""

from datetime import datetime
from typing import Any, Dict, List, Optional
from enum import Enum
from pydantic import BaseModel, Field, ConfigDict


class DeviceStatus(str, Enum):
    """Device operational status."""
    ONLINE = "online"
    OFFLINE = "offline"
    ERROR = "error"
    MAINTENANCE = "maintenance"


class CommandStatus(str, Enum):
    """Command execution status."""
    PENDING = "pending"
    EXECUTING = "executing"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


class CommandPriority(str, Enum):
    """Command priority levels."""
    LOW = "low"
    NORMAL = "normal"
    HIGH = "high"
    CRITICAL = "critical"


class DeviceState(BaseModel):
    """Device state stored in Redis."""
    model_config = ConfigDict(use_enum_values=True)
    
    device_id: str = Field(..., description="UUID of the device")
    status: DeviceStatus = Field(DeviceStatus.OFFLINE, description="Current device status")
    attributes: Dict[str, Any] = Field(default_factory=dict, description="Device-specific attributes")
    last_seen: datetime = Field(default_factory=datetime.utcnow, description="Last activity timestamp")
    last_command_id: Optional[str] = Field(None, description="ID of last executed command")
    error_message: Optional[str] = Field(None, description="Error message if status is ERROR")
    firmware_version: Optional[str] = Field(None, description="Device firmware version")
    
    # Device location info (cached from Device Registry)
    house_id: Optional[str] = Field(None, description="House ID from Device Registry")
    location_id: Optional[str] = Field(None, description="Location ID from Device Registry")
    
    def to_redis_dict(self) -> Dict[str, str]:
        """Convert to Redis-compatible dictionary."""
        data = self.model_dump(mode='json')
        # Convert datetime to ISO format
        data['last_seen'] = self.last_seen.isoformat()
        # Convert nested dict to JSON string
        import json
        data['attributes'] = json.dumps(data['attributes'])
        # Remove None values
        return {k: str(v) for k, v in data.items() if v is not None}
    
    @classmethod
    def from_redis_dict(cls, data: Dict[str, str]) -> 'DeviceState':
        """Create instance from Redis data."""
        import json
        if 'attributes' in data:
            data['attributes'] = json.loads(data['attributes'])
        if 'last_seen' in data:
            data['last_seen'] = datetime.fromisoformat(data['last_seen'])
        return cls(**data)


class DeviceCommand(BaseModel):
    """Command to be executed on a device."""
    model_config = ConfigDict(use_enum_values=True)
    
    command_id: str = Field(..., description="UUID of the command")
    device_id: str = Field(..., description="Target device UUID")
    command_type: str = Field(..., description="Type of command (e.g., 'turn_on', 'set_temperature')")
    parameters: Dict[str, Any] = Field(default_factory=dict, description="Command parameters")
    priority: CommandPriority = Field(CommandPriority.NORMAL, description="Command priority")
    status: CommandStatus = Field(CommandStatus.PENDING, description="Command execution status")
    
    # Execution details
    created_at: datetime = Field(default_factory=datetime.utcnow)
    started_at: Optional[datetime] = Field(None, description="When execution started")
    completed_at: Optional[datetime] = Field(None, description="When execution completed")
    
    # User context
    requested_by: Optional[str] = Field(None, description="User ID who requested the command")
    
    # Execution results
    result: Optional[Dict[str, Any]] = Field(None, description="Command execution result")
    error_message: Optional[str] = Field(None, description="Error message if failed")
    retry_count: int = Field(0, description="Number of retry attempts")
    max_retries: int = Field(3, description="Maximum retry attempts")
    
    def to_redis_dict(self) -> Dict[str, str]:
        """Convert to Redis-compatible dictionary."""
        data = self.model_dump(mode='json')
        # Convert datetimes to ISO format
        for field in ['created_at', 'started_at', 'completed_at']:
            if data.get(field):
                data[field] = data[field] if isinstance(data[field], str) else datetime.fromisoformat(str(data[field])).isoformat()
        # Convert nested dicts to JSON strings
        import json
        for field in ['parameters', 'result']:
            if data.get(field):
                data[field] = json.dumps(data[field])
        # Remove None values
        return {k: str(v) for k, v in data.items() if v is not None}
    
    @classmethod
    def from_redis_dict(cls, data: Dict[str, str]) -> 'DeviceCommand':
        """Create instance from Redis data."""
        import json
        # Parse JSON fields
        for field in ['parameters', 'result']:
            if field in data and data[field]:
                data[field] = json.loads(data[field])
        # Parse datetime fields
        for field in ['created_at', 'started_at', 'completed_at']:
            if field in data and data[field]:
                data[field] = datetime.fromisoformat(data[field])
        # Parse integer fields
        for field in ['retry_count', 'max_retries']:
            if field in data:
                data[field] = int(data[field])
        return cls(**data)


# Request/Response models for API

class DeviceStateUpdate(BaseModel):
    """Request model for updating device state."""
    status: Optional[DeviceStatus] = None
    attributes: Optional[Dict[str, Any]] = None
    firmware_version: Optional[str] = None
    error_message: Optional[str] = None


class CommandRequest(BaseModel):
    """Request model for sending command to device."""
    command_type: str = Field(..., description="Type of command")
    parameters: Dict[str, Any] = Field(default_factory=dict, description="Command parameters")
    priority: CommandPriority = Field(CommandPriority.NORMAL, description="Command priority")
    requested_by: Optional[str] = Field(None, description="User ID requesting the command")


class CommandResponse(BaseModel):
    """Response model for command submission."""
    command_id: str
    device_id: str
    command_type: str
    status: CommandStatus
    created_at: datetime
    message: str = Field(..., description="Human-readable status message")


class DeviceStateResponse(BaseModel):
    """Response model for device state."""
    device_id: str
    status: DeviceStatus
    attributes: Dict[str, Any]
    last_seen: datetime
    last_command_id: Optional[str] = None
    firmware_version: Optional[str] = None
    house_id: Optional[str] = None
    location_id: Optional[str] = None 