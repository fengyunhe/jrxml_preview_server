#!/bin/bash
set -e

IMAGE_NAME="krccr.ccs.tencentyun.com/firegod/jrxml_preview_server"
IMAGE_TAG="latest"

echo "🔐 请先登录到腾讯云容器镜像服务"
echo "   docker login krccr.ccs.tencentyun.com"
echo ""
read -p "已登录完成？(y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "请先完成登录后再次运行此脚本"
    exit 1
fi

echo ""
echo "🔨 步骤 1/3: 构建多平台 Docker 镜像"

# 检查是否已有 buildx builder
if docker buildx ls 2>&1 | grep -q "linux/amd64"; then
  echo "   使用已有 builder"
else
  echo "   创建新的 builder"
  docker buildx create --name multiplatform --use
fi

# 构建多平台镜像
echo "   构建 linux/amd64 和 linux/arm64"
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ${IMAGE_NAME}:${IMAGE_TAG} \
  --push .

echo ""
echo "📤 步骤 2/3: 镜像已在构建时推送完成"

echo ""
echo "✅ 步骤 3/3: 验证"
docker buildx imagetools inspect ${IMAGE_NAME}:${IMAGE_TAG}

echo ""
echo "🎉 完成！镜像已推送至："
echo "   ${IMAGE_NAME}:${IMAGE_TAG}"
echo "   支持平台：linux/amd64, linux/arm64"
echo ""
echo "📋 使用方式："
echo "   docker pull --platform linux/amd64 ${IMAGE_NAME}:${IMAGE_TAG}"
echo "   docker run -d --name jrxml_preview_server -p 8084:8084 ${IMAGE_NAME}:${IMAGE_TAG}"
