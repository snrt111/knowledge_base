# Docker 与 Kubernetes 实战指南

## 1. Docker 基础

### 1.1 Docker 简介

Docker 是一个开源的容器化平台，它允许开发者将应用及其依赖打包到一个轻量级、可移植的容器中，然后在任何环境中一致地运行。

### 1.2 核心概念

| 概念 | 说明 |
|------|------|
| 镜像 (Image) | 只读模板，包含运行应用所需的代码、库、环境变量和配置文件 |
| 容器 (Container) | 镜像的运行实例，可以被创建、启动、停止、删除 |
| 仓库 (Registry) | 存储和分发镜像的服务，如 Docker Hub |
| Dockerfile | 用于构建镜像的文本文件，包含一系列指令 |

### 1.3 常用命令

```bash
# 镜像操作
docker pull nginx:latest                    # 拉取镜像
docker images                               # 查看本地镜像
docker rmi nginx:latest                     # 删除镜像
docker build -t myapp:1.0 .                 # 构建镜像

# 容器操作
docker run -d -p 80:80 --name web nginx     # 运行容器
docker ps                                   # 查看运行中的容器
docker ps -a                                # 查看所有容器
docker stop web                             # 停止容器
docker start web                            # 启动容器
docker rm web                               # 删除容器
docker exec -it web bash                    # 进入容器

# 数据卷
docker volume create mydata                 # 创建数据卷
docker run -v mydata:/data nginx            # 挂载数据卷
docker run -v $(pwd):/app nginx             # 挂载主机目录
```

### 1.4 Dockerfile 编写

```dockerfile
# 基础镜像
FROM node:18-alpine

# 设置工作目录
WORKDIR /app

# 复制依赖文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

# 启动命令
CMD ["node", "server.js"]
```

### 1.5 多阶段构建

```dockerfile
# 构建阶段
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 1.6 Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  web:
    build: ./web
    ports:
      - "80:80"
    depends_on:
      - api
      - db
    networks:
      - frontend
      - backend

  api:
    build: ./api
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - DB_HOST=db
      - DB_PORT=5432
    depends_on:
      - db
      - redis
    networks:
      - backend
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '0.5'
          memory: 512M

  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - backend

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    networks:
      - backend

volumes:
  postgres_data:
  redis_data:

networks:
  frontend:
  backend:
```

## 2. Kubernetes 基础

### 2.1 K8s 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Master Node                         │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │  │
│  │  │ API Server  │  │   etcd      │  │  Scheduler  │   │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘   │  │
│  │  ┌─────────────┐                                      │  │
│  │  │ Controller  │                                      │  │
│  │  └─────────────┘                                      │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Worker Node 1                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │  │
│  │  │   kubelet   │  │  kube-proxy │  │  Container  │   │  │
│  │  │             │  │             │  │  Runtime    │   │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘   │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │  │
│  │  │    Pod 1    │  │    Pod 2    │  │    Pod 3    │   │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘   │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Worker Node 2                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │  │
│  │  │    Pod 4    │  │    Pod 5    │  │    Pod 6    │   │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘   │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心资源对象

| 资源 | 说明 |
|------|------|
| Pod | 最小的部署单元，包含一个或多个容器 |
| Deployment | 管理 Pod 的副本集，支持滚动更新和回滚 |
| Service | 为 Pod 提供稳定的网络访问入口 |
| ConfigMap | 存储配置数据 |
| Secret | 存储敏感数据 |
| PersistentVolume | 持久化存储 |
| Ingress | 集群入口，提供 HTTP/HTTPS 路由 |

### 2.3 Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
  labels:
    app: web
    tier: frontend
spec:
  containers:
    - name: nginx
      image: nginx:1.25
      ports:
        - containerPort: 80
      resources:
        requests:
          memory: "64Mi"
          cpu: "250m"
        limits:
          memory: "128Mi"
          cpu: "500m"
      livenessProbe:
        httpGet:
          path: /health
          port: 80
        initialDelaySeconds: 30
        periodSeconds: 10
      readinessProbe:
        httpGet:
          path: /ready
          port: 80
        initialDelaySeconds: 5
        periodSeconds: 5
      volumeMounts:
        - name: config
          mountPath: /etc/nginx/conf.d
        - name: data
          mountPath: /data
  volumes:
    - name: config
      configMap:
        name: nginx-config
    - name: data
      emptyDir: {}
```

### 2.4 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-deployment
  labels:
    app: web
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      labels:
        app: web
    spec:
      containers:
        - name: web
          image: myapp:1.0
          ports:
            - containerPort: 8080
          env:
            - name: NODE_ENV
              value: "production"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
```

### 2.5 Service

