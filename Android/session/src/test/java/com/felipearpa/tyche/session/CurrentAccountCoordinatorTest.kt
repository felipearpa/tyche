package com.felipearpa.tyche.session

import com.felipearpa.tyche.session.authentication.domain.AccountLink
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import com.felipearpa.tyche.session.authentication.domain.ExternalAccountId
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentAccountCoordinatorTest {

    private val storedBundle = AccountBundle(
        accountId = "account-1",
        externalAccountId = "external-1",
        email = "gambler@tyche.com",
    ).withUsername("ElGoleador")

    private val canonicalBundle = AccountBundle(
        accountId = "account-1",
        externalAccountId = "external-1",
        email = "gambler@tyche.com",
    ).withUsername("ElGoleadorRenamed")

    @Test
    fun `given a persisted snapshot when hydrated then it is published without touching the repository`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = 0L,
                ),
            )

            val hydrated = fixture.coordinator.hydrate()

            hydrated shouldBe storedBundle
            fixture.coordinator.state.value shouldBe storedBundle
            fixture.repository.getCurrentAccountCallCount shouldBe 0
        }

    @Test
    fun `given nothing persisted when hydrated then null is published`() = runTest {
        val fixture = fixture(persisted = null)

        val hydrated = fixture.coordinator.hydrate()

        hydrated.shouldBeNull()
        fixture.coordinator.state.value.shouldBeNull()
    }

    @Test
    fun `given a stale snapshot when a cold-start refresh runs then the canonical account is persisted and published with a fresh validation`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = null,
                ),
            )
            fixture.repository.currentAccountResults.add(Result.success(canonicalBundle))
            fixture.coordinator.hydrate()
            fixture.now = 1_234L

            fixture.coordinator.refresh(CurrentAccountRefreshTrigger.COLD_START)

            fixture.coordinator.state.value shouldBe canonicalBundle
            fixture.storage.snapshot shouldBe CurrentAccountSnapshot(
                account = canonicalBundle,
                validatedAtEpochMillis = 1_234L,
            )
        }

    @Test
    fun `given a fresh snapshot when foreground and profile triggers occur then no request starts`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = 0L,
                ),
            )
            fixture.coordinator.hydrate()
            fixture.now = 1_000L // Well inside the five-minute freshness window.

            fixture.coordinator.refresh(CurrentAccountRefreshTrigger.FOREGROUND)
            fixture.coordinator.refresh(CurrentAccountRefreshTrigger.PROFILE_OPENED)

            fixture.repository.getCurrentAccountCallCount shouldBe 0
            fixture.coordinator.state.value shouldBe storedBundle
        }

    @Test
    fun `given concurrent refresh triggers then they coalesce into one request and both complete`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = null,
                ),
            )
            fixture.repository.gate = CompletableDeferred()
            fixture.repository.currentAccountResults.add(Result.success(canonicalBundle))
            fixture.coordinator.hydrate()

            val first = launch {
                fixture.coordinator.refresh(CurrentAccountRefreshTrigger.COLD_START)
            }
            val second = launch {
                fixture.coordinator.refresh(CurrentAccountRefreshTrigger.FOREGROUND)
            }
            runCurrent() // Both triggers are now awaiting the gated request.

            fixture.repository.gate!!.complete(Unit)
            first.join()
            second.join()

            fixture.repository.getCurrentAccountCallCount shouldBe 1
            fixture.coordinator.state.value shouldBe canonicalBundle
        }

    @Test
    fun `given a refresh failure then the snapshot is retained and a later trigger retries and succeeds`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = null,
                ),
            )
            fixture.repository.currentAccountResults.add(
                Result.failure(RuntimeException("offline")),
            )
            fixture.repository.currentAccountResults.add(Result.success(canonicalBundle))
            fixture.coordinator.hydrate()

            fixture.coordinator.refresh(CurrentAccountRefreshTrigger.FOREGROUND)

            fixture.coordinator.state.value shouldBe storedBundle
            fixture.storage.snapshot!!.account shouldBe storedBundle

            fixture.coordinator.refresh(CurrentAccountRefreshTrigger.FOREGROUND)

            fixture.repository.getCurrentAccountCallCount shouldBe 2
            fixture.coordinator.state.value shouldBe canonicalBundle
        }

    @Test
    fun `given a refresh began before a username save succeeds then the late refresh result cannot overwrite the saved username`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = null,
                ),
            )
            fixture.repository.gate = CompletableDeferred()
            fixture.repository.currentAccountResults.add(Result.success(canonicalBundle))
            fixture.coordinator.hydrate()

            val refreshJob = launch {
                fixture.coordinator.refresh(CurrentAccountRefreshTrigger.FOREGROUND)
            }
            runCurrent() // The refresh has captured its epoch and awaits the gate.

            val saved = fixture.coordinator.updateUsername(username = "ElNuevoGoleador")

            fixture.repository.gate!!.complete(Unit)
            refreshJob.join()

            saved shouldBeSuccess "ElNuevoGoleador"
            fixture.coordinator.state.value!!.username shouldBe "ElNuevoGoleador"
            fixture.storage.snapshot!!.account.username shouldBe "ElNuevoGoleador"
        }

    @Test
    fun `given a stored account when the username save succeeds then it is persisted and published once`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = 500L,
                ),
            )
            fixture.coordinator.hydrate()

            val result = fixture.coordinator.updateUsername(username = "ElNuevoGoleador")

            result shouldBeSuccess "ElNuevoGoleador"
            fixture.repository.updateUsernameCalls shouldBe
                listOf("account-1" to "ElNuevoGoleador")
            fixture.coordinator.state.value!!.username shouldBe "ElNuevoGoleador"
            fixture.storage.snapshot shouldBe CurrentAccountSnapshot(
                account = storedBundle.withUsername("ElNuevoGoleador"),
                validatedAtEpochMillis = 500L,
            )
            fixture.storage.operations shouldBe listOf("store")
        }

    @Test
    fun `given the username save fails then the state and storage are untouched`() = runTest {
        val fixture = fixture(
            persisted = CurrentAccountSnapshot(
                account = storedBundle,
                validatedAtEpochMillis = 500L,
            ),
        )
        val patchException = RuntimeException("patch failed")
        fixture.repository.updateUsernameResult = Result.failure(patchException)
        fixture.coordinator.hydrate()

        val result = fixture.coordinator.updateUsername(username = "ElNuevoGoleador")

        result.shouldBeFailure { exception -> exception shouldBe patchException }
        fixture.coordinator.state.value shouldBe storedBundle
        fixture.storage.snapshot!!.account shouldBe storedBundle
        fixture.storage.operations shouldBe emptyList()
    }

    @Test
    fun `given no stored account when the username save runs then it fails without a request`() =
        runTest {
            val fixture = fixture(persisted = null)
            fixture.coordinator.hydrate()

            val result = fixture.coordinator.updateUsername(username = "ElNuevoGoleador")

            result.shouldBeFailure { exception ->
                exception.shouldBeInstanceOf<IllegalStateException>()
            }
            fixture.repository.updateUsernameCalls shouldBe emptyList<Pair<String, String>>()
        }

    @Test
    fun `given a logout during a gated refresh then the late result cannot restore the account`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = null,
                ),
            )
            fixture.repository.gate = CompletableDeferred()
            fixture.repository.currentAccountResults.add(Result.success(canonicalBundle))
            fixture.coordinator.hydrate()

            val refreshJob = launch {
                fixture.coordinator.refresh(CurrentAccountRefreshTrigger.FOREGROUND)
            }
            runCurrent() // The refresh has captured its epoch and awaits the gate.

            fixture.coordinator.clear()

            fixture.repository.gate!!.complete(Unit)
            refreshJob.join()

            fixture.coordinator.state.value.shouldBeNull()
            fixture.storage.snapshot.shouldBeNull()
            fixture.storage.operations.last() shouldBe "delete"
        }

    @Test
    fun `given a published account when cleared then null is published and the snapshot is deleted`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = 0L,
                ),
            )
            fixture.coordinator.hydrate()

            fixture.coordinator.clear()

            fixture.coordinator.state.value.shouldBeNull()
            fixture.storage.snapshot.shouldBeNull()
            fixture.storage.operations shouldBe listOf("delete")
        }

    @Test
    fun `given failing storage then install and clear propagate the failure but refresh and username updates do not`() =
        runTest {
            val fixture = fixture(
                persisted = CurrentAccountSnapshot(
                    account = storedBundle,
                    validatedAtEpochMillis = null,
                ),
            )
            fixture.coordinator.hydrate()
            fixture.storage.failure = RuntimeException("disk full")

            // Refresh persists quietly: the server stays authoritative, so the published
            // account still advances even when the snapshot write fails.
            fixture.repository.currentAccountResults.add(Result.success(canonicalBundle))
            fixture.coordinator.refresh(CurrentAccountRefreshTrigger.FOREGROUND)
            fixture.coordinator.state.value shouldBe canonicalBundle

            // Username updates persist quietly for the same reason.
            val renamed = fixture.coordinator.updateUsername(username = "ElNuevoGoleador")
            renamed shouldBeSuccess "ElNuevoGoleador"

            // Install and clear fail loud: silently missing the write would sign in for one
            // process lifetime only, or resurrect the account on the next launch.
            shouldThrow<RuntimeException> { fixture.coordinator.install(account = storedBundle) }
            shouldThrow<RuntimeException> { fixture.coordinator.clear() }
        }

    @Test
    fun `given an install then it publishes with a fresh validation and suppresses the cold-start refresh`() =
        runTest {
            val fixture = fixture(persisted = null)
            fixture.now = 9_000L

            fixture.coordinator.install(account = storedBundle)

            fixture.coordinator.state.value shouldBe storedBundle
            fixture.storage.snapshot shouldBe CurrentAccountSnapshot(
                account = storedBundle,
                validatedAtEpochMillis = 9_000L,
            )

            fixture.coordinator.refresh(CurrentAccountRefreshTrigger.COLD_START)

            fixture.repository.getCurrentAccountCallCount shouldBe 0
        }

    private fun TestScope.fixture(persisted: CurrentAccountSnapshot?): Fixture {
        val storage = FakeAccountStorage(snapshot = persisted)
        val repository = FakeAuthenticationRepository()
        val fixture = Fixture(storage = storage, repository = repository)
        fixture.coordinator = CurrentAccountCoordinator(
            accountStorage = storage,
            authenticationRepository = repository,
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            nowEpochMillis = { fixture.now },
        )
        return fixture
    }

    private class Fixture(
        val storage: FakeAccountStorage,
        val repository: FakeAuthenticationRepository,
    ) {
        lateinit var coordinator: CurrentAccountCoordinator
        var now: Long = 0L
    }
}

