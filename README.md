# FlowPay - Sistema de Distribuição de Atendimentos

Sistema de gerenciamento e distribuição automática de atendimentos FlowPay.

## Sobre o Projeto

A FlowPay é uma fintech que estruturou sua central de relacionamento em 3 times especializados:
- **Time Cartões**: Problemas com cartão
- **Time Empréstimos**: Contratação de empréstimo
- **Time Outros Assuntos**: Demais solicitações

### Regras de Negócio

- ✅ Cada atendente pode atender no máximo **3 pessoas simultaneamente**
- ✅ Quando todos os atendentes estão ocupados, os atendimentos são **enfileirados**
- ✅ Ao finalizar um atendimento, o próximo da fila é **distribuído automaticamente**
- ✅ Notificações em **tempo real**

## Arquitetura

### Stack

**Backend:**
- Java 21
- Spring Boot 4.0.2
- Spring WebSocket (STOMP)
- Redis (opcional - via Docker)
- Maven

**Documentação:**
- SpringDoc OpenAPI (Swagger)

**Infraestrutura:**
- Docker & Docker Compose

### Arquitetura

O sistema implementa **múltiplas estratégias de armazenamento** usando Design Patterns:

```
Interfaces (Contratos)
    ├── FilaService
    ├── AtendenteService
    └── AtendimentoService
         ↓
Implementações (Strategies)
    ├── In-Memory (desenvolvimento)
    └── Redis (produção)
```

**Troca de implementação apenas mudando o profile!**

### Profiles Disponíveis

#### Profile `memory` (Desenvolvimento)
```properties
spring.profiles.active=memory
```
- Armazena tudo em memória (ConcurrentHashMap, Queue)
- Desenvolvimento e testes rápidos
- Não requer infraestrutura externa

#### Profile `redis` (Produção)
```properties
spring.profiles.active=redis
```
- Armazena dados no Redis
- Persistência e escalabilidade
- Necessário rodar Redis (disponível via Docker)

## 🚀 Como Executar

### Pré-requisitos

- Java 21+
- Maven 3.6+
- Docker & Docker Compose (opcional)

### 1. Clonar o Repositório

```bash
git clone https://github.com/flowpay/atendimento-service.git
cd atendimento-service
```

### 2. Executar em Desenvolvimento (In-Memory)

```bash
mvn spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080`

### 3. Executar com Redis (Produção)

#### 3.1. Subir o Redis via Docker

```bash
docker-compose up -d
```

#### 3.2. Executar com profile Redis

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

Ou definir no `application.properties`:
```properties
spring.profiles.active=redis
```

## Documentação da API

### Swagger UI

Acesse a documentação interativa em:
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI JSON

```
http://localhost:8080/v3/api-docs
```

### Endpoints Principais

#### Atendimentos

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/atendimentos` | Criar novo atendimento |
| GET | `/api/atendimentos` | Listar todos os atendimentos |
| GET | `/api/atendimentos/{id}` | Buscar atendimento por ID |
| GET | `/api/atendimentos/time/{time}` | Listar atendimentos por time |
| GET | `/api/atendimentos/status/{status}` | Listar atendimentos por status |
| PATCH | `/api/atendimentos/{id}/finalizar` | Finalizar atendimento |

#### Atendentes

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/atendentes` | Cadastrar atendente |
| GET | `/api/atendentes` | Listar todos os atendentes |
| GET | `/api/atendentes/{id}` | Buscar atendente por ID |
| GET | `/api/atendentes/time/{time}` | Listar atendentes por time |
| GET | `/api/atendentes/time/{time}/disponiveis` | Listar atendentes disponíveis |

#### Dashboard

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/dashboard/metricas` | Métricas gerais do sistema |
| GET | `/api/dashboard/time/{time}` | Status de um time específico |

### Exemplo de Requisição

#### Criar Atendimento

```bash
curl -X POST http://localhost:8080/api/atendimentos \
  -H "Content-Type: application/json" \
  -d '{
    "nomeCliente": "João Silva",
    "assunto": "Problema com cartão de crédito",
    "time": "CARTOES"
  }'
```

#### Cadastrar Atendente

```bash
curl -X POST http://localhost:8080/api/atendentes \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Ana Silva",
    "time": "CARTOES"
  }'
```

## WebSocket 

### Conectar ao WebSocket

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Conectado: ' + frame);

    // Inscrever em todos os times
    stompClient.subscribe('/topic/atendimentos', function(message) {
        console.log('Novo atendimento:', JSON.parse(message.body));
    });

    // Inscrever em time específico
    stompClient.subscribe('/topic/atendimentos/CARTOES', function(message) {
        console.log('Atualização time Cartões:', JSON.parse(message.body));
    });
});
```

### Tópicos Disponíveis

| Tópico | Descrição |
|--------|-----------|
| `/topic/atendimentos` | Todos os atendimentos |
| `/topic/atendimentos/CARTOES` | Atendimentos do time Cartões |
| `/topic/atendimentos/EMPRESTIMOS` | Atendimentos do time Empréstimos |
| `/topic/atendimentos/OUTROS` | Atendimentos do time Outros |
| `/topic/fila/{time}` | Atualizações da fila de um time |

## Health Checks

### Endpoints do Actuator

| Endpoint | Descrição |
|----------|-----------|
| `/actuator/health` | Status de saúde da aplicação |
| `/actuator/info` | Informações da aplicação |
| `/actuator/metrics` | Métricas de desempenho |

### Verificar Status

```bash
curl http://localhost:8080/actuator/health
```

## Testes

### Executar Testes

```bash
mvn test
```

## Configurações

### Principais Configurações (application.properties)

```properties
# Porta do servidor
server.port=8080

# Profile ativo (memory ou redis)
spring.profiles.active=memory

# Logging
logging.level.root=INFO
logging.level.com.flowpay=DEBUG

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
```

### Configurações do Redis (application-redis.properties)

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms
```

## 📊 Estrutura do Projeto

```
atendimento-service/
├── src/main/java/com/flowpay/atendimento/
│   ├── config/              # Configurações (WebSocket, Swagger, CORS)
│   ├── controller/          # Controllers REST
│   ├── dto/                 # DTOs (Request/Response)
│   ├── exception/           # Exceções customizadas
│   ├── model/               # Entidades do domínio
│   ├── service/             # Interfaces de serviços
│   │   └── impl/            # Implementações dos serviços
│   └── AtendimentoServiceApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-memory.properties
│   └── application-redis.properties
└── pom.xml
```