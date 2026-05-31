# JRXML Preview Server - Docker 构建指南

## 快速开始

### 使用构建脚本

```bash
# 构建 Docker 镜像
./build-docker.sh

# 运行容器
docker run -d -p 8084:8084 --name jrxml_preview_server jrxml_preview_server:latest
```

### 使用 Docker Compose

```bash
# 构建并启动
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止
docker-compose down
```

## 访问应用

构建完成后，应用将运行在：
- **http://localhost:8084**

## Docker 镜像特点

✅ **多阶段构建**
- 使用 Maven 构建阶段生成可执行 JAR
- 运行阶段使用精简 JRE 镜像，减小镜像大小

✅ **优化配置**
- Java 8 运行时（与项目编译目标一致）
- 安装字体支持（JasperReports 报表渲染需要）
- 设置时区和 JVM 参数
- 无头模式运行（-Djava.awt.headless=true）

✅ **生产就绪**
- 自动重启策略
- 可配置环境变量
- 优雅的启动方式

## 配置选项

在 `docker-compose.yml` 中修改以下环境变量：

```yaml
environment:
  - TZ=Asia/Shanghai                    # 时区设置
  - JAVA_OPTS=-Djava.awt.headless=true -Duser.timezone=Asia/Shanghai
```

## 自定义构建

如果需要修改 Maven 构建参数，可以在 Dockerfile 的构建阶段添加：

```dockerfile
RUN mvn package -DskipTests -Dmaven.test.skip=true
```

## 故障排查

**问题：容器无法启动**
- 检查日志：`docker logs jrxml_preview_server`
- 确认端口 8084 未被占用

**问题：字体渲染问题**
- 已在 Dockerfile 中安装 fontconfig
- 如需额外字体，可挂载字体目录：
  ```
  docker run -v /path/to/fonts:/usr/share/fonts ...
  ```

**问题：构建失败**
- 确保 Docker 有足够权限
- 使用 `sudo ./build-docker.sh` 或添加用户到 docker 组