private class FakeAccountStorage(var snapshot: CurrentAccountSnapshot?) : AccountStorage {
    val operations = mutableListOf<String>()
    var failure: Throwable? = null

    override suspend fun store(snapshot: CurrentAccountSnapshot) {
        failure?.let { throw it }
        this.snapshot = snapshot
        operations += "store"
    }

    override suspend fun delete() {
        failure?.let { throw it }
        snapshot = null
        operations += "delete"
    }

    override suspend fun retrieve(): CurrentAccountSnapshot? = snapshot
}

private class FakeAuthenticationRepository : AuthenticationRepository {
    var gate: CompletableDeferred<Unit>? = null
    val currentAccountResults = ArrayDeque<Result<AccountBundle>>()
    var getCurrentAccountCallCount = 0

    var updateUsernameResult: Result<Unit> = Result.success(Unit)
    val updateUsernameCalls = mutableListOf<Pair<String, String>>()

    override suspend fun getCurrentAccount(): Result<AccountBundle> {
        getCurrentAccountCallCount += 1
        gate?.await()
        return currentAccountResults.removeFirst()
    }

    override suspend fun updateUsername(accountId: String, username: String): Result<Unit> {
        updateUsernameCalls += accountId to username
        return updateUsernameResult
    }

    override suspend fun sendSignInLinkToEmail(email: String): Result<Unit> =
        error("not used in this test")

    override suspend fun signInWithEmailLink(
        email: String,
        emailLink: String,
    ): Result<ExternalAccountId> = error("not used in this test")

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<ExternalAccountId> = error("not used in this test")

    override suspend fun signInWithGoogle(idToken: String): Result<GoogleSignInResult> =
        error("not used in this test")

    override suspend fun logout(): Result<Unit> = error("not used in this test")

    override suspend fun linkAccount(accountLink: AccountLink): Result<AccountBundle> =
        error("not used in this test")
}
