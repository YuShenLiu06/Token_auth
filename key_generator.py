#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Token Auth Mod 密钥生成器
用于生成安全的共享密钥和随机挑战数据

@author nety.ys
"""

import base64
import secrets
import sys
from datetime import datetime


def generate_shared_secret(length_bytes=32):
    """
    生成指定长度的共享密钥
    
    Args:
        length_bytes: 密钥长度（字节）
    
    Returns:
        Base64编码的共享密钥字符串
    """
    if length_bytes <= 0:
        raise ValueError("密钥长度必须大于0")
    
    # 生成安全的随机字节
    secret_bytes = secrets.token_bytes(length_bytes)
    
    # 转换为Base64字符串
    secret_b64 = base64.b64encode(secret_bytes).decode('ascii')
    
    return secret_b64


def generate_challenge(length_bytes=16):
    """
    生成指定长度的随机挑战数据
    
    Args:
        length_bytes: 挑战数据长度（字节）
    
    Returns:
        Base64编码的挑战数据字符串
    """
    if length_bytes <= 0:
        raise ValueError("挑战数据长度必须大于0")
    
    # 生成安全的随机字节
    challenge_bytes = secrets.token_bytes(length_bytes)
    
    # 转换为Base64字符串
    challenge_b64 = base64.b64encode(challenge_bytes).decode('ascii')
    
    return challenge_b64


def validate_secret(secret):
    """
    验证密钥格式是否有效（Base64编码）
    
    Args:
        secret: Base64编码的密钥字符串
    
    Returns:
        如果密钥格式有效则返回True
    """
    if not secret:
        return False
    
    try:
        decoded = base64.b64decode(secret)
        return len(decoded) > 0
    except Exception:
        return False


def main():
    """主函数"""
    print("=== Token Auth Mod 密钥生成工具 ===")
    print(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    try:
        # 生成默认长度的共享密钥
        shared_secret = generate_shared_secret()
        print(f"生成的共享密钥: {shared_secret}")
        print(f"密钥长度: 256 位 (32 字节)")
        
        # 验证密钥
        is_valid = validate_secret(shared_secret)
        print(f"密钥有效性: {'有效' if is_valid else '无效'}")
        
        # 解码并显示密钥的字节表示
        if is_valid:
            decoded = base64.b64decode(shared_secret)
            print(f"解码后的字节长度: {len(decoded)} 字节")
        
        print()
        print("请将此密钥同时配置到服务器和客户端配置文件中")
        print("服务器配置文件: config/token-auth/token-auth-server.properties")
        print("客户端配置文件: config/token-auth/token-auth-client.properties")
        print()
        
        # 生成示例挑战数据
        challenge = generate_challenge()
        print(f"示例挑战数据: {challenge}")
        print(f"挑战数据长度: 16 字节")
        
        print()
        print("=== 使用说明 ===")
        print("1. 将生成的共享密钥复制到服务器和客户端配置文件中")
        print("2. 重启Minecraft服务器以加载新配置")
        print("3. 客户端连接服务器时将自动进行令牌认证")
        print()
        print("=== 高级用法 ===")
        print("生成自定义长度的密钥:")
        print("  python key_generator.py --secret-length 64  # 生成64字节(512位)的密钥")
        print("生成自定义长度的挑战数据:")
        print("  python key_generator.py --challenge-length 32  # 生成32字节的挑战数据")
        
    except Exception as e:
        print(f"错误: {e}")
        sys.exit(1)


def parse_args():
    """解析命令行参数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Token Auth Mod 密钥生成器')
    parser.add_argument('--secret-length', type=int, default=32,
                        help='共享密钥长度（字节，默认32）')
    parser.add_argument('--challenge-length', type=int, default=16,
                        help='挑战数据长度（字节，默认16）')
    
    return parser.parse_args()


if __name__ == "__main__":
    # 如果有命令行参数，则使用高级模式
    if len(sys.argv) > 1:
        args = parse_args()
        
        if '--secret-length' in sys.argv or '--challenge-length' in sys.argv:
            # 高级模式
            if '--secret-length' in sys.argv:
                secret = generate_shared_secret(args.secret_length)
                print(f"生成的共享密钥: {secret}")
                print(f"密钥长度: {args.secret_length * 8} 位 ({args.secret_length} 字节)")
            
            if '--challenge-length' in sys.argv:
                challenge = generate_challenge(args.challenge_length)
                print(f"生成的挑战数据: {challenge}")
                print(f"挑战数据长度: {args.challenge_length} 字节")
    else:
        # 默认模式
        main()