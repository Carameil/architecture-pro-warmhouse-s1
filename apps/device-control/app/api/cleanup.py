"""Cleanup API endpoints for device-control service."""

from fastapi import APIRouter, HTTPException, Depends
from typing import Dict, Any
import logging

from ..services.redis_service import RedisService
from ..core.dependencies import get_redis_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/cleanup", tags=["cleanup"])


@router.delete("/device/{device_id}")
async def cleanup_device_data(
    device_id: str,
    redis_service: RedisService = Depends(get_redis_service)
) -> Dict[str, Any]:
    """Clean up all data for a specific device.
    
    This endpoint is used for cascading deletion when a device is removed
    from the device registry.
    
    Args:
        device_id: UUID of the device to clean up
        redis_service: Redis service dependency
        
    Returns:
        Cleanup result with status and message
    """
    try:
        logger.info(f"Manual cleanup requested for device: {device_id}")
        
        success = await redis_service.cleanup_device_data(device_id)
        
        if success:
            return {
                "status": "success",
                "message": f"Successfully cleaned up data for device {device_id}",
                "device_id": device_id
            }
        else:
            raise HTTPException(
                status_code=500,
                detail=f"Failed to clean up data for device {device_id}"
            )
            
    except Exception as e:
        logger.error(f"Error in cleanup endpoint for device {device_id}: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error during cleanup: {str(e)}"
        )


@router.post("/expired-commands")
async def cleanup_expired_commands(
    redis_service: RedisService = Depends(get_redis_service)
) -> Dict[str, Any]:
    """Clean up expired commands from all device queues.
    
    Returns:
        Number of commands cleaned up
    """
    try:
        logger.info("Manual cleanup of expired commands requested")
        
        cleaned_count = await redis_service.cleanup_expired_commands()
        
        return {
            "status": "success",
            "message": f"Cleaned up {cleaned_count} expired commands",
            "cleaned_count": cleaned_count
        }
        
    except Exception as e:
        logger.error(f"Error in expired commands cleanup: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error during cleanup: {str(e)}"
        ) 