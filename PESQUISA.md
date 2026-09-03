# Tutorial: Docker e Kubernetes em três provedores

## 1. Objetivo

Este documento apresenta um roteiro reproduzível para executar a aplicação **Biblioteca** com Docker e Kubernetes em três ambientes distintos:

1. Amazon Web Services — AWS, utilizando Amazon EKS.
2. Google Cloud Platform — GCP, utilizando Google Kubernetes Engine — GKE.
3. VPS DigitalOcean, utilizando K3s em nó único.

A escolha atende à regra acadêmica de utilizar **três provedores distintos**, sendo dois provedores de nuvem pública entre AWS, GCP e Azure e um terceiro ambiente diferente.

## 2. Aplicação de referência

A aplicação deste pacote foi construída para reproduzir o cenário solicitado:

- Spring Boot 4.0.6;
- Java 21;
- interface HTML com Thymeleaf;
- Spring Security;
- API REST com autenticação e geração de JWT;
- Spring Data JPA;
- banco H2 em memória;
- porta 8080;
- build Maven;
- cadastro e consulta de autores;
- cadastro e consulta de livros;
- administração de usuários conforme permissões.

### Credenciais acadêmicas

Administrador:

```text
Usuário: admin
Senha: admin
```

Usuário comum:

```text
Usuário: user
Senha: user
```

Essas credenciais existem exclusivamente para o laboratório. Não são adequadas para produção.

## 3. Limitação do H2

O datasource está configurado como:

```text
jdbc:h2:mem:banco
```

Isso significa que os dados existem somente enquanto o processo Java está em execução. Quando o container ou Pod é reiniciado, os dados cadastrados são perdidos.

Por esse motivo:

- o Deployment Kubernetes utiliza `replicas: 1`;
- a implantação deve ser tratada como demonstrativa;
- não há garantia de persistência;
- não é necessário migrar para outro banco nesta atividade.

## 4. Arquitetura mínima

```text
Usuário
  |
IP/DNS público
  |
Service / LoadBalancer / NodePort
  |
Deployment Kubernetes
  |
Pod da aplicação Biblioteca
  |
H2 em memória
```

A mesma imagem Docker deve ser utilizada nos três ambientes. Os manifests só devem mudar quando o provedor exigir alguma adaptação justificável.

## 5. Pré-requisitos locais

Instalar:

- Java 21;
- Maven 3.9+;
- Docker Engine ou Docker Desktop;
- kubectl;
- VS Code ou outra IDE;
- AWS CLI;
- eksctl;
- Google Cloud CLI;
- cliente SSH.

Verifique:

```bash
java -version
mvn -version
docker --version
kubectl version --client
aws --version
eksctl version
gcloud --version
```

## 6. Executar localmente no VS Code

Abra a pasta do projeto no VS Code e execute:

Windows:

```powershell
mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Alternativamente:

```bash
mvn spring-boot:run
```

Acessos:

```text
Aplicação: http://localhost:8080/
Login: http://localhost:8080/login
Console H2: http://localhost:8080/h2-console
```

Valide login, autores e livros antes de seguir para Docker.

## 7. Testar a API REST

Endpoint:

```text
POST /api/auth/login
```

Exemplo:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

A resposta deve conter um token JWT e o tipo `Bearer`.

Para comprovar o acesso autenticado com Bearer Token, copie o token retornado e execute:

```bash
curl http://localhost:8080/api/livros \
  -H "Authorization: Bearer SEU_TOKEN"
```

O endpoint `/api/autores` também exige autenticação.

## 8. Dockerfile

O projeto utiliza build multi-stage:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/biblioteca-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

A imagem final contém somente o runtime Java e o JAR da aplicação, reduzindo o tamanho em relação a uma imagem que mantivesse Maven.

## 9. `.dockerignore`

```text
.git
.idea
.vscode
target
docs
*.log
*.iml
.env
README.md
```

Objetivos:

- reduzir o contexto enviado ao Docker;
- evitar arquivos locais desnecessários;
- diminuir o tempo de build;
- evitar o envio de credenciais.

## 10. Construir a imagem

```bash
docker build -t biblioteca:1.0 .
```

Validar:

```bash
docker image ls biblioteca
docker history biblioteca:1.0
```

## 11. Executar a imagem localmente

```bash
docker run --name biblioteca-local -p 8080:8080 \
  -e API_SECURITY_TOKEN_SECRET="segredo-super-forte-com-mais-de-32-bytes" \
  biblioteca:1.0
