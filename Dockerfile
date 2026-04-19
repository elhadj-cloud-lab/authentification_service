# -------------------------------
# Étape 1 : Build de l'application
# -------------------------------
# Image Maven avec Java 21 (Eclipse Temurin recommandé)
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Dossier de travail dans le conteneur
WORKDIR /app

# Copier uniquement le pom.xml pour profiter du cache Docker
# → les dépendances ne seront retéléchargées que si le pom change
COPY pom.xml .

# Télécharger les dépendances Maven (optimisation du cache)
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copier le code source
COPY src ./src

# Compiler et packager l'application (sans lancer les tests)
RUN mvn clean package -DskipTests


# -------------------------------
# Étape 2 : Runtime (image finale)
# -------------------------------
# Image légère avec Java 21 (JRE suffisant pour exécuter un JAR)
FROM eclipse-temurin:21-jre-jammy

# Dossier de travail
WORKDIR /app

# Création d'un utilisateur non-root pour la sécurité
# → évite d'exécuter l'application en tant que root
RUN addgroup --system --gid 1000 appgroup && \
    adduser --system --uid 1000 --gid 1000 appuser

# Copier le JAR généré depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Utiliser l'utilisateur non-root
USER appuser

# Exposer le port de l'application
EXPOSE 8080

# Commande de lancement de l'application
ENTRYPOINT ["java", "-jar", "app.jar"]