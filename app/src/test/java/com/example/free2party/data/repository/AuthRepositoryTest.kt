package com.example.free2party.data.repository

import com.example.free2party.data.model.UserSocials
import com.example.free2party.exception.EmailAlreadyInUseException
import com.example.free2party.exception.WeakPasswordException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {
    private lateinit var repository: AuthRepositoryImpl
    private val auth: FirebaseAuth = mockk()
    private val userRepository: UserRepository = mockk()
    private val firebaseUser: FirebaseUser = mockk()
    private val authResult: AuthResult = mockk()

    @Before
    fun setup() {
        repository = AuthRepositoryImpl(auth, userRepository)
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `register success`() = runTest {
        val email = "test@test.com"
        val password = "password123"
        val phoneNumber = "123456789"
        val birthday = "1990-01-01"
        val socials = UserSocials(facebookUsername = "fb_user")
        
        every { auth.createUserWithEmailAndPassword(email, password) } returns Tasks.forResult(authResult)
        every { authResult.user } returns firebaseUser
        every { firebaseUser.uid } returns "user123"
        coEvery { userRepository.createUserProfile(any()) } returns Result.success(Unit)

        val result = repository.register(
            firstName = "First",
            lastName = "Last",
            email = email,
            password = password,
            phoneNumber = phoneNumber,
            birthday = birthday,
            socials = socials
        )
        
        assertTrue(result.isSuccess)
        assertEquals(firebaseUser, result.getOrNull())
        verify { auth.createUserWithEmailAndPassword(email, password) }
        coVerify { 
            userRepository.createUserProfile(match { 
                it.uid == "user123" && 
                it.firstName == "First" && 
                it.lastName == "Last" && 
                it.email == email &&
                it.phoneNumber == phoneNumber &&
                it.birthday == birthday &&
                it.socials == socials
            }) 
        }
    }

    @Test
    fun `register fails with weak password`() = runTest {
        val email = "test@test.com"
        val password = "123"
        val exception = mockk<FirebaseAuthWeakPasswordException>()
        
        every { auth.createUserWithEmailAndPassword(email, password) } returns Tasks.forException(exception)

        val result = repository.register("First", "Last", email, password)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WeakPasswordException)
    }

    @Test
    fun `register fails with email collision`() = runTest {
        val email = "existing@test.com"
        val password = "password123"
        val exception = mockk<FirebaseAuthUserCollisionException>()
        
        every { auth.createUserWithEmailAndPassword(email, password) } returns Tasks.forException(exception)

        val result = repository.register("First", "Last", email, password)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is EmailAlreadyInUseException)
    }

    @Test
    fun `login success`() = runTest {
        val email = "test@test.com"
        val password = "password123"
        
        every { auth.signInWithEmailAndPassword(email, password) } returns Tasks.forResult(authResult)
        every { authResult.user } returns firebaseUser

        val result = repository.login(email, password)
        
        assertTrue(result.isSuccess)
        assertEquals(firebaseUser, result.getOrNull())
    }

    @Test
    fun `login fails with invalid credentials`() = runTest {
        val email = "test@test.com"
        val password = "wrong"
        
        every { auth.signInWithEmailAndPassword(email, password) } returns Tasks.forException(Exception("Invalid credentials"))

        val result = repository.login(email, password)
        
        assertTrue(result.isFailure)
    }

    @Test
    fun `sendPasswordResetEmail success`() = runTest {
        val email = "test@test.com"
        every { auth.sendPasswordResetEmail(email) } returns Tasks.forResult(null)

        val result = repository.sendPasswordResetEmail(email)
        
        assertTrue(result.isSuccess)
        verify { auth.sendPasswordResetEmail(email) }
    }

    @Test
    fun `logout calls auth signOut`() {
        every { auth.signOut() } returns Unit
        repository.logout()
        verify { auth.signOut() }
    }
}