```

Consultar logs:

```bash
docker logs biblioteca-local
```

Encerrar:

```bash
docker stop biblioteca-local
docker rm biblioteca-local
```

Só prossiga se o container iniciar corretamente e o login funcionar.

## 12. Registro da imagem

A mesma imagem deve ser disponibilizada para os três ambientes. Pode-se utilizar Docker Hub para simplificar a demonstração ou registros específicos de cada provedor.

Fluxo genérico:

```bash
docker tag biblioteca:1.0 REGISTRO/USUARIO/biblioteca:1.0
docker push REGISTRO/USUARIO/biblioteca:1.0
```

## 13. Segredo JWT no Kubernetes

O segredo JWT não deve ser salvo diretamente no repositório público.

Exemplo de criação direta:

```bash
kubectl create secret generic biblioteca-secret \
  --from-literal=jwt-secret="VALOR-FORTE-COM-32-BYTES-OU-MAIS"
```

Ou utilizar `k8s/secret.yaml`, substituindo o marcador antes de aplicar.

## 14. Deployment

O arquivo `k8s/deployment.yaml` define:

- uma réplica;
- imagem Docker;
- porta 8080;
- segredo JWT por variável de ambiente;
- requests e limits;
- readinessProbe;
- livenessProbe.

Aplicar:

```bash
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
```

Verificar:

```bash
kubectl get pods
kubectl get deployment
kubectl rollout status deployment/biblioteca
```

Diagnóstico:

```bash
kubectl describe pod NOME_DO_POD
kubectl logs deployment/biblioteca
```

## 15. Service em nuvem gerenciada

AWS EKS e GKE podem utilizar:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: biblioteca
spec:
  type: LoadBalancer
  selector:
    app: biblioteca
  ports:
    - port: 80
      targetPort: 8080
```

Aplicar:

```bash
kubectl apply -f k8s/service-cloud.yaml
kubectl get service biblioteca
```

## 16. Provedor 1 — AWS com Amazon EKS

### 16.1 Autenticar

```bash
aws configure
aws sts get-caller-identity
```

Defina uma região adequada ao laboratório, por exemplo:

```bash
export AWS_REGION=us-east-1
```

No PowerShell:

```powershell
$env:AWS_REGION="us-east-1"
```

### 16.2 Criar repositório ECR

```bash
aws ecr create-repository \
  --repository-name biblioteca \
  --region $AWS_REGION
```

Obtenha o ID da conta:

```bash
aws sts get-caller-identity --query Account --output text
```

Faça login no ECR:

```bash
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin ID_DA_CONTA.dkr.ecr.$AWS_REGION.amazonaws.com
```

### 16.3 Publicar imagem

```bash
docker tag biblioteca:1.0 ID_DA_CONTA.dkr.ecr.$AWS_REGION.amazonaws.com/biblioteca:1.0
docker push ID_DA_CONTA.dkr.ecr.$AWS_REGION.amazonaws.com/biblioteca:1.0
```

Edite `k8s/deployment.yaml` e use essa imagem.

### 16.4 Criar cluster EKS

```bash
eksctl create cluster \
  --name biblioteca \
  --region $AWS_REGION \
  --nodes 1
```

Validar:

```bash
kubectl get nodes
```

### 16.5 Implantar

```bash
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service-cloud.yaml
kubectl get pods
kubectl get svc
```

Aguarde o `EXTERNAL-IP` ou hostname do LoadBalancer.

### 16.6 Validar

Abra o endereço público e verifique:

- `/login`;
- autenticação;
- cadastro de autores;
- cadastro de livros;
- logs sem erro impeditivo.

### 16.7 Limpeza AWS

```bash
kubectl delete -f k8s/service-cloud.yaml
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/secret.yaml
eksctl delete cluster --name biblioteca --region $AWS_REGION
```

Opcionalmente remover o repositório ECR:

```bash
aws ecr delete-repository --repository-name biblioteca --force --region $AWS_REGION
```

## 17. Provedor 2 — Google Cloud com GKE

### 17.1 Login e projeto

```bash
gcloud auth login
gcloud config set project ID_DO_PROJETO
```

### 17.2 Habilitar APIs

```bash
gcloud services enable container.googleapis.com artifactregistry.googleapis.com
```

### 17.3 Criar Artifact Registry

Exemplo de região:

```bash
export GCP_REGION=us-central1
```

Criar repositório:

```bash
gcloud artifacts repositories create biblioteca \
  --repository-format=docker \
  --location=$GCP_REGION
```

Configurar autenticação Docker:

```bash
gcloud auth configure-docker $GCP_REGION-docker.pkg.dev
```

### 17.4 Publicar imagem

```bash
docker tag biblioteca:1.0 \
  $GCP_REGION-docker.pkg.dev/ID_DO_PROJETO/biblioteca/biblioteca:1.0

docker push \
  $GCP_REGION-docker.pkg.dev/ID_DO_PROJETO/biblioteca/biblioteca:1.0
```

Atualize a imagem em `k8s/deployment.yaml`.

### 17.5 Criar GKE Autopilot

