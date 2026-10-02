# Hospital Management System

A comprehensive multi-specialty hospital management system with appointment scheduling, patient management, and doctor consultation features.

## 🏥 Features

### Patient Portal
- User registration and authentication
- Appointment booking with real-time availability
- Medical history access
- Prescription management
- Appointment reminders and notifications

### Doctor Portal
- Schedule management
- Consultation notes
- Prescription management
- Patient medical records
- Appointment status management

### Admin Portal
- Doctor onboarding
- Department management
- User management
- System configuration
- Billing oversight

## 🛠 Technology Stack

### Backend
- **Java 21** - Programming language
- **Spring Boot 3.2.0** - Application framework
- **Spring Cloud** - Microservices support
- **Spring Security** - Security framework
- **PostgreSQL** - Database
- **Redis** - Caching layer
- **Kafka** - Message broker for notifications
- **JWT** - Authentication tokens

### Frontend
- **React 18** - UI framework
- **Ant Design** - UI component library
- **Axios** - HTTP client
- **React Router** - Navigation

### DevOps
- **Docker** - Containerization
- **Kubernetes** - Orchestration
- **Jenkins** - CI/CD pipeline
- **SonarQube** - Code quality analysis
- **Nexus Repository** - Artifact management
- **ELK Stack** - Logging and monitoring

## 📄 Requirements

See [Software Requirements Specification](docs/SRS.md) for functional requirements, user roles, API scope, security expectations, and items that still need confirmation.

## 📋 Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+
- PostgreSQL 15
- Redis 7
- Kafka 7.5
- Docker & Docker Compose
- Kubernetes cluster (for production deployment)

## 🚀 Quick Start

### Using Docker Compose

1. Clone the repository:
```bash
git clone <repository-url>
cd hospital-management-system
```

2. Start all services:
```bash
docker-compose up -d
```

3. Access the application:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432
- Redis: localhost:6379
- Kafka: localhost:9092

### Manual Setup

#### Backend Setup

1. Navigate to backend directory:
```bash
cd backend
```

2. Update database configuration in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hospital_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

3. Build and run:
```bash
mvn clean install
mvn spring-boot:run
```

#### Frontend Setup

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

4. Access the application at http://localhost:3000

## 🗄️ Database Schema

The system uses the following main entities:

- **Users** - Authentication and authorization
- **Patients** - Patient profiles and medical information
- **Doctors** - Doctor profiles and specialties
- **Departments** - Hospital departments
- **Appointments** - Appointment scheduling
- **Medical Records** - Patient medical history
- **Prescriptions** - Medication prescriptions
- **Schedules** - Doctor availability schedules

## 🔐 Security

- JWT-based authentication
- Role-based access control (RBAC)
- Password encryption using BCrypt
- CORS configuration
- SQL injection prevention via JPA

## 📡 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Patient Endpoints
- `GET /api/patient/profile` - Get patient profile
- `PUT /api/patient/profile` - Update patient profile
- `GET /api/patient/appointments` - Get patient appointments
- `POST /api/patient/appointments` - Book appointment
- `DELETE /api/patient/appointments/{id}` - Cancel appointment
- `GET /api/patient/medical-history` - Get medical history
- `GET /api/patient/prescriptions` - Get prescriptions

### Doctor Endpoints
- `GET /api/doctor/appointments` - Get doctor appointments
- `PUT /api/doctor/appointments/{id}/status` - Update appointment status
- `POST /api/doctor/medical-records` - Create medical record
- `GET /api/doctor/medical-records` - Get medical records
- `POST /api/doctor/prescriptions` - Create prescription
- `GET /api/doctor/prescriptions` - Get prescriptions
- `POST /api/doctor/schedules` - Create schedule
- `GET /api/doctor/schedules` - Get schedules

### Admin Endpoints
- `GET /api/admin/departments` - Get all departments
- `POST /api/admin/departments` - Create department
- `PUT /api/admin/departments/{id}` - Update department
- `DELETE /api/admin/departments/{id}` - Delete department
- `GET /api/admin/doctors` - Get all doctors
- `POST /api/admin/doctors` - Create doctor
- `PUT /api/admin/doctors/{id}` - Update doctor
- `DELETE /api/admin/doctors/{id}` - Delete doctor

## 🔔 Notifications

The system uses Kafka for sending notifications:
- Appointment reminders (sent 24 hours before)
- Prescription notifications
- Follow-up alerts

Topics:
- `appointment-reminders`
- `prescription-notifications`
- `follow-up-alerts`

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 🚢 Deployment

### Kubernetes Deployment

1. Build Docker images:
```bash
docker build -t hospital-backend:latest ./backend
docker build -t hospital-frontend:latest ./frontend
```

2. Apply Kubernetes manifests:
```bash
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/kafka-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml
```

3. Verify deployment:
```bash
kubectl get pods
kubectl get services
```

## 🔄 CI/CD Pipeline

### Jenkins Pipeline
- Manual and automated deployment
- Staging and production environments
- Quality gates with SonarQube
- Nexus repository integration

## 📊 Monitoring

The system uses ELK Stack for monitoring:
- **Elasticsearch** - Log storage and search
- **Logstash** - Log processing
- **Kibana** - Log visualization

## 🤝 Contributing

Use the contribution and review process configured for the Git hosting service chosen for this project.

## 📝 License

This project is licensed under the MIT License.

## 👥 Team

Hospital Management System Development Team

## 📞 Support

For support, contact the project maintainers through the channel configured for this project.