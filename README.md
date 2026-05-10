# Team Task Management Web Application

A full-stack, interview-ready task management application inspired by Trello/Asana. 
Built with Spring Boot 3+ (Java 17) and React (Vite + Tailwind CSS).

## 🚀 Features

- **Authentication**: JWT-based secure signup & login with BCrypt hashing.
- **Projects**: Create projects (Creator is auto-assigned `ADMIN` role).
- **Team Collaboration**: Admin can invite users to their project as `MEMBER`s.
- **Tasks**: Create, edit, delete tasks. Assign tasks to project members.
- **Status Tracking**: Kanban-style status tracking (TODO, IN_PROGRESS, DONE).
- **Dashboard**: High-level statistics on tasks and projects.
- **Role-Based Access Control**: Admins have full access to their projects; Members can only modify their assigned tasks.

## 🛠️ Tech Stack

### Backend
- **Java 17+**
- **Spring Boot 3.2.x** (Web, Data JPA, Security, Validation)
- **MySQL**
- **JWT (io.jsonwebtoken)**
- **Maven**
- **Lombok**

### Frontend
- **React 18+** (via Vite)
- **React Router DOM v6**
- **Tailwind CSS v4**
- **Axios** (with interceptors for JWT)
- **Lucide React** (Icons)

---

## 🏃‍♂️ How to Run Locally

### Prerequisites
- JDK 17+
- Node.js 18+
- MySQL database running locally

### 1. Database Setup
Create a MySQL database named `taskmanager`.
```sql
CREATE DATABASE taskmanager;
```

### 2. Backend Setup
1. Navigate to the `backend/` directory.
2. The default `application.properties` connects to `localhost:3306` with username/password `root/root`. If your local setup differs, you can pass environment variables or update `application.properties`.
3. Run the application:
```bash
./mvnw spring-boot:run
```
*(Or import the Maven project into your IDE and run `TaskmanagerApplication.java`)*

### 3. Frontend Setup
1. Navigate to the `frontend/` directory.
2. Install dependencies:
```bash
npm install
```
3. Start the dev server:
```bash
npm run dev
```
4. Access the application at `http://localhost:5173`.

---

## ☁️ Railway Deployment Steps

This application is ready to be deployed on Railway.

### Backend Deployment (Railway)
1. In Railway, provision a **MySQL** database.
2. Create a new service and link it to your GitHub repository (specifically pointing to the `backend/` folder).
3. Under the Service Settings > Variables, add the following standard Railway variables (or rely on Railway's auto-injected ones):
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`
   - `PORT` (e.g., `8080`)
   - `JWT_SECRET` (generate a random 64-character hex string)
4. Railway will automatically detect Maven and build the application.

### Frontend Deployment (Railway)
1. Create a new service from the same GitHub repo, pointing to the `frontend/` folder.
2. Under Variables, add:
   - `VITE_API_URL`: The public URL of your deployed backend (e.g., `https://my-backend.up.railway.app`).
3. Railway will build it using `npm run build`.

---

## 🔐 Role-Based Access Control (RBAC)

- **ADMIN**:
  - Automatically assigned to the user who creates a project.
  - Can invite/remove `MEMBER`s.
  - Can create, edit, and delete any task within the project.
  - Can delete the entire project.
- **MEMBER**:
  - Assigned when an ADMIN invites a user.
  - Can view tasks and project details.
  - Can only update the status of tasks specifically assigned to them.
  - Cannot delete projects, manage members, or delete tasks.

---

## 📡 API Endpoints Summary

### Auth
- `POST /api/auth/signup` - Register a new user
- `POST /api/auth/login` - Login and get JWT token
- `GET /api/auth/me` - Get current user profile

### Projects
- `POST /api/projects` - Create a project
- `GET /api/projects` - Get all projects for current user
- `GET /api/projects/{id}` - Get project details
- `DELETE /api/projects/{id}` - Delete project (ADMIN only)
- `GET /api/projects/{id}/members` - Get project members
- `POST /api/projects/{id}/members` - Invite member (ADMIN only)
- `DELETE /api/projects/{id}/members/{userId}` - Remove member (ADMIN only)

### Tasks
- `POST /api/projects/{id}/tasks` - Create task in project (ADMIN only)
- `GET /api/projects/{id}/tasks` - Get all tasks for project
- `PUT /api/tasks/{taskId}` - Update task details
- `PATCH /api/tasks/{taskId}/status` - Update task status
- `DELETE /api/tasks/{taskId}` - Delete task (ADMIN only)

### Dashboard
- `GET /api/dashboard/my-tasks` - Get user's personal task statistics
- `GET /api/dashboard/projects/{id}` - Get task statistics for a specific project