```bash
gcloud container clusters create-auto biblioteca --region $GCP_REGION
```

Obter credenciais:

```bash
gcloud container clusters get-credentials biblioteca --region $GCP_REGION
```

Validar:

```bash
kubectl get nodes
```

### 17.6 Implantar

```bash
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service-cloud.yaml
kubectl get pods
kubectl get service biblioteca
```

### 17.7 Validar

Aguarde o IP externo e faça as mesmas validações realizadas na AWS.

### 17.8 Limpeza GCP

```bash
kubectl delete -f k8s/service-cloud.yaml
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/secret.yaml
```

Excluir cluster:

```bash
gcloud container clusters delete biblioteca --region $GCP_REGION
```

Excluir registro, se necessário:

```bash
gcloud artifacts repositories delete biblioteca --location=$GCP_REGION
```

## 18. Provedor 3 — VPS DigitalOcean com K3s

### 18.1 Criar VPS

Recomenda-se uma VPS Linux com:

- Ubuntu LTS ou outra distribuição compatível;
- endereço IPv4 público;
- 2 GB de RAM ou mais para maior folga no laboratório;
- acesso SSH administrativo.

Registre no trabalho:

- produto/plano;
- região;
- vCPU;
- RAM;
- armazenamento;
- sistema operacional.

### 18.2 Acessar por SSH

```bash
ssh root@IP_DA_VPS
```

### 18.3 Instalar K3s

```bash
curl -sfL https://get.k3s.io | sh -
```

Validar:

```bash
sudo kubectl get nodes
```

### 18.4 Enviar os manifests

No computador local:

```bash
scp -r k8s root@IP_DA_VPS:/root/
```

### 18.5 Configurar imagem

O arquivo `deployment.yaml` deve apontar para uma imagem acessível pela VPS. Para simplificar, pode ser utilizado um registro público durante a atividade acadêmica.

### 18.6 Implantar

```bash
sudo kubectl apply -f /root/k8s/secret.yaml
sudo kubectl apply -f /root/k8s/deployment.yaml
sudo kubectl apply -f /root/k8s/service-vps.yaml
```

Validar:

```bash
sudo kubectl get pods
sudo kubectl get services
sudo kubectl logs deployment/biblioteca
```

### 18.7 Liberar porta

O `service-vps.yaml` utiliza NodePort 30080.

A porta TCP 30080 deve ser permitida no firewall do provedor e, se necessário, no firewall do sistema operacional.

Exemplo com UFW:

```bash
sudo ufw allow 30080/tcp
```

Acesso:

```text
http://IP_DA_VPS:30080/login
```

### 18.8 HTTPS

Para produção não é recomendado manter somente NodePort HTTP. Uma arquitetura real deveria utilizar Ingress ou proxy reverso com certificado TLS/HTTPS.

### 18.9 Limpeza VPS

Remover recursos:

```bash
sudo kubectl delete -f /root/k8s/service-vps.yaml
sudo kubectl delete -f /root/k8s/deployment.yaml
sudo kubectl delete -f /root/k8s/secret.yaml
```

Desinstalar K3s:

```bash
/usr/local/bin/k3s-uninstall.sh
```

Depois, excluir a VPS no painel da DigitalOcean para interromper cobranças.

## 19. Evidências obrigatórias

Para cada provedor, registrar capturas de tela contendo:

1. provedor, produto e região;
2. cluster criado;
3. nós disponíveis;
4. imagem no registro;
5. Deployment disponível;
6. Pod em estado Running e Ready;
7. Service ou NodePort;
8. aplicação acessível externamente;
9. login realizado com sucesso;
10. autores e livros funcionando;
11. logs sem erro impeditivo;
12. procedimento de remoção.

Não mostrar:

- tokens;
- chaves privadas;
- segredos JWT;
- dados de cobrança pessoais;
- identificadores sensíveis desnecessários.

## 20. Solução de problemas

### `ImagePullBackOff`

Possíveis causas:

- nome da imagem incorreto;
- tag inexistente;
- registro privado sem autenticação.

Verificar:

```bash
kubectl describe pod NOME_DO_POD
```

### `CrashLoopBackOff`

Consultar:

```bash
kubectl logs deployment/biblioteca
kubectl describe pod NOME_DO_POD
```

### Pod não fica Ready

Pode ser necessário aumentar `initialDelaySeconds` das probes caso a infraestrutura seja lenta.

### LoadBalancer sem IP externo

Verificar:

```bash
kubectl get svc
kubectl describe svc biblioteca
```

Em uma VPS simples, prefira NodePort ou Ingress.

### Erro de JWT

Certifique-se de que `API_SECURITY_TOKEN_SECRET` tenha no mínimo 32 bytes.

## 21. Comparação de custos

