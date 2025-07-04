"""Event listener for device events in device-control service."""

import asyncio
import json
import logging
import os
from typing import Optional

import aio_pika
from aio_pika.abc import AbstractIncomingMessage

from ..services.redis_service import RedisService

# Configure logger
logger = logging.getLogger(__name__)

# RabbitMQ connection settings from environment variables
RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", 5672))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "admin")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "admin123")
RABBITMQ_URL = f"amqp://{RABBITMQ_USER}:{RABBITMQ_PASSWORD}@{RABBITMQ_HOST}:{RABBITMQ_PORT}/"

# Exchange and queue details
EXCHANGE_NAME = "events.device"
QUEUE_NAME = "device_control_cleanup_queue"
ROUTING_KEY = "device.deleted"


class DeviceEventListener:
    """Listens for device events from RabbitMQ and triggers cleanup."""

    def __init__(self, redis_service: RedisService):
        self.redis_service = redis_service
        self.connection: Optional[aio_pika.RobustConnection] = None
        self.channel: Optional[aio_pika.Channel] = None
        self._is_running = False

    async def _process_message(self, message: AbstractIncomingMessage):
        """Callback to process a message from the queue."""
        async with message.process():
            try:
                body = json.loads(message.body.decode())
                device_id = body.get("device_id")

                if not device_id:
                    logger.warning("Received message without a device_id")
                    return

                logger.info(f"Received device.deleted event for device_id: {device_id}")
                
                # Perform the cleanup using the centralized service
                success = await self.redis_service.cleanup_device_data(device_id)
                
                if success:
                    logger.info(f"Successfully cleaned up data for device: {device_id}")
                else:
                    logger.error(f"Failed to clean up data for device: {device_id}")

            except json.JSONDecodeError:
                logger.error("Failed to decode message body as JSON")
            except Exception as e:
                logger.error(f"An unexpected error occurred while processing message: {e}")

    async def start(self):
        """Establish connection to RabbitMQ and start consuming messages."""
        logger.info("Starting device event listener...")
        loop = asyncio.get_event_loop()
        
        try:
            # Create a robust connection that handles reconnects
            self.connection = await aio_pika.connect_robust(
                RABBITMQ_URL, 
                loop=loop,
                client_properties={"connection_name": "device-control-listener"}
            )
            logger.info("Successfully connected to RabbitMQ")

            self.channel = await self.connection.channel()
            
            if not self.channel:
                logger.error("Failed to create RabbitMQ channel.")
                await self.stop()
                return
                
            await self.channel.set_qos(prefetch_count=10) # Process up to 10 messages concurrently

            # Declare a topic exchange (durable)
            exchange = await self.channel.declare_exchange(
                EXCHANGE_NAME, aio_pika.ExchangeType.TOPIC, durable=True
            )

            # Declare a durable queue
            queue = await self.channel.declare_queue(QUEUE_NAME, durable=True)

            # Bind the queue to the exchange with the specific routing key
            await queue.bind(exchange, ROUTING_KEY)

            await queue.consume(self._process_message)
            self._is_running = True
            logger.info(f"Listener started. Waiting for '{ROUTING_KEY}' messages on queue '{QUEUE_NAME}'...")

        except aio_pika.exceptions.AMQPConnectionError as e:
            logger.error(f"Failed to connect to RabbitMQ: {e}. Retrying in the background...")
        except Exception as e:
            logger.error(f"An unexpected error occurred during listener startup: {e}")


    async def stop(self):
        """Gracefully stop the listener and close connections."""
        if not self._is_running:
            return
            
        logger.info("Stopping device event listener...")
        try:
            if self.channel and not self.channel.is_closed:
                await self.channel.close()
                logger.info("RabbitMQ channel closed.")
            if self.connection and not self.connection.is_closed:
                await self.connection.close()
                logger.info("RabbitMQ connection closed.")
        except Exception as e:
            logger.error(f"Error during listener shutdown: {e}")
        finally:
            self._is_running = False
            logger.info("Device event listener stopped.") 