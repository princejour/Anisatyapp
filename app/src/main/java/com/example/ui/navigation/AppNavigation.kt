package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.repository.AuthRepository
import com.example.repository.FirestoreRepository
import com.example.repository.PreferencesRepository
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.parent.ParentDashboardScreen
import com.example.ui.screens.parent.ParentLoginScreen
import com.example.ui.screens.teacher.ClassDetailsScreen
import com.example.ui.screens.teacher.StudentDetailsScreen
import com.example.ui.screens.teacher.TeacherDashboardScreen
import com.example.ui.screens.teacher.TeacherLoginScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object WelcomeRoute
@Serializable object TeacherLoginRoute
@Serializable object TeacherDashboardRoute
@Serializable data class ClassDetailsRoute(val classId: String, val className: String, val classGroup: String)
@Serializable data class StudentDetailsRoute(val studentId: String)
@Serializable object ParentLoginRoute
@Serializable data class ParentDashboardRoute(val studentId: String)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefsRepo = remember { PreferencesRepository(context) }
    val authRepo = remember { AuthRepository(prefsRepo) }
    val firestoreRepo = remember { FirestoreRepository() }
    
    val userRole by prefsRepo.userRoleFlow.collectAsState(initial = null)
    val parentCode by prefsRepo.parentCodeFlow.collectAsState(initial = null)
    
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = WelcomeRoute
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onTeacherClick = {
                    if (userRole == "teacher") {
                        navController.navigate(TeacherDashboardRoute) {
                            popUpTo(WelcomeRoute) { inclusive = true }
                        }
                    } else {
                        navController.navigate(TeacherLoginRoute)
                    }
                },
                onParentClick = {
                    navController.navigate(ParentLoginRoute)
                }
            )
        }
        
        composable<TeacherLoginRoute> {
            TeacherLoginScreen(
                authRepository = authRepo,
                onLoginSuccess = {
                    navController.navigate(TeacherDashboardRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<TeacherDashboardRoute> {
            TeacherDashboardScreen(
                firestoreRepository = firestoreRepo,
                onClassClick = { classId, className, classGroup ->
                    navController.navigate(ClassDetailsRoute(classId, className, classGroup))
                },
                onLogout = {
                    coroutineScope.launch {
                        authRepo.logout()
                    }
                    navController.navigate(WelcomeRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable<ClassDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ClassDetailsRoute>()
            ClassDetailsScreen(
                route = route,
                firestoreRepository = firestoreRepo,
                onStudentClick = { studentId ->
                    navController.navigate(StudentDetailsRoute(studentId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<StudentDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<StudentDetailsRoute>()
            StudentDetailsScreen(
                studentId = route.studentId,
                firestoreRepository = firestoreRepo,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<ParentLoginRoute> {
            ParentLoginScreen(
                firestoreRepository = firestoreRepo,
                prefsRepository = prefsRepo,
                onLoginSuccess = { studentId ->
                    navController.navigate(ParentDashboardRoute(studentId)) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<ParentDashboardRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ParentDashboardRoute>()
            ParentDashboardScreen(
                studentId = route.studentId,
                firestoreRepository = firestoreRepo,
                onLogout = {
                    navController.navigate(WelcomeRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
