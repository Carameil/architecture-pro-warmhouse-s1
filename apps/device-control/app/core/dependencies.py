"""Dependency injection for device-control service."""

from typing import Optional
from ..services.redis_service import RedisService

# Global reference to redis service instance
_redis_service: Optional[RedisService] = None


def set_redis_service(redis_service: RedisService) -> None:
    """Set the global Redis service instance."""
    global _redis_service
    _redis_service = redis_service


def get_redis_service() -> RedisService:
    """Get the Redis service instance for dependency injection."""
    if _redis_service is None:
        raise RuntimeError("Redis service not initialized")
    return _redis_service 