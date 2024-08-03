# Stack Exchange Clone

This project is a Stack Exchange clone built using Spring Boot with Kotlin.
The application provides features such as user authentication, question and
answer posting, voting, and more.

## Features

- User registration with username, email, and password
- Authentication using opaque bearer tokens and refresh tokens
- Question and answer posting
- Voting system
- Commenting on posts
- Email notifications for various events
- **Planned Feature:** OAuth2 user registration and authentication

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

## Authentication and Authorization

### User Registration

Users can register with the application using a username, email, and password. Upon successful registration,
a verification email is sent to the user's email address to confirm their account.

### Token-Based Authentication

The application uses opaque bearer tokens for authentication. Upon successful login, users receive an access token
and a refresh token.

- **Access Token:** This token is used to authenticate and authorize the user for API requests. It has a limited
  lifespan for security purposes.
- **Refresh Token:** This token is used to obtain a new access token without re-authenticating. It has a longer
  lifespan and should be stored securely.

Users must include the bearer token in the Authorization header of their API requests:

```text
Authorization: Bearer <access_token>
```

### Planned Feature: OAuth2 User Registration

We plan to support OAuth2-based user registration and authentication in future releases. This feature will allow
users to register and log in using their existing accounts from popular OAuth2 providers.

## License

This project is licensed under the MIT License. See the `LICENSE.txt` file for details.