Learning Management System (LMS) - JavaFX Application
Overview
This project is a comprehensive Learning Management System (LMS) built using JavaFX for the user interface and PostgreSQL for data persistence. It supports role-based functionalities for administrators, instructors, and students, enabling user management, course creation, content upload, assessments, grading, communication, and reporting.

Features
User Management
Register new users with roles: admin, instructor, student
Role-based access control
Login and logout functionality
Activate/deactivate users
View system activity logs
Manage user enrollments
Course Management
Create, update, delete, activate, deactivate courses
Enroll students in courses
View enrolled students
Upload and access course content files
Manage course details
Assessments & Quizzes
Create, update, delete assessments
Add multiple-choice questions (single/multiple answers)
Submit assessments
Grade student submissions
View individual grades and overall progress
Content & Resources
Upload course content files (documents, videos, etc.)
Access course materials within the application
Communication & Forums
Instructor discussion forums
Participate in student forums
Post and view messages
Reports & Analytics
Generate CSV reports for users and course enrollments
Visualize reports with bar charts
View system logs
Certificates & Notifications
Generate downloadable completion certificates
Manage system notifications
Student Dashboard
View enrolled courses
Access course materials
Take quizzes & assessments
Submit assignments
View grades & progress
Download certificates
Update profile information
Technologies Used
JavaFX for GUI
PostgreSQL as the database backend
JDBC for database connectivity
Maven for project management and dependency handling
Desktop (AWT) for opening files and reports
Setup Instructions
Prerequisites
Java Development Kit (JDK 22 or compatible)
PostgreSQL database installed and running
Maven build system
Database Setup
Ensure your PostgreSQL database has the following tables with appropriate schema:

users (id, username, email, password, role, active)
system_logs (timestamp, username, action)
courses (id, name, description, instructor, status)
enrollments (student_username, course_id)
course_content (id, course_id, title, description, file_path)
assessments (id, course_id, title, description)
questions (question_id, question_text, is_multiple_answer)
options (option_id, question_id, option_text, is_correct)
submissions (id, student_username, assessment_id, submission_text, grade)
student_progress (student_username, course_id, progress_percentage)
system_settings (setting_key, setting_value)
notifications (message, created_at)
discussion_forums (name)
forum_messages (forum_name, message, sender)

The application will launch with the login screen.
Usage
Login/Register: Use your credentials or register a new user.
Admin functionalities: Manage users, courses, system settings, notifications, view logs.
Instructor functionalities: Manage courses, upload content, create assessments and questions, participate in forums.
Student functionalities: Enroll in courses, access materials, take assessments, submit assignments, view grades, participate in forums, generate certificates.
Extending & Customizing
Add new question types or content formats.
Enhance security with hashed passwords.
Improve UI/UX with custom styling.
Integrate email notifications for events.
Add features like gamification, badges, or advanced analytics.
