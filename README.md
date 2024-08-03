# Stack Exchange Clone

This project is a Stack Exchange clone built using Spring Boot with Kotlin.
The application provides features such as user authentication, question and
answer posting, voting, and more.

## Features

- User authentication (email/password, OAuth2)
- Question and answer posting
- Voting system
- Commenting on posts
- Email notifications for various events

## Prerequisites

1. **Java 21**: Make sure you have JDK 21 installed on your machine.
2. **Gradle**: Ensure Gradle is installed. The project uses the Gradle wrapper, so you can also use `./gradlew`.
3. **Docker**: Docker is required to run the application in development mode, as it uses Docker Compose for the 
   database and other services.
4. **Mailtrap Account**: A Mailtrap account is needed to handle email functionalities during development.

## Setup

### Environment Variables

Create a `.env` file in the root directory of the project. You can use the `.env.example` file as a template.

```text
MAIL_SERVER_USERNAME=example
MAIL_SERVER_PASSWORD=example
```

Replace `example` with your actual Mailtrap credentials.

### Running the Application

To run the application in local development mode, execute the following command:

```bash
./gradlew bootRun "-Dspring.profiles.active=local"
```

This command sets the active profile to local, which is configured to work with local development settings,
including the usage of Docker Compose for setting up the database.

## License

This project is licensed under the MIT License. See the `LICENSE.txt` file for details.