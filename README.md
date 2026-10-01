# authentification-service

Microservice d'authentification JWT pour `elhadj-cloud-lab`.

## Stack

- **Spring Boot** 3.4.5 · Java 21
- **Sécurité** : Spring Security + JWT (Auth0 `java-jwt`) stateless
- **Base de données** : PostgreSQL (JPA / Hibernate)
- **Cache** : Redis (blacklist des tokens révoqués)
- **Email** : Spring Mail (vérification à l'inscription)
- **Tests** : JUnit 5 · Testcontainers · H2 (tests unitaires)

## Endpoints

### Authentification publique

| Méthode | URL | Description |
|---|---|---|
| `POST` | `/users/login` | Connexion → renvoie `Authorization` + `Refresh-Token` dans les headers |
| `POST` | `/users/refresh` | Renouvellement du token d'accès |
| `POST` | `/users/logout` | Révocation du token courant |
| `POST` | `/users/register` | Inscription + envoi email de vérification |
| `GET` | `/users/verifyEmail/{token}` | Activation du compte |

### Utilisateurs (ADMIN)

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/users/all` | Liste tous les utilisateurs |
| `POST` | `/users/admin/revoke/{username}` | Révoque toutes les sessions d'un utilisateur |

### Tableau de bord admin (ADMIN)

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/users/api/admin/stats/dashboard` | Statistiques de connexion : KPIs, graphiques horaires/journaliers, 20 derniers événements |

### Actuator

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/users/actuator/health` | État du service (DB, Redis) |

## Fonctionnalités de sécurité

- **JWT stateless** : access token (15 min par défaut) + refresh token (7 jours)
- **Blacklist Redis** : les tokens révoqués sont mis en cache jusqu'à leur expiration
- **BCrypt** : hashage des mots de passe
- **Rôles** : `ADMIN`, `USER`
- **Vérification email** : compte désactivé jusqu'à confirmation

## Statistiques de connexion

Chaque tentative de login est enregistrée dans la table `login_events` :

| Colonne | Type | Description |
|---|---|---|
| `username` | `VARCHAR` | Nom d'utilisateur tenté |
| `ip_address` | `VARCHAR(45)` | IP client (supporte IPv6 + `X-Forwarded-For`) |
| `user_agent` | `VARCHAR(500)` | User-Agent du navigateur |
| `success` | `BOOLEAN` | Succès ou échec |
| `failure_reason` | `VARCHAR(100)` | `INVALID_CREDENTIALS` ou `DISABLED` |
| `event_time` | `TIMESTAMP` | Horodatage de l'événement |

Le dashboard expose :
- Connexions du jour (total / réussies / échouées / taux de succès)
- Nombre total d'utilisateurs enregistrés
- Répartition par heure (aujourd'hui)
- Tendance sur 7 jours
- 20 derniers événements

## Variables d'environnement

| Variable | Obligatoire | Défaut | Description |
|---|---|---|---|
| `JWT_SECRET` | Oui | — | Clé secrète JWT |
| `JWT_ACCESS_EXPIRATION` | Non | `900000` | Durée access token (ms) |
| `JWT_REFRESH_EXPIRATION` | Non | `604800000` | Durée refresh token (ms) |
| `SPRING_DATASOURCE_URL` | Oui | — | URL PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Oui | — | Utilisateur PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | Oui | — | Mot de passe PostgreSQL |
| `REDIS_HOST` | Non | `localhost` | Host Redis |
| `REDIS_PORT` | Non | `6379` | Port Redis |
| `MAIL_USERNAME` | Oui | — | Adresse Gmail expéditrice |
| `MAIL_PASSWORD` | Oui | — | App password Gmail |

## Lancer en local

```bash
export JWT_SECRET=dev-secret-change-me
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export MAIL_USERNAME=your@email.com
export MAIL_PASSWORD=your-app-password

mvn spring-boot:run
```

Service disponible sur `http://localhost:8080/users`

## Tests

```bash
mvn test
```

Les tests d'intégration utilisent **Testcontainers** (PostgreSQL) et excluent Redis via le profil `application-integration-test.yaml`.

## Docker

```bash
# Build
docker build -t authentification-service:local .

# Run (avec variables d'environnement)
docker run --rm -p 8080:8080 \
  -e JWT_SECRET=dev-secret \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/auth_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e MAIL_USERNAME=your@email.com \
  -e MAIL_PASSWORD=your-app-password \
  authentification-service:local
```

## CI/CD

Workflow : `.github/workflows/deploy.yaml`

- **PR → `develop`** : build + tests + docker build (sans push)
- **Push → `main`** : push image `dev-<sha>` sur GHCR + mise à jour GitOps DEV
- **Push → `prod`** : push image `prod-<sha>` + SBOM/provenance + mise à jour GitOps PROD

## Structure du code

```
src/main/java/com/bestech/authentification_service/
├── Controllers/
│   ├── AuthController.java          # /refresh, /logout, /admin/revoke
│   ├── UserController.java          # /register, /all, /verifyEmail
│   └── AdminStatsController.java    # /api/admin/stats/dashboard
├── dto/
│   ├── DashboardStatsDto.java
│   ├── HourlyStatsDto.java
│   ├── DailyStatsDto.java
│   └── RecentEventDto.java
├── model/
│   ├── MyUser.java
│   ├── Role.java
│   └── LoginEvent.java              # Historique des connexions
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── LoginEventRepository.java    # Requêtes stats PostgreSQL natives
│   └── ...
├── security/
│   ├── SecurityConfig.java
│   ├── JWTAuthenticationFilter.java # Filtre login → enregistre LoginEvent
│   ├── JWTAuthorizationFilter.java
│   ├── JwtTokenService.java
│   └── LogoutService.java
└── service/
    ├── UserService.java
    ├── LoginEventService.java        # recordSuccess/Failure + getDashboardStats
    └── refreshtoken/
```
