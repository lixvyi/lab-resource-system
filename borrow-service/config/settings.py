"""环境配置(统一从环境变量读取,支持 .env 文件)"""

import os
from pathlib import Path

try:
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).resolve().parent.parent / ".env")
except ImportError:  # python-dotenv 未安装时降级为纯环境变量
    pass


class Settings:
    """借用记录服务配置"""

    SERVICE_NAME = "borrow-service"
    SERVICE_VERSION = "1.0.0"

    HOST = os.getenv("HOST", "0.0.0.0")
    PORT = int(os.getenv("PORT", "8081"))
    DEBUG = os.getenv("DEBUG", "false").lower() == "true"

    # MySQL 连接配置
    DB_HOST = os.getenv("DB_HOST", "127.0.0.1")
    DB_PORT = int(os.getenv("DB_PORT", "3306"))
    DB_USER = os.getenv("DB_USER", "root")
    DB_PASSWORD = os.getenv("DB_PASSWORD", "lab123456")
    DB_NAME = os.getenv("DB_NAME", "lab_resource_system")
    DB_POOL_SIZE = int(os.getenv("DB_POOL_SIZE", "10"))
    DB_CHARSET = "utf8mb4"

    # 下游服务
    GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8080")
    MATERIAL_SERVICE_URL = os.getenv("MATERIAL_SERVICE_URL", "http://localhost:8082")
