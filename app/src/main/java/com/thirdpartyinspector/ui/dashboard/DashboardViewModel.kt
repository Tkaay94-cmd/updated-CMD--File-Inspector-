package com.thirdpartyinspector.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirdpartyinspector.data.local.entities.FindingEntity
import com.thirdpartyinspector.data.repository.ScanRepository
import com.thirdpartyinspector.domain.usecase.RunScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val runScanUseCase: RunScanUseCase,
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val scanRunState = MutableStateFlow(ScanRunState())

    private val scans = scanRepository.getScans()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val latestFindings = scans
        .map { scanList -> scanList.maxByOrNull { it.startTime }?.scanId }
        .distinctUntilChangedBy { scanId -> scanId }
        .flatMapLatest { scanId ->
            if (scanId == null) {
                flowOf(emptyList())
            } else {
                scanRepository.getFindings(scanId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList<FindingEntity>()
        )

    val uiState: StateFlow<DashboardUiState> = combine(
        scans,
        latestFindings,
        scanRunState
    ) { scanList, findings, runState ->
        DashboardStateMapper.toUiState(
            scans = scanList,
            findings = findings,
            scanStatus = runState.status,
            statusMessage = runState.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardUiState()
    )

    fun runScan() {
        if (scanRunState.value.status == ScanStatus.RUNNING) return

        viewModelScope.launch {
            scanRunState.value = ScanRunState(
                status = ScanStatus.RUNNING,
                message = "Inspecting device metadata…"
            )
            runCatching { runScanUseCase.runScan() }
                .onSuccess {
                    scanRunState.value = ScanRunState(
                        status = ScanStatus.IDLE,
                        message = "Scan complete. Results are saved on this device."
                    )
                }
                .onFailure {
                    scanRunState.value = ScanRunState(
                        status = ScanStatus.FAILED,
                        message = "Scan could not finish. Please try again."
                    )
                }
        }
    }

    private data class ScanRunState(
        val status: ScanStatus = ScanStatus.IDLE,
        val message: String? = null
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