Os valores devem ser levantados no dia da aula ou da entrega. Como preços e descontos mudam, o grupo deve registrar:

- data da consulta;
- moeda;
- região;
- sistema operacional;
- arquitetura;
- quantidade e tamanho dos nós;
- horas estimadas no mês;
- gerenciamento do cluster;
- computação;
- disco;
- armazenamento da imagem;
- LoadBalancer ou IP público;
- tráfego de saída;
- DNS, quando usado.

Créditos gratuitos devem ser separados do preço normal.

### Modelo

| Critério | AWS EKS | Google GKE | DigitalOcean + K3s |
|---|---:|---:|---:|
| Serviço Kubernetes | EKS | GKE Autopilot | K3s |
| Região | preencher | preencher | preencher |
| Gestão do cluster/mês | pesquisar | pesquisar | incluída no esforço operacional |
| Computação/mês | pesquisar | pesquisar | pesquisar |
| Disco/armazenamento | pesquisar | pesquisar | pesquisar |
| LoadBalancer/IP/rede | pesquisar | pesquisar | pesquisar |
| Total estimado | calcular | calcular | calcular |
| Tempo de implantação | medir | medir | medir |

## 22. Comparação qualitativa

| Critério | AWS EKS | Google GKE | DigitalOcean + K3s |
|---|---|---|---|
| Facilidade de configuração | Média | Alta com Autopilot | Média |
| Kubernetes gerenciado | Sim | Sim | Não |
| Escalabilidade | Alta | Alta | Depende da administração |
| Integração com registro | Alta com ECR | Alta com Artifact Registry | Manual/registro externo |
| Observabilidade | Serviços AWS | Serviços Google Cloud | Configuração manual |
| Responsabilidade operacional | Menor | Menor | Maior |
| Controle | Médio/alto | Médio | Alto |
| Indicação | Projetos integrados à AWS | Equipes que querem reduzir operação | Laboratório e cenários de baixo custo |

## 23. Vantagens e desvantagens

### AWS EKS

Vantagens:

- ecossistema amplo;
- integração com ECR, IAM e observabilidade;
- alta escalabilidade.

Desvantagens:

- mais serviços e conceitos para configurar;
- controle de custos exige atenção;
- curva de aprendizado maior.

### Google GKE

Vantagens:

- forte integração com Kubernetes;
- GKE Autopilot reduz trabalho operacional;
- boa experiência de implantação.

Desvantagens:

- serviços extras podem gerar cobrança;
- dependência de componentes do Google Cloud.

### DigitalOcean + K3s

Vantagens:

- arquitetura simples;
- maior controle;
- excelente para demonstrar Kubernetes em VPS.

Desvantagens:

- atualizações e segurança ficam sob responsabilidade da equipe;
- não existe plano de controle totalmente gerenciado no modelo descrito;
- alta disponibilidade exigiria arquitetura adicional.

## 24. Recomendação final

Para uma equipe acadêmica que pretende demonstrar conceitos de Kubernetes com o menor esforço operacional possível, o **GKE Autopilot** tende a ser uma alternativa bastante adequada entre os ambientes gerenciados.

A AWS EKS é recomendável quando a aplicação será integrada a outros serviços AWS e quando a equipe precisa explorar um ecossistema corporativo amplo.

A VPS com K3s é especialmente útil para demonstrar que Kubernetes também pode ser operado fora dos grandes serviços gerenciados, com maior controle e maior responsabilidade operacional.

No trabalho final, a recomendação deve considerar não somente o custo financeiro, mas também:

- facilidade de implantação;
- tempo gasto;
- responsabilidade de manutenção;
- escalabilidade;
- segurança;
- observabilidade;
- experiência da equipe.

## 25. Estrutura sugerida para entrega

1. Capa e integrantes.
2. Objetivo e escopo.
3. Análise da aplicação Biblioteca.
4. Containerização com Docker.
5. Manifestos Kubernetes.
6. Tutorial AWS EKS.
7. Tutorial Google GKE.
8. Tutorial DigitalOcean/K3s.
9. Evidências e testes.
10. Comparação de custos.
11. Vantagens e desvantagens.
12. Recomendação final.
13. Limpeza dos recursos.
14. Referências.

## 26. Referências sugeridas

Para a versão final do trabalho, utilizar prioritariamente documentação oficial e registrar a data de acesso:

- Spring Boot Documentation;
- Docker Documentation;
- Kubernetes Documentation;
- Amazon EKS Documentation;
- Amazon ECR Documentation;
- Google Kubernetes Engine Documentation;
- Google Artifact Registry Documentation;
- K3s Documentation;
- DigitalOcean Documentation.

---

**Observação:** valores de custos não foram fixados neste arquivo porque devem ser consultados e registrados na data da aula ou entrega, conforme a regra da atividade.