```yaml
# ClusterIP - 集群内部访问
apiVersion: v1
kind: Service
metadata:
  name: web-service
spec:
  type: ClusterIP
  selector:
    app: web
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP

---
# NodePort - 节点端口暴露
apiVersion: v1
kind: Service
metadata:
  name: web-nodeport
spec:
  type: NodePort
  selector:
    app: web
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080

---
# LoadBalancer - 云提供商负载均衡
apiVersion: v1
kind: Service
metadata:
  name: web-lb
spec:
  type: LoadBalancer
  selector:
    app: web
  ports:
    - port: 80
      targetPort: 8080
```

### 2.6 ConfigMap 和 Secret

```yaml
# ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  database.properties: |
    db.host=postgres
    db.port=5432
    db.name=myapp
  app.yml: |
    server:
      port: 8080
    logging:
      level: INFO

---
# Secret
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
data:
  # echo -n 'admin' | base64
  username: YWRtaW4=
  # echo -n 'password123' | base64
  password: cGFzc3dvcmQxMjM=
```

### 2.7 Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.example.com
        - www.example.com
      secretName: tls-secret
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-service
                port:
                  number: 80
    - host: www.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: web-service
                port:
                  number: 80
```

### 2.8 HorizontalPodAutoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: web-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: web-deployment
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
```

## 3. Kubectl 命令

```bash
# 集群信息
kubectl cluster-info
kubectl get nodes
kubectl describe node <node-name>

# Pod 操作
kubectl get pods
kubectl get pods -o wide
kubectl get pods --all-namespaces
kubectl describe pod <pod-name>
kubectl logs <pod-name>
kubectl logs <pod-name> -f
kubectl logs <pod-name> --previous
kubectl exec -it <pod-name> -- /bin/sh
kubectl port-forward <pod-name> 8080:80

# Deployment 操作
kubectl get deployments
kubectl rollout status deployment/<name>
kubectl rollout history deployment/<name>
kubectl rollout undo deployment/<name>
kubectl rollout undo deployment/<name> --to-revision=2
kubectl scale deployment <name> --replicas=5

# Service 操作
kubectl get services
kubectl get svc -o wide
kubectl describe svc <name>

# ConfigMap 和 Secret
kubectl get configmaps
kubectl get secrets
kubectl create configmap <name> --from-file=config.properties
kubectl create secret generic <name> --from-literal=password=secret

# 应用配置
kubectl apply -f deployment.yaml
kubectl apply -f .
kubectl delete -f deployment.yaml
kubectl diff -f deployment.yaml

# 命名空间
kubectl get namespaces
kubectl create namespace <name>
kubectl config set-context --current --namespace=<name>

# 资源使用
kubectl top node
kubectl top pod
```

## 4. Helm 包管理

### 4.1 Helm 基础

```bash
# 安装 Helm
brew install helm

# 添加仓库
helm repo add stable https://charts.helm.sh/stable
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# 搜索和安装 Chart
helm search repo nginx
helm install my-nginx bitnami/nginx

# 管理 Release
helm list
helm status my-nginx
helm upgrade my-nginx bitnami/nginx
helm rollback my-nginx 1
helm uninstall my-nginx
```

### 4.2 创建 Helm Chart

```bash
helm create mychart
```

Chart 结构：

```
mychart/
├── Chart.yaml          # Chart 元数据
├── values.yaml         # 默认配置值
├── charts/             # 依赖的 Chart
├── templates/          # K8s 模板文件
│   ├── _helpers.tpl    # 辅助模板
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   └── NOTES.txt       # 安装说明
└── README.md
```

Chart.yaml：

```yaml
apiVersion: v2
name: mychart
description: A Helm chart for Kubernetes
type: application
version: 0.1.0
appVersion: "1.0.0"
dependencies:
  - name: postgresql
    version: 12.x.x
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
```

values.yaml：

```yaml
replicaCount: 2

image:
  repository: myapp
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: nginx
  hosts:
    - host: app.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: app-tls
      hosts:
        - app.example.com

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

## 5. CI/CD 集成

### 5.1 GitLab CI

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - push
  - deploy

variables:
  DOCKER_IMAGE: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  KUBECONFIG: /etc/deploy/config

build:
  stage: build
  image: docker:20-dind
  services:
    - docker:20-dind
  script:
    - docker build -t $DOCKER_IMAGE .
    - docker tag $DOCKER_IMAGE $CI_REGISTRY_IMAGE:latest

test:
  stage: test
  image: node:18
  script:
    - npm ci
    - npm run test
    - npm run lint

push:
  stage: push
  image: docker:20-dind
  services:
    - docker:20-dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - docker push $DOCKER_IMAGE
    - docker push $CI_REGISTRY_IMAGE:latest
  only:
    - main

deploy:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context production
    - kubectl set image deployment/app app=$DOCKER_IMAGE
    - kubectl rollout status deployment/app
  only:
    - main
```

### 5.2 ArgoCD GitOps

```yaml
# application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/myorg/myapp.git
    targetRevision: HEAD
    path: k8s/overlays/production
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

---

本文档详细介绍了 Docker 和 Kubernetes 的核心概念、常用命令、资源配置以及 CI/CD 集成，是容器化部署和编排的完整实践指南。
